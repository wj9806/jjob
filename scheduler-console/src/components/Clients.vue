<template>
  <div class="clients-container">
    <el-card shadow="hover" style="width: 100%; height: 100%;">
      <template #header>
        <div class="card-header">
          <span>客户端列表</span>
          <el-button
            type="primary"
            size="small"
            icon="Refresh"
            @click="handleRefresh"
          >
            刷新
          </el-button>
        </div>
      </template>
      
      <div class="table-container">
        <el-table
          :data="clientsList"
          stripe
          style="width: 100%"
          size="small"
          @row-click="handleRowClick"
          fit
        >
          <el-table-column prop="clientId" label="客户端ID" min-width="180" />
          <el-table-column prop="applicationName" label="客户端名称" min-width="180" />
          <el-table-column prop="hostName" label="主机名" min-width="200" />
          <el-table-column prop="ipAddress" label="IP地址" min-width="180" />
          <el-table-column prop="status" label="状态" min-width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'online' ? 'success' : 'danger'">
                {{ scope.row.status === 'online' ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="180">
            <template #default="scope">
              {{ formatDate(scope.row.lastHeartbeat) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="120">
            <template #default="scope">
              <el-button
                type="primary"
                size="small"
                @click.stop="viewClientDetail(scope.row)"
              >
                详情
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
            :total="clientsList.length"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
      
      <!-- 客户端详情对话框 -->
      <el-dialog
        v-model="clientDetailVisible"
        title="客户端详情"
        width="600px"
      >
        <div v-if="selectedClient" class="client-detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户端ID">{{ selectedClient.clientId }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ selectedClient.applicationName }}</el-descriptions-item>
            <el-descriptions-item label="主机名">{{ selectedClient.hostName }}</el-descriptions-item>
            <el-descriptions-item label="IP地址">{{ selectedClient.ipAddress }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedClient.status === 'online' ? 'success' : 'danger'">
                {{ selectedClient.status === 'online' ? '在线' : '离线' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最后心跳">{{ formatDate(selectedClient.lastHeartbeat) }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import apiClient from '../utils/axios'

// 客户端接口
interface Client {
  clientId: string
  applicationName: string
  hostName: string
  ipAddress: string
  status: 'online' | 'offline'
  lastHeartbeat: number
}

// 数据
const clientsList = ref<Client[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const clientDetailVisible = ref(false)
const selectedClient = ref<Client | null>(null)

// 格式化日期
const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

// 获取客户端列表
const fetchClients = async () => {
  try {
    const response = await apiClient.get('/clients')
    const clients = response.data
    
    // 将对象转换为数组
    if (typeof clients === 'object' && clients !== null) {
      clientsList.value = Object.values(clients).map((client: any) => ({
        clientId: client.clientId,
        applicationName: client.applicationName || '未知客户端',
        hostName: client.hostName || '未知主机',
        ipAddress: client.ipAddress || '未知IP',
        status: 'online', // 假设所有返回的客户端都是在线的
        lastHeartbeat: client.lastHeartbeatTime || Date.now() // 使用后端返回的lastHeartbeatTime字段
      }))
    }
  } catch (error) {
    console.error('Failed to fetch clients:', error)
  }
}

// 处理行点击
const handleRowClick = (row: Client) => {
  console.log('Row clicked:', row)
}

// 查看客户端详情
const viewClientDetail = (row: Client) => {
  selectedClient.value = row
  clientDetailVisible.value = true
}

// 分页处理
const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize
  currentPage.value = 1
}

const handleCurrentChange = (newPage: number) => {
  currentPage.value = newPage
}

// 刷新客户端列表
const handleRefresh = () => {
  fetchClients()
}

// 组件挂载时获取数据
onMounted(() => {
  fetchClients()
})
</script>

<style scoped>
.clients-container {
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

.table-container {
  margin-top: 20px;
  width: 100%;
  min-width: 800px;
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

.client-detail {
  padding: 10px;
}
</style>