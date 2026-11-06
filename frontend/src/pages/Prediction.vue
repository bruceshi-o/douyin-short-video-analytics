<script setup>
import {ref,onMounted,computed} from 'vue'
import {api} from '../main'
const models=ref([]),modelId=ref(''),result=ref(null),error=ref('')
const input=ref({duration:30,likes:1000,comments:60,shares:20,favorites:40,hashtags:2,hot_bgm:0,category:'生活',publish_period:'晚'})
const chosen=computed(()=>models.value.find(m=>m.id===modelId.value))
const fields=computed(()=>{try{return JSON.parse(chosen.value?.features||'[]')}catch{return []}})
const names={duration:'视频时长（秒）',likes:'预估点赞',comments:'评论数',shares:'转发数',favorites:'收藏数',hashtags:'话题标签数',hot_bgm:'热门 BGM',category:'分类',publish_period:'发布时段'}
async function predict(){try{error.value='';result.value=(await api.post('/predict',{modelId:modelId.value,input:input.value})).data}catch(e){error.value=e.response?.data?.error||e.message}}
onMounted(async()=>models.value=(await api.get('/models')).data.filter(m=>m.status==='训练成功'))
</script>
<template><section class="panel"><h2>单条短视频热度预测</h2><p class="muted">输入字段由训练模型自动决定，预处理规则与训练保持一致。</p><div class="form"><label>已训练模型<select v-model="modelId"><option value="">请选择模型</option><option v-for="m in models" :key="m.id" :value="m.id">{{m.name}} · {{m.target}}</option></select></label></div><div class="form"><label v-for="f in fields" :key="f">{{names[f]}}<select v-if="f==='category'" v-model="input[f]"><option v-for="x in ['生活','美食','美妆','游戏','知识','舞蹈','剧情']">{{x}}</option></select><select v-else-if="f==='publish_period'" v-model="input[f]"><option v-for="x in ['早','中','晚','凌晨']">{{x}}</option></select><select v-else-if="f==='hot_bgm'" v-model.number="input[f]"><option :value="0">否</option><option :value="1">是</option></select><input v-else type="number" v-model.number="input[f]" min="0"></label></div><button :disabled="!modelId" @click="predict">开始预测</button><p v-if="error" class="message error">{{error}}</p></section><section v-if="result" class="panel" style="margin-top:16px"><h2>预测结果</h2><div class="grid"><div class="card">预测目标值<b>{{result.value}}</b></div><div class="card">预估播放量<b>{{result.estimatedViews}}</b></div><div class="card">综合热度分<b>{{result.score}}</b></div><div class="card">热度等级<b>{{result.level}}</b></div></div><p class="muted">热度分采用演示规则：播放量 / 10000，上限 100；高热度 ≥70，中热度 ≥30。</p></section></template>
