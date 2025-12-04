<template>
  <el-menu
    :default-active="activeIndex"
    class="sidebar-menu"
    background-color="#001529"
    text-color="#fff"
    active-text-color="#ffd04b"
    router
    :collapse="isCollapse"
  >
    <el-menu-item index="/">
      <el-icon><House /></el-icon>
      <template #title>系统概览</template>
    </el-menu-item>
    <el-menu-item index="/clients">
      <el-icon><User /></el-icon>
      <template #title>客户端列表</template>
    </el-menu-item>
    <el-menu-item index="/tasks">
      <el-icon><List /></el-icon>
      <template #title>任务列表</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { House, User, List } from '@element-plus/icons-vue'

// 获取当前路由
const route = useRoute()

// 根据当前路由动态设置activeIndex
const activeIndex = computed(() => route.path)

const isCollapse = ref(false)
</script>

<style scoped>
.sidebar-menu {
  height: 100%;
  width: 100%;
}
</style>