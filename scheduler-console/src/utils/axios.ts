import axios from 'axios'

// 创建axios实例
const apiClient = axios.create({
  baseURL: '/api', // API基础URL (使用相对路径以便Vite代理生效)
  timeout: 10000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 可以在这里添加认证信息等
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    // 可以在这里统一处理响应数据
    return response
  },
  (error) => {
    // 可以在这里统一处理错误
    console.error('API请求错误:', error)
    return Promise.reject(error)
  }
)

export default apiClient
