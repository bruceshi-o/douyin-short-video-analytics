import * as echarts from 'echarts'
import { onMounted,onUnmounted,watch } from 'vue'
export function useChart(element,option){
 let chart
 function draw(){if(element.value){chart??=echarts.init(element.value);chart.setOption(option.value,true)}}
 onMounted(draw);watch(option,draw,{deep:true,flush:'post'});onUnmounted(()=>chart?.dispose())
}
