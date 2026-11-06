import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'
import App from './App.vue'
import './style.css'
export const api=axios.create({baseURL:'/api'})
export const routes=[
 {path:'/',name:'首页',component:()=>import('./pages/Home.vue')},
 {path:'/datasets',name:'数据集管理',component:()=>import('./pages/Datasets.vue')},
 {path:'/training',name:'模型训练评估',component:()=>import('./pages/Training.vue')},
 {path:'/prediction',name:'模型预测',component:()=>import('./pages/Prediction.vue')},
 {path:'/videos',name:'短视频管理',component:()=>import('./pages/Videos.vue')}
]
export const router=createRouter({history:createWebHistory(),routes})
createApp(App).use(router).mount('#app')
