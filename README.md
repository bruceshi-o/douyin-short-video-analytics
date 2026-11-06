# 抖音短视频热度预测与分析系统

Spring Boot API + Vue 3 页面 + Python scikit-learn 训练器。项目提供 CSV 数据集管理、异步训练、模型评估、单条预测、短视频管理和首页数据看板。

## 启动

1. JDK 17、Maven 3.9、Node.js 20 和 Python 3.10+。
2. `python -m pip install -r ml/requirements.txt`
3. `cd backend && mvn spring-boot:run`（默认端口 8080）
4. `cd frontend && npm install && npm run dev`（默认端口 5173）
5. 打开 http://localhost:5173 。后端数据和上传文件保存在 `backend/data/`。

Windows 可运行 `start.ps1`，分别启动前后端。首次使用请在“数据集管理”上传 `sample/douyin_sample.csv`，然后在“模型训练”中选择数据集与算法训练。训练成功后到“模型预测”选择该模型。

## CSV 格式

必须包含 `duration,likes,comments,shares,favorites,hashtags,hot_bgm,category,publish_period,views`。可选 `title,author,published_at,hot_score`。`hot_score` 缺失时采用 `min(100, views / 10000)` 作为综合热度分数；这是演示定义，不代表平台官方口径。分类和发布时段以字符串录入；布尔列为 0/1。CSV 使用 UTF-8。上传限制 10MB，至少 10 行有效样本。训练和预测共用已保存的预处理管道。

## API

`GET /api/home` 首页统计；`GET/POST/PUT/DELETE /api/datasets` 数据集；`GET /api/models` 模型；`POST /api/train` 发起训练；`GET /api/train/{id}` 查看状态和评估；`POST /api/predict` 预测；`GET/POST/PUT/DELETE /api/videos` 视频。详情见 `docs/API.md`。

## 实现说明

训练算法支持线性回归、随机森林、XGBoost 和 LightGBM。后两者需要相应 Python 包，已列入 requirements。训练调用单独 Python 进程，避免阻塞 HTTP；训练状态和结果由 H2 持久化。生产部署应增加身份认证、任务队列和受控的文件存储。示例数据为合成数据，不应据此宣称真实平台预测效果。
