<template>
  <div class="system-monitor">
    <div class="monitor-grid">
      <!-- CPU 使用率图表 -->
      <el-card class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>CPU 使用率</span>
            <span class="current-value">{{ cpuUsage }}%</span>
          </div>
        </template>
        <div class="chart-container">
          <v-chart :option="cpuOption" autoresize />
        </div>
      </el-card>
      
      <!-- 内存使用率图表 -->
      <el-card class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>内存使用率</span>
            <span class="current-value">{{ memoryUsage }}%</span>
          </div>
        </template>
        <div class="chart-container">
          <v-chart :option="memoryOption" autoresize />
        </div>
      </el-card>
      
      <!-- 磁盘使用率图表 -->
      <el-card class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>磁盘使用率</span>
            <span class="current-value">{{ diskUsage }}%</span>
          </div>
        </template>
        <div class="chart-container">
          <v-chart :option="diskOption" autoresize />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import {
  LineChart,
  BarChart
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DatasetComponent,
  TransformComponent
} from 'echarts/components'
import {
  CanvasRenderer
} from 'echarts/renderers'

// 注册必要的组件
use([
  LineChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DatasetComponent,
  TransformComponent,
  CanvasRenderer
])

// 定义监控数据接口
interface MonitorData {
  timestamp: number
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
}

// 组件属性
const props = defineProps<{
  data?: MonitorData[]
  updateInterval?: number
}>()

// 默认属性值
const defaultUpdateInterval = 10000 // 10秒

// 本地数据
const chartData = ref<MonitorData[]>(props.data || [])
const updateTimer = ref<number | null>(null)

// 当前值
const cpuUsage = computed(() => {
  const data = chartData.value || []
  if (data.length === 0) return 0
  const lastData = data[data.length - 1] || { cpuUsage: 0 }
  return lastData.cpuUsage.toFixed(1)
})

const memoryUsage = computed(() => {
  const data = chartData.value || []
  if (data.length === 0) return 0
  const lastData = data[data.length - 1] || { memoryUsage: 0 }
  return lastData.memoryUsage.toFixed(1)
})

const diskUsage = computed(() => {
  const data = chartData.value || []
  if (data.length === 0) return 0
  const lastData = data[data.length - 1] || { diskUsage: 0 }
  return lastData.diskUsage.toFixed(1)
})

// 格式化时间戳
const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString()
}

// CPU 图表配置
const cpuOption = computed(() => {
  const data = chartData.value || []
  const times = data.map(item => formatTime(item.timestamp))
  const usages = data.map(item => item.cpuUsage)
  
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>CPU 使用率: ${data.value}%`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [
      {
        name: 'CPU 使用率',
        type: 'line',
        smooth: true,
        data: usages,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: 'rgba(64, 158, 255, 0.3)'
            }, {
              offset: 1, color: 'rgba(64, 158, 255, 0.05)'
            }]
          }
        },
        lineStyle: {
          color: '#409EFF'
        },
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  }
})

// 内存图表配置
const memoryOption = computed(() => {
  const data = chartData.value || []
  const times = data.map(item => formatTime(item.timestamp))
  const usages = data.map(item => item.memoryUsage)
  
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>内存使用率: ${data.value}%`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [
      {
        name: '内存使用率',
        type: 'line',
        smooth: true,
        data: usages,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: 'rgba(103, 194, 58, 0.3)'
            }, {
              offset: 1, color: 'rgba(103, 194, 58, 0.05)'
            }]
          }
        },
        lineStyle: {
          color: '#67C23A'
        },
        itemStyle: {
          color: '#67C23A'
        }
      }
    ]
  }
})

// 磁盘图表配置
const diskOption = computed(() => {
  const data = chartData.value || []
  const times = data.map(item => formatTime(item.timestamp))
  const usages = data.map(item => item.diskUsage)
  
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>磁盘使用率: ${data.value}%`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [
      {
        name: '磁盘使用率',
        type: 'line',
        smooth: true,
        data: usages,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: 'rgba(230, 162, 60, 0.3)'
            }, {
              offset: 1, color: 'rgba(230, 162, 60, 0.05)'
            }]
          }
        },
        lineStyle: {
          color: '#E6A23C'
        },
        itemStyle: {
          color: '#E6A23C'
        }
      }
    ]
  }
})



// 监听属性变化
watch(() => props.data, (newData) => {
  if (newData) {
    chartData.value = newData
  }
}, { deep: true })

// 组件卸载
onUnmounted(() => {
  if (updateTimer.value) {
    clearInterval(updateTimer.value)
  }
})
</script>

<style scoped>
.system-monitor {
  width: 100%;
  height: 100%;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 16px;
}

.monitor-card {
  height: 300px;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  padding: 12px 16px;
  margin-bottom: 0;
}

.current-value {
  font-weight: bold;
  color: #409EFF;
}

.chart-container {
  width: 100%;
  height: calc(100% - 12px); /* 减去header的高度 */
  padding: 0 8px 8px;
}

/* 移除卡片默认的padding，让内容更紧凑 */
:deep(.el-card__body) {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .monitor-grid {
    grid-template-columns: 1fr;
  }
  
  .monitor-card {
    height: 250px;
  }
  
  .chart-container {
    height: calc(100% - 48px); /* 减去header的高度 */
  }
}
</style>