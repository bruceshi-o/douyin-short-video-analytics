<script setup>
import {ref,onMounted} from 'vue'
import {api} from '../main'
const items=ref([]),file=ref(),name=ref(''),description=ref(''),query=ref(''),message=ref('')
async function load(){items.value=(await api.get('/datasets')).data}
async function upload(){try{let form=new FormData();form.append('file',file.value);form.append('name',name.value);form.append('description',description.value);await api.post('/datasets',form);message.value='上传成功';await load()}catch(e){message.value=e.response?.data?.error||e.message}}
async function edit(d){const value=prompt('数据集描述',d.description||'');if(value!==null){await api.put('/datasets/'+d.id,{description:value});await load()}}
async function remove(d){if(confirm('删除数据集 '+d.name+'？')){try{await api.delete('/datasets/'+d.id);await load()}catch(e){message.value=e.response?.data?.error||e.message}}}
onMounted(load)
</script>
<template><section class="panel"><h2>上传 CSV 数据集</h2><div class="form"><input v-model="name" placeholder="数据集名称"><input v-model="description" placeholder="描述"><input type="file" accept=".csv" @change="file=$event.target.files[0]"><button :disabled="!file" @click="upload">上传数据集</button></div><p v-if="message" class="message">{{message}}</p></section><section class="panel" style="margin-top:16px"><div class="row"><h2>数据集列表</h2><input v-model="query" placeholder="搜索名称"></div><table><tr><th>名称</th><th>描述</th><th>样本数</th><th>大小</th><th>状态</th><th>操作</th></tr><tr v-for="d in items.filter(x=>x.name.includes(query))" :key="d.id"><td>{{d.name}}</td><td>{{d.description}}</td><td>{{d.samples}}</td><td>{{(d.sizeBytes/1024).toFixed(1)}} KB</td><td>{{d.status}}</td><td><button class="secondary" @click="edit(d)">编辑</button> <button class="danger" @click="remove(d)">删除</button></td></tr></table></section></template>
