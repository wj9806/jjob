<template>
  <div class="tasks-container">
    <el-card shadow="hover" style="width: 100%; height: 100%;">
      <template #header>
        <div class="card-header">
          <span>任务列表</span>
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
            <el-button type="primary" size="small">
              <el-icon><Plus /></el-icon> 新建任务
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 搜索和筛选 -->
      <div class="search-container">
        <el-input
          v-model="searchQuery"
          placeholder="搜索任务名称或ID"
          style="width: 300px; margin-right: 10px;"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="statusFilter"
          placeholder="筛选任务状态"
          style="width: 150px; margin-right: 10px;"
        >
          <el-option label="全部" value="" />
          <el-option label="启用" value="enabled" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
      
      <!-- 任务表格 -->
      <div class="table-container">
        <el-table
          :data="filteredTasks"
          stripe
          style="width: 100%"
          size="small"
          @row-click="handleRowClick"
          fit
        >
          <el-table-column prop="taskId" label="任务ID" min-width="180" />
          <el-table-column prop="taskName" label="任务名称" min-width="200" />
          <el-table-column prop="taskGroup" label="任务组" min-width="120" />
          <el-table-column prop="cronExpression" label="Cron表达式" min-width="180" />
          <el-table-column prop="targetClass" label="任务类" min-width="200" />
          <el-table-column prop="targetMethod" label="目标方法" min-width="100" />
          <el-table-column prop="executionCount" label="执行次数" min-width="100" />
          <el-table-column prop="oneRunning" label="只允许一个实例" min-width="120">
            <template #default="scope">
              <el-switch
                v-model="scope.row.oneRunning"
                size="small"
                disabled
              />
            </template>
          </el-table-column>
          <el-table-column prop="scheduleStrategy" label="调度策略" min-width="120" />
          <el-table-column prop="enabled" label="状态" min-width="100">
            <template #default="scope">
              <el-switch
                v-model="scope.row.enabled"
                size="small"
                active-color="#13ce66"
                inactive-color="#ff4949"
                @change="handleStatusChange(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="150">
            <template #default="scope">
              <el-button
                type="primary"
                size="small"
                @click.stop="editTask(scope.row)"
                style="margin-right: 5px;"
              >
                编辑
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click.stop="deleteTask(scope.row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredTasks.length"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import apiClient from '../utils/axios'

// 任务接口
interface Task {
  taskId: string
  taskName: string
  taskGroup: string
  cronExpression: string
  targetClass: string
  targetMethod: string
  params: any
  enabled: boolean
  description: string | null
  executionCount: number
  oneRunning: boolean
  scheduleStrategy: string | null
}

// 数据
const tasksList = ref<Task[]>([])
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 定时刷新相关
const refreshInterval = ref<string>('0') // 0表示不刷新，单位秒
const refreshTimer = ref<number | null>(null)

// 格式化日期 (预留函数，可能后续使用)
// const formatDate = (timestamp: number) => {
//   return new Date(timestamp).toLocaleString()
// }

// 筛选后的任务列表
const filteredTasks = computed(() => {
  return tasksList.value.filter(task => {
    // 搜索筛选
    const matchesSearch = !searchQuery.value || 
      task.taskName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      task.taskId.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      task.taskGroup.toLowerCase().includes(searchQuery.value.toLowerCase())
    
    // 状态筛选
    const matchesStatus = !statusFilter.value ||
      (statusFilter.value === 'enabled' && task.enabled) ||
      (statusFilter.value === 'disabled' && !task.enabled)
    
    return matchesSearch && matchesStatus
  })
})

// 获取任务列表
const fetchTasks = async () => {
  try {
    const response = await apiClient.get('/tasks')
    const tasks = response.data
    
    // 将对象转换为数组
    if (typeof tasks === 'object' && tasks !== null) {
      tasksList.value = Object.entries(tasks).map(([taskId, task]: [string, any]) => ({
        taskId,
        taskName: task.taskName,
        taskGroup: task.taskGroup,
        cronExpression: task.cronExpression,
        targetClass: task.targetClass,
        targetMethod: task.targetMethod,
        params: task.params,
        enabled: task.enabled,
        description: task.description,
        executionCount: task.executionCount,
        oneRunning: task.oneRunning,
        scheduleStrategy: task.scheduleStrategy
      }))
    }
  } catch (error) {
    console.error('Failed to fetch tasks:', error)
  }
}

// 搜索和筛选
const handleSearch = () => {
  // 搜索逻辑已在computed中实现
  currentPage.value = 1
}

// 重置筛选
const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  currentPage.value = 1
}

// 处理行点击
const handleRowClick = (row: Task) => {
  console.log('Row clicked:', row)
}

// 处理任务状态切换
const handleStatusChange = (row: Task) => {
  // 这里需要调用API更新任务状态
  console.log('Task status changed:', row.taskId, row.enabled)
}

// 编辑任务
const editTask = (row: Task) => {
  console.log('Edit task:', row.taskId)
}

// 删除任务
const deleteTask = (taskId: string) => {
  console.log('Delete task:', taskId)
}

// 分页处理
const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize
  currentPage.value = 1
}

const handleCurrentChange = (newPage: number) => {
  currentPage.value = newPage
}

// 刷新任务列表
const handleRefresh = () => {
  fetchTasks()
}

// 启动刷新定时器
const startRefreshTimer = (interval: number) => {
  stopRefreshTimer()
  if (interval > 0) {
    refreshTimer.value = window.setInterval(() => {
      fetchTasks()
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

// 组件挂载时获取数据
onMounted(() => {
  fetchTasks()
})

// 组件卸载时清除定时器
onUnmounted(() => {
  stopRefreshTimer()
})
</script>

<style scoped>
.tasks-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-container {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.table-container {
  margin-top: 20px;
  width: 100%;
  min-width: 1000px;
  overflow: auto;
  min-height: 300px;
  height: calc(100vh - 220px); /* 减去顶部导航和其他元素的高度 */
  display: flex;
  flex-direction: column;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
}
</style>