<template>
  <div class="dashboard-container">
    <el-card shadow="hover" style="width: 100%; height: 100%;">
      <template #header>
        <div class="card-header">
          <span style="font-size: 30px">分布式任务调度系统概览</span>
          <div style="display: flex; gap: 10px; align-items: center;">
            <el-button
              type="primary"
              size="small"
              icon="Refresh"
              @click="handleRefresh"
            >
              刷新
            </el-button>
            <el-select
              v-model="refreshInterval"
              placeholder="选择刷新间隔"
              size="small"
              style="width: 120px;"
              @change="handleRefreshIntervalChange"
            >
              <el-option label="不刷新" value="0" />
              <el-option label="5s" value="5" />
              <el-option label="10s" value="10" />
              <el-option label="30s" value="30" />
              <el-option label="60s" value="60" />
            </el-select>
          </div>
        </div>
      </template>
      
      <div class="stats-grid">
        <el-card class="stat-card" shadow="hover" @click="handleCardClick('/clients')">
          <div class="stat-content">
            <div class="stat-number">{{ clientCount }}</div>
            <div class="stat-label">在线客户端</div>
          </div>
          <div class="stat-icon">
            <el-icon size="48"><User /></el-icon>
          </div>
        </el-card>
        
        <el-card class="stat-card" shadow="hover" @click="handleCardClick('/tasks')">
          <div class="stat-content">
            <div class="stat-number">{{ taskCount }}</div>
            <div class="stat-label">任务总数</div>
          </div>
          <div class="stat-icon">
            <el-icon size="48"><List /></el-icon>
          </div>
        </el-card>
        
        <el-card class="stat-card" shadow="hover" @click="handleCardClick('/tasks')">
          <div class="stat-content">
            <div class="stat-number">{{ activeTasks }}</div>
            <div class="stat-label">活跃任务</div>
          </div>
          <div class="stat-icon">
            <el-icon size="48"><VideoPlay /></el-icon>
          </div>
        </el-card>
        
        <el-card class="stat-card" shadow="hover" @click="handleCardClick('/tasks')">
          <div class="stat-content">
            <div class="stat-number">{{ completedTasks }}</div>
            <div class="stat-label">已完成任务</div>
          </div>
          <div class="stat-icon">
            <el-icon size="48"><Check /></el-icon>
          </div>
        </el-card>
      </div>
      
      <!-- 系统监控图表 -->
      <div class="monitor-section">
        <h3>系统监控</h3>
        <system-monitor :data="monitorData" />
      </div>
      
      <div class="status-section">
        <h3>系统状态</h3>
        <el-table :data="systemStatus" size="small" @row-click="handleRowClick">
          <el-table-column prop="key" label="项目" width="150" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, List, VideoPlay, Check } from '@element-plus/icons-vue'
import apiClient from '../utils/axios'
import SystemMonitor from './SystemMonitor.vue'

// 路由实例
const router = useRouter()

// 点击统计卡片的处理函数
const handleCardClick = (route: string) => {
  router.push(route)
}

// 点击表格行的处理函数
const handleRowClick = (_row: any) => {
  router.push('/clients')
}

// 监控数据接口
interface MonitorData {
  timestamp: number
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
}

// 系统状态接口
interface SystemStatus {
  clients: number
  totalTasks: number
  enabledTasks: number
  disabledTasks: number
  runningTasks: number
  pendingTasks: number
  completedTasks: number
  failedTasks: number
  systemTime: number
}

// 数据
const clientCount = ref(0)
const taskCount = ref(0)
const activeTasks = ref(0)
const completedTasks = ref(0)
const systemStatus = ref<{ key: string; value: string }[]>([])
// 监控数据
const monitorData = ref<MonitorData[]>([])

// 定时刷新相关
const refreshInterval = ref<string>('10') // 默认10秒刷新一次
const refreshTimer = ref<number | null>(null)

// 格式化日期
const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

// 获取系统状态
const fetchSystemStatus = async () => {
  try {
    // 获取基本状态
    const response = await apiClient.get<SystemStatus>('/status')
    const data = response.data
    
    clientCount.value = data.clients
    taskCount.value = data.totalTasks
    
    // 设置活跃任务和已完成任务
    activeTasks.value = data.enabledTasks
    completedTasks.value = data.completedTasks
    
    // 准备系统状态表格数据
    systemStatus.value = [
      { key: '启用任务', value: data.enabledTasks.toString() },
      { key: '禁用任务', value: data.disabledTasks.toString() },
      { key: '运行中任务', value: data.runningTasks.toString() },
      { key: '已完成任务', value: data.completedTasks.toString() },
      { key: '失败任务', value: data.failedTasks.toString() },
      { key: '最后更新时间', value: formatDate(data.systemTime) }
    ]
    
    // 获取并处理系统监控数据
    await fetchMonitorData()
  } catch (error) {
    console.error('Failed to fetch system status:', error)
  }
}

// 获取系统监控数据
const fetchMonitorData = async () => {
  try {
    const monitorResponse = await apiClient.get<MonitorData>('/monitor')
    const monitor = monitorResponse.data
    
    // 确保返回的数据有效
    if (monitor && typeof monitor.timestamp === 'number' && 
        typeof monitor.cpuUsage === 'number' && 
        typeof monitor.memoryUsage === 'number' && 
        typeof monitor.diskUsage === 'number') {
      
      // 最多显示最近100秒的数据
      const now = Date.now()
      const cutoffTime = now - 100000 // 100秒前的时间戳
      // 过滤旧数据并创建新的数组引用，确保响应式更新
      monitorData.value = [...monitorData.value.filter(item => item.timestamp >= cutoffTime), monitor]
    }
  } catch (monitorError) {
    console.error('Failed to fetch monitor data:', monitorError)
    // 不再添加新的模拟数据，保持原有数据不变
    // 只有当完全没有数据时，才使用模拟数据作为初始值
    if (monitorData.value.length === 0) {
      const mockData: MonitorData = {
        timestamp: Date.now(),
        cpuUsage: 0, // 初始值设为0，而不是随机值
        memoryUsage: 0,
        diskUsage: 0
      }
      monitorData.value.push(mockData)
    }
  }
}

// 刷新系统状态
const handleRefresh = () => {
  fetchSystemStatus()
}

// 启动刷新定时器
const startRefreshTimer = (interval: number) => {
  stopRefreshTimer()
  if (interval > 0) {
    refreshTimer.value = window.setInterval(() => {
      fetchSystemStatus()
    }, interval * 1000)
  }
}

// 停止刷新定时器
const stopRefreshTimer = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

// 处理刷新间隔变化
const handleRefreshIntervalChange = () => {
  const interval = parseInt(refreshInterval.value)
  startRefreshTimer(interval)
}

// 组件挂载时获取数据并设置定时刷新
onMounted(() => {
  fetchSystemStatus()
  
  // 根据选择的间隔启动定时器
  const interval = parseInt(refreshInterval.value)
  startRefreshTimer(interval)
})

// 组件卸载时清除定时器
onUnmounted(() => {
  stopRefreshTimer()
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  height: 120px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.15);
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-top: 4px;
}

.stat-icon {
  color: #409EFF;
  font-size: 40px;
  margin-left: 16px;
}

.monitor-section {
  margin-top: 20px;
  margin-bottom: 20px;
}

.monitor-section h3 {
  margin-bottom: 12px;
  font-size: 16px;
  color: #303133;
}

.status-section {
  margin-top: 24px;
}

.status-section h3 {
  margin-bottom: 12px;
  font-size: 16px;
  color: #303133;
}
</style>