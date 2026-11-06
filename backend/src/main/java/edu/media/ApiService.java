package edu.media;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@Service public class ApiService {
 final DatasetRepo datasets; final TrainingRepo trainings; final VideoRepo videos; final PredictionRepo predictions; final ObjectMapper json; final HomeMapper homeMapper;
 @Value("${app.python}") String python; @Value("${app.ml-script}") String script;
 ApiService(DatasetRepo d,TrainingRepo t,VideoRepo v,PredictionRepo p,ObjectMapper j,HomeMapper h){datasets=d;trainings=t;videos=v;predictions=p;json=j;homeMapper=h;}
 Path root(){return Paths.get("data").toAbsolutePath().normalize();}
 @org.springframework.transaction.annotation.Transactional public Entity.Dataset upload(MultipartFile file,String name,String description)throws Exception{
  if(file.isEmpty()||file.getSize()>10_000_000||!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".csv"))throw new IllegalArgumentException("请上传不超过10MB的CSV文件");
  Path dir=Files.createDirectories(root().resolve("uploads")); Path path=dir.resolve(UUID.randomUUID()+".csv"); file.transferTo(path);
  try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8); CSVParser csv=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)){
   Set<String> required=Set.of("duration","likes","comments","shares","favorites","hashtags","hot_bgm","category","publish_period","views");
   if(!csv.getHeaderMap().keySet().containsAll(required))throw new IllegalArgumentException("CSV缺少必需字段: "+required);
   int count=0;List<CSVRecord> rows=new ArrayList<>(); for(CSVRecord row:csv){for(String key:required)if(row.get(key).isBlank())throw new IllegalArgumentException("第"+row.getRecordNumber()+"行缺少"+key); count++;rows.add(row);}
   if(count<10)throw new IllegalArgumentException("至少需要10条样本");
   Entity.Dataset d=new Entity.Dataset();d.name=name==null||name.isBlank()?file.getOriginalFilename():name;d.description=description;d.path=path.toString();d.sizeBytes=file.getSize();d.samples=count;d=datasets.save(d);
   if(csv.getHeaderMap().keySet().containsAll(Set.of("title","author","published_at"))){
    List<Entity.Video> imported=new ArrayList<>();
    for(CSVRecord row:rows){Entity.Video v=new Entity.Video();v.title=row.get("title");v.author=row.get("author");v.category=row.get("category");v.publishedAt=LocalDate.parse(row.get("published_at")).atStartOfDay();v.duration=Double.parseDouble(row.get("duration"));v.likes=Long.parseLong(row.get("likes"));v.comments=Long.parseLong(row.get("comments"));v.shares=Long.parseLong(row.get("shares"));v.favorites=Long.parseLong(row.get("favorites"));v.hashtags=Integer.parseInt(row.get("hashtags"));v.hotBgm="1".equals(row.get("hot_bgm"));v.publishPeriod=row.get("publish_period");v.views=Long.parseLong(row.get("views"));imported.add(v);}
    videos.saveAll(imported);
   }
   return d;
  }catch(Exception e){Files.deleteIfExists(path);throw e;}
 }
 public Map<String,Object> home(){
  List<Entity.Video> vs=videos.findAll();List<Entity.Training> ts=trainings.findAll();List<Entity.Prediction> ps=predictions.findAll();
  Map<String,Long> cats=new TreeMap<>(),daily=new TreeMap<>();Map<String,Long> plays=new TreeMap<>(),levels=new LinkedHashMap<>();levels.put("高热度",0L);levels.put("中热度",0L);levels.put("低热度",0L);
  for(var v:vs){cats.merge(v.category==null?"其他":v.category,1L,Long::sum);if(v.publishedAt!=null){String day=v.publishedAt.toLocalDate().toString();daily.merge(day,1L,Long::sum);plays.merge(day,v.views,Long::sum);}}
  for(var p:ps)levels.merge(p.level,1L,Long::sum);
  List<Entity.Video> ranking=new ArrayList<>(vs); ranking.sort(Comparator.comparingDouble((Entity.Video v)->v.predictedScore==null?v.views:v.predictedScore).reversed());
  ts.sort(Comparator.comparing((Entity.Training t)->t.createdAt).reversed());
  Map<String,Object> result=new LinkedHashMap<>(homeMapper.counts());result.put("categories",cats);result.put("dailyCount",daily);result.put("dailyViews",plays);result.put("levels",levels);result.put("ranking",ranking.subList(0,Math.min(10,ranking.size())));result.put("recentTrainings",ts.subList(0,Math.min(5,ts.size())));return result;
 }
 @Async public void train(Long id){
  Entity.Training t=trainings.findById(id).orElseThrow();Entity.Dataset d=datasets.findById(t.datasetId).orElseThrow(); t.status="训练中";d.status="训练中";trainings.save(t);datasets.save(d);
  try{
   Path modelDir=Files.createDirectories(root().resolve("models"));Path config=Files.createTempFile(root(),"train-",".json");
   Map<String,Object> args=new HashMap<>();args.put("csv",d.path);args.put("model",modelDir.resolve("model-"+id+".joblib").toString());args.put("algorithm",t.algorithm);args.put("target",t.target);args.put("features",json.readValue(t.features,List.class));args.put("params",json.readValue(t.params,Map.class));
   Files.writeString(config,json.writeValueAsString(args));Process p=new ProcessBuilder(python,script,"train",config.toString()).redirectErrorStream(true).start();String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);int code=p.waitFor();Files.deleteIfExists(config);
   if(code!=0)throw new IllegalStateException(output.substring(0,Math.min(output.length(),1500)));
   Map<?,?> report=json.readValue(output,Map.class);t.mae=((Number)report.get("mae")).doubleValue();t.mse=((Number)report.get("mse")).doubleValue();t.r2=((Number)report.get("r2")).doubleValue();t.reportJson=output;t.modelPath=(String)args.get("model");t.status="训练成功";d.status="已用于训练";
  }catch(Exception e){t.status="训练失败";t.error=e.getMessage();d.status="未使用";}
  t.finishedAt=LocalDateTime.now();trainings.save(t);datasets.save(d);
 }
 public Map<?,?> predict(Long modelId,Map<String,Object> input)throws Exception{
  Entity.Training t=trainings.findById(modelId).orElseThrow();if(!"训练成功".equals(t.status))throw new IllegalArgumentException("模型尚未训练成功");
  List<String> features=json.readValue(t.features,List.class);for(String feature:features)if(!input.containsKey(feature))throw new IllegalArgumentException("缺少训练特征: "+feature);
  Path config=Files.createTempFile(root(),"predict-",".json");Files.writeString(config,json.writeValueAsString(Map.of("model",t.modelPath,"input",input)));
  Process p=new ProcessBuilder(python,script,"predict",config.toString()).redirectErrorStream(true).start();String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);int code=p.waitFor();Files.deleteIfExists(config);if(code!=0)throw new IllegalStateException(output);
  Map<?,?> result=json.readValue(output,Map.class);Entity.Prediction record=new Entity.Prediction();record.modelId=modelId;record.value=((Number)result.get("value")).doubleValue();record.score=((Number)result.get("score")).doubleValue();record.level=(String)result.get("level");predictions.save(record);return result;
 }
}
