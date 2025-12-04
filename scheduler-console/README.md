# 分布式任务调度系统 - Web控制台

## 项目概述

这是分布式任务调度系统的Web控制台模块，基于Vue 3 + TypeScript + Vite开发，提供可视化界面用于查看客户端信息和监控任务状态。

## 功能特性

- 显示所有已注册的客户端信息
- 实时监控客户端状态
- 查看任务执行历史记录
- 任务状态可视化展示

## 技术栈

- Vue 3
- TypeScript
- Vite
- Axios
- Element Plus

## 快速开始

### 环境要求

- Node.js 16+
- npm 8+

### 安装和启动

```bash
# 安装依赖
npm install

# 开发环境启动
npm run dev

# 构建生产版本
npm run build

# 生产环境预览
npm run preview
```

### 访问地址

启动后，默认访问地址为：
```
http://localhost:5173
```

## 配置说明

### API配置

控制台需要连接到scheduler-server的API接口，配置文件位于：
```
src/utils/api.ts
```

默认配置为：
```typescript
export const API_BASE_URL = 'http://localhost:8080/api';
```

如果scheduler-server部署在其他地址，请修改此配置。

## 项目结构

```
scheduler-console/
├── public/              # 静态资源
├── src/
│   ├── assets/          # 资源文件
│   ├── components/      # Vue组件
│   ├── router/          # 路由配置
│   ├── utils/           # 工具函数
│   │   └── api.ts       # API配置和请求
│   ├── App.vue          # 根组件
│   └── main.ts          # 入口文件
├── index.html           # HTML模板
├── package.json         # 项目配置
├── tsconfig.json        # TypeScript配置
└── vite.config.ts       # Vite配置
```

## 开发指南

### 组件开发

使用Vue 3的`<script setup>`语法开发组件，示例：

```vue
<template>
  <div class="client-list">
    <h2>客户端列表</h2>
    <!-- 组件内容 -->
  </div>
</template>

<script setup lang="ts">
// 组件逻辑
import { ref, onMounted } from 'vue';
import { getClients } from '../utils/api';

const clients = ref([]);

onMounted(async () => {
  clients.value = await getClients();
});
</script>

<style scoped>
/* 组件样式 */
.client-list {
  padding: 20px;
}
</style>
```

### API调用

使用封装好的API函数进行接口调用：

```typescript
import { getClients, getStatus } from './api';

// 获取所有客户端信息
const clients = await getClients();

// 获取系统状态
const status = await getStatus();
```

## 构建和部署

### 构建生产版本

```bash
npm run build
```

构建产物将生成在`dist`目录下。

### 部署

将`dist`目录下的文件部署到Web服务器即可，如Nginx、Apache等。

## 注意事项

1. 确保scheduler-server服务正常运行
2. 正确配置API_BASE_URL指向scheduler-server的API接口
3. 开发环境下可能需要解决跨域问题，可以在vite.config.ts中配置代理

## 许可证

MIT License
