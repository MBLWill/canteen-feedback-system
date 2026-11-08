<template>
  <div class="page-container">
    <div class="favorite-toolbar">
      <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
      <span class="selected-hint">已选 {{ selectedIds.length }} 项</span>
      <el-button type="danger" plain :disabled="!selectedIds.length" :loading="batchDeleting" @click="handleBatchCancel">
        批量取消收藏
      </el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="dish in list" :key="dish.id" :xs="24" :sm="12" :md="8" :lg="6" style="margin-bottom: 16px">
        <div class="favorite-item">
          <el-checkbox v-model="selectedIds" :value="dish.id" class="favorite-check" @click.stop />
          <DishCard :dish="dish" />
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!list.length && !loading" description="还没有收藏的菜品～" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DishCard from '@/components/DishCard.vue'
import { listFavorites, removeFavorite } from '@/api/favorite'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const selectedIds = ref([])
const batchDeleting = ref(false)

const allSelected = computed({
  get: () => list.value.length > 0 && selectedIds.value.length === list.value.length,
  set: (val) => {
    selectedIds.value = val ? list.value.map(dish => dish.id) : []
  }
})
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < list.value.length)

async function loadData() {
  loading.value = true
  try {
    const res = await listFavorites()
    list.value = res.data
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

async function handleBatchCancel() {
  const ids = selectedIds.value
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定取消收藏选中的 ${ids.length} 项吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  batchDeleting.value = true
  try {
    const results = await Promise.allSettled(ids.map(id => removeFavorite(id)))
    const success = results.filter(r => r.status === 'fulfilled').length
    const failed = results.length - success
    if (failed) ElMessage.warning(`成功取消 ${success} 项，${failed} 项操作失败`)
    else ElMessage.success(`成功取消 ${success} 项收藏`)
    await loadData()
  } finally {
    batchDeleting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.favorite-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.selected-hint {
  font-size: 12px;
  color: var(--text-3);
}

.favorite-item {
  position: relative;
}

.favorite-check {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.9);
}
</style>
