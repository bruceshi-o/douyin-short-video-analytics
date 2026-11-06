<script setup>
import {ref,computed,onMounted} from 'vue'
import {api} from '../main'
import {useChart} from '../chart'
const data=ref({videoCount:0,datasetCount:0,trainingCount:0,predictionCount:0,categories:{},dailyCount:{},dailyViews:{},levels:{},ranking:[],recentTrainings:[]})
const trend=ref(),categories=ref(),levels=ref(),error=ref('')
const trendOption=computed(()=>({tooltip:{trigger:'axis'},legend:{data:['新增视频','播放量']},xAxis:{type:'category',data:Object.keys(data.value.dailyCount)},yAxis:[{type:'value'},{type:'value'}],series:[{name:'新增视频',type:'bar',data:Object.values(data.value.dailyCount)},{name:'播放量',type:'line',yAxisIndex:1,smooth:true,data:Object.values(data.value.dailyViews)}]}))
const categoryOption=computed(()=>({tooltip:{trigger:'item'},series:[{type:'pie',radius:['38%','68%'],data:Object.entries(data.value.categories).map(([name,value])=>({name,value}))}]}))
const levelOption=computed(()=>({tooltip:{trigger:'item'},xAxis:{type:'category',data:Object.keys(data.value.levels)},yAxis:{type:'value'},series:[{type:'bar',data:Object.values(data.value.levels),itemStyle:{color:'#4582e7'}}]}))
useChart(trend,trendOption);useChart(categories,categoryOption);useChart(levels,levelOption)
onMounted(async()=>{try{data.value=(await api.get('/home')).data}catch(e){error.value=e.response?.data?.error||e.message}})
</script>
<template><p v-if="error" class="message error">{{error}}</p><div class="grid"><div v-for="item in [['短视频总条数',data.videoCount],['数据集数量',data.datasetCount],['训练任务数',data.trainingCount],['预测任务数',data.predictionCount]]" :key="item[0]" class="card"><span class="muted">{{item[0]}}</span><b>{{item[1]}}</b></div></div><div class="panels"><section class="panel"><h2>新增视频与播放趋势</h2><div ref="trend" class="chart"></div></section><section class="panel"><h2>视频分类分布</h2><div ref="categories" class="chart"></div></section><section class="panel"><h2>预测热度分布</h2><div ref="levels" class="chart"></div></section><section class="panel"><h2>最近训练任务</h2><table><tr><th>名称</th><th>算法</th><th>状态</th></tr><tr v-for="t in data.recentTrainings" :key="t.id"><td>{{t.name}}</td><td>{{t.algorithm}}</td><td>{{t.status}}</td></tr></table></section><section class="panel wide"><h2>短视频热门度排行</h2><table><tr><th>标题</th><th>作者</th><th>播放</th><th>点赞</th><th>预测分</th></tr><tr v-for="v in data.ranking" :key="v.id"><td>{{v.title}}</td><td>{{v.author}}</td><td>{{v.views}}</td><td>{{v.likes}}</td><td>{{v.predictedScore??'—'}}</td></tr></table></section></div></template>
