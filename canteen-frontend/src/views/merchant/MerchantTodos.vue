<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">待办事项</span>
          <el-button size="small" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-empty v-if="!todos.length && !loading" description="暂无待办，一切正常" />

      <div v-for="t in todos" :key="t.id" class="todo-item" :class="{ 'todo-warn': t.type === 'BAD_REVIEW' }">
        <div class="todo-badge">{{ t.type === 'INSPECT' ? '整改' : '预警' }}</div>
        <div class="todo-main">
          <div class="todo-title">{{ t.title }}</div>
          <div v-if="t.content" class="todo-content">{{ t.content }}</div>
          <div class="todo-time">{{ t.createTime }}</div>
        </div>
        <el-button type="primary" plain size="small" @click="handleDone(t)">已处理</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listTodos, doneTodo } from '@/api/merchant'

const loading = ref(false)
const todos = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await listTodos()
    todos.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleDone(t) {
  await doneTodo(t.id)
  ElMessage.success('已标记处理')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.todo-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
}

.todo-item.todo-warn {
  border-color: #fecaca;
  background: #fff7f7;
}

.todo-badge {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: #f59e0b;
}

.todo-warn .todo-badge {
  background: #e0353b;
}

.todo-main {
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
}

.todo-content {
  font-size: 13px;
  color: var(--text-2);
  margin-top: 4px;
}

.todo-time {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}
</style>
