import json,sys
from pathlib import Path
import joblib,numpy as np,pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error,mean_squared_error,r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder,StandardScaler
NUMERIC=['duration','likes','comments','shares','favorites','hashtags','hot_bgm']
CATEGORICAL=['category','publish_period']
ALLOWED=NUMERIC+CATEGORICAL
def estimator(name,p):
 seed=int(p.get('seed',42))
 if name=='linear': return LinearRegression()
 if name=='forest': return RandomForestRegressor(n_estimators=int(p.get('trees',100)),max_depth=int(p['maxDepth']) if p.get('maxDepth') else None,random_state=seed,n_jobs=-1)
 if name=='xgboost':
  from xgboost import XGBRegressor
  return XGBRegressor(n_estimators=int(p.get('iterations',100)),learning_rate=float(p.get('learningRate',.1)),random_state=seed)
 if name=='lightgbm':
  from lightgbm import LGBMRegressor
  return LGBMRegressor(n_estimators=int(p.get('iterations',100)),learning_rate=float(p.get('learningRate',.1)),random_state=seed,verbosity=-1)
 raise ValueError('Unsupported algorithm')
def train(c):
 f=c['features']
 if not isinstance(f,list) or not f or len(set(f))!=len(f) or any(x not in ALLOWED for x in f): raise ValueError('Invalid feature selection')
 df=pd.read_csv(c['csv'],encoding='utf-8-sig'); target=c['target']
 if target=='hot_score' and target not in df: df[target]=np.minimum(100,pd.to_numeric(df['views'],errors='coerce')/10000)
 if target not in ('views','hot_score') or target not in df: raise ValueError('Invalid target')
 df[target]=pd.to_numeric(df[target],errors='coerce'); df=df.dropna(subset=[target])
 if len(df)<10: raise ValueError('At least 10 valid rows required')
 for col in [x for x in f if x in NUMERIC]: df[col]=pd.to_numeric(df[col],errors='coerce')
 nums=[x for x in f if x in NUMERIC]; cats=[x for x in f if x in CATEGORICAL]; transforms=[]
 if nums: transforms.append(('num',Pipeline([('impute',SimpleImputer(strategy='median')),('scale',StandardScaler())]),nums))
 if cats: transforms.append(('cat',Pipeline([('impute',SimpleImputer(strategy='most_frequent')),('encode',OneHotEncoder(handle_unknown='ignore',sparse_output=False))]),cats))
 pipeline=Pipeline([('preprocess',ColumnTransformer(transforms)),('regressor',estimator(c['algorithm'],c.get('params',{})))])
 p=c.get('params',{}); ratio=float(p.get('testRatio',.2))
 if not .1<=ratio<=.4: raise ValueError('Test ratio must be 0.1 to 0.4')
 xt,xv,yt,yv=train_test_split(df[f],df[target],test_size=ratio,random_state=int(p.get('seed',42)))
 pipeline.fit(xt,yt); predicted=pipeline.predict(xv)
 names=pipeline.named_steps['preprocess'].get_feature_names_out().tolist(); reg=pipeline.named_steps['regressor']; importance=getattr(reg,'feature_importances_',None)
 if importance is None and hasattr(reg,'coef_'): importance=np.abs(reg.coef_)
 imp=sorted(({'feature':n,'importance':float(v)} for n,v in zip(names,importance)),key=lambda x:x['importance'],reverse=True) if importance is not None else []
 Path(c['model']).parent.mkdir(parents=True,exist_ok=True); joblib.dump({'pipeline':pipeline,'features':f,'target':target},c['model'])
 return {'mae':float(mean_absolute_error(yv,predicted)),'mse':float(mean_squared_error(yv,predicted)),'r2':float(r2_score(yv,predicted)),'actual':[float(v) for v in yv[:100]],'predicted':[float(v) for v in predicted[:100]],'importance':imp[:15],'trainCount':len(xt),'testCount':len(xv)}
def predict(c):
 artifact=joblib.load(c['model']); f=artifact['features']; data=c['input']
 if set(f)-set(data): raise ValueError('Prediction input differs from training features')
 row=pd.DataFrame([{x:data[x] for x in f}])
 for x in [x for x in f if x in NUMERIC]: row[x]=pd.to_numeric(row[x],errors='raise')
 value=max(0.,float(artifact['pipeline'].predict(row)[0])); views=value if artifact['target']=='views' else value*10000; score=min(100.,value/10000 if artifact['target']=='views' else value)
 return {'value':round(value,2),'target':artifact['target'],'estimatedViews':round(views,2),'score':round(score,2),'level':'高热度' if score>=70 else '中热度' if score>=30 else '低热度'}
if __name__=='__main__':
 try:
  with open(sys.argv[2],encoding='utf-8') as stream: config=json.load(stream)
  print(json.dumps(train(config) if sys.argv[1]=='train' else predict(config),ensure_ascii=False))
 except Exception as exc: print(str(exc),file=sys.stderr);sys.exit(1)
