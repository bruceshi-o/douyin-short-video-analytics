<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { routes } from './main'
const collapsed=ref(false),route=useRoute()
</script>
<template>
<div class="shell"><aside :class="{collapsed}"><div class="brand">▶ <span v-if="!collapsed">短视频分析平台</span></div><button class="fold" @click="collapsed=!collapsed">{{collapsed?'展开菜单':'收起菜单'}}</button><nav><router-link v-for="item in routes" :key="item.path" :to="item.path" :class="{active:route.path===item.path}"><span class="menu-icon">◈</span><span v-if="!collapsed">{{item.name}}</span></router-link></nav></aside><main><header><div><small>传媒数据管理与分析</small><h1>{{route.name}}</h1></div><div class="badge">Spring Boot · Vue · ML</div></header><router-view/></main></div>
</template>
