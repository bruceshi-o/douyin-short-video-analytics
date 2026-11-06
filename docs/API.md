# 接口说明

所有接口使用 JSON，上传接口使用 multipart/form-data。前端开发服务器将 /api 代理到 8080。

| 功能 | 方法与路径 | 输入 |
|---|---|---|
| 首页统计 | GET /api/home | 无 |
| 数据集列表 | GET /api/datasets | 无 |
| 上传数据集 | POST /api/datasets | file、name、description |
| 修改数据集 | PUT /api/datasets/{id} | name、description |
| 删除数据集 | DELETE /api/datasets/{id} | 无；已被训练任务引用时拒绝 |
| 训练记录 | GET /api/models | 无 |
| 发起训练 | POST /api/train | datasetId、name、algorithm、target、features、params |
| 训练详情 | GET /api/train/{id} | 无 |
| 单条预测 | POST /api/predict | modelId、input |
| 视频列表 | GET /api/videos | 可选 category、level |
| 视频新增/修改/删除 | POST/PUT/DELETE /api/videos | 视频 JSON |
| 视频一键预测 | POST /api/videos/{id}/predict | modelId |

训练请求示例：`{"datasetId":1,"name":"随机森林播放预测","algorithm":"forest","target":"views","features":["duration","likes","comments","shares","favorites","hashtags","hot_bgm","category","publish_period"],"params":{"testRatio":0.2,"seed":42,"trees":100,"maxDepth":8}}`。

训练是异步的。POST 返回“待训练”记录，页面每 3 秒刷新任务列表。成功后 reportJson 含真实值、预测值和特征重要性；失败时 error 有错误信息。
