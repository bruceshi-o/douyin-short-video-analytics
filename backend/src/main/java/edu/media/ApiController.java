package edu.media;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/api") @CrossOrigin(origins={"http://localhost:5173","http://127.0.0.1:5173"}) public class ApiController {
 final ApiService service;final DatasetRepo datasets;final TrainingRepo trainings;final VideoRepo videos;final ObjectMapper json;
 ApiController(ApiService s,DatasetRepo d,TrainingRepo t,VideoRepo v,ObjectMapper j){service=s;datasets=d;trainings=t;videos=v;json=j;}
 @GetMapping("/home") public Object home(){return service.home();}
 @GetMapping("/datasets") public Object datasets(){return datasets.findAll();}
 @PostMapping(value="/datasets",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object upload(@RequestParam MultipartFile file,@RequestParam(required=false) String name,@RequestParam(required=false) String description)throws Exception{return service.upload(file,name,description);}
 @PutMapping("/datasets/{id}") public Object updateDataset(@PathVariable Long id,@RequestBody Map<String,String> body){var d=datasets.findById(id).orElseThrow();d.description=body.getOrDefault("description",d.description);d.name=body.getOrDefault("name",d.name);return datasets.save(d);}
 @DeleteMapping("/datasets/{id}") public void deleteDataset(@PathVariable Long id)throws Exception{if(trainings.findAll().stream().anyMatch(t->t.datasetId.equals(id)))throw new IllegalArgumentException("数据集已关联训练任务，不能删除");var d=datasets.findById(id).orElseThrow();java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(d.path));datasets.delete(d);}
 @GetMapping("/models") public Object models(){return trainings.findAll();}
 @GetMapping("/train/{id}") public Object training(@PathVariable Long id){return trainings.findById(id).orElseThrow();}
 @PostMapping("/train") public Object train(@RequestBody Map<String,Object> body)throws Exception{
  Long datasetId=Long.valueOf(body.get("datasetId").toString());datasets.findById(datasetId).orElseThrow();String algorithm=String.valueOf(body.get("algorithm"));if(!Set.of("linear","forest","xgboost","lightgbm").contains(algorithm))throw new IllegalArgumentException("不支持的算法");String target=String.valueOf(body.get("target"));if(!Set.of("views","hot_score").contains(target))throw new IllegalArgumentException("不支持的目标");
  Entity.Training t=new Entity.Training();t.datasetId=datasetId;t.name=String.valueOf(body.getOrDefault("name",algorithm+"-"+System.currentTimeMillis()));t.algorithm=algorithm;t.target=target;t.features=json.writeValueAsString(body.get("features"));t.params=json.writeValueAsString(body.getOrDefault("params",Map.of()));t=trainings.save(t);service.train(t.id);return t;
 }
 @PostMapping("/predict") public Object predict(@RequestBody Map<String,Object> body)throws Exception{return service.predict(Long.valueOf(body.get("modelId").toString()),(Map<String,Object>)body.get("input"));}
 @GetMapping("/videos") public Object videos(@RequestParam(required=false) String category,@RequestParam(required=false) String level){return videos.findAll().stream().filter(v->category==null||category.isBlank()||category.equals(v.category)).filter(v->level==null||level.isBlank()||level.equals(v.heatLevel)).toList();}
 @PostMapping("/videos") public Object addVideo(@RequestBody Entity.Video v){v.id=null;return videos.save(v);}
 @PutMapping("/videos/{id}") public Object editVideo(@PathVariable Long id,@RequestBody Entity.Video v){videos.findById(id).orElseThrow();v.id=id;return videos.save(v);}
 @DeleteMapping("/videos/{id}") public void deleteVideo(@PathVariable Long id){videos.deleteById(id);}
 @PostMapping("/videos/{id}/predict") public Object predictVideo(@PathVariable Long id,@RequestBody Map<String,Object> body)throws Exception{var v=videos.findById(id).orElseThrow();Map<String,Object> input=new HashMap<>();input.put("duration",v.duration);input.put("likes",v.likes);input.put("comments",v.comments);input.put("shares",v.shares);input.put("favorites",v.favorites);input.put("hashtags",v.hashtags);input.put("hot_bgm",v.hotBgm?1:0);input.put("category",v.category);input.put("publish_period",v.publishPeriod);Map<?,?> result=service.predict(Long.valueOf(body.get("modelId").toString()),input);v.predictedScore=((Number)result.get("score")).doubleValue();v.predictedViews=((Number)result.get("estimatedViews")).doubleValue();v.heatLevel=(String)result.get("level");videos.save(v);return result;}
 @ExceptionHandler(Exception.class) public ResponseEntity<?> error(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"请求失败":e.getMessage()));}
}
