<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
</script>

<template>
  <div class="app-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <Sidebar />
    </aside>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <div class="content-wrapper">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 200px;
  background-color: #001529;
  color: #fff;
  overflow-y: auto;
}

.main-content {
  flex: 1;
  background-color: #f0f2f5;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.content-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

#app {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 确保所有路由组件都能占满高度 */
.router-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
