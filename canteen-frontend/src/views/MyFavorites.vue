<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col v-for="dish in list" :key="dish.id" :span="6" style="margin-bottom: 16px">
        <DishCard :dish="dish" />
      </el-col>
    </el-row>

    <el-empty v-if="!list.length && !loading" description="还没有收藏的菜品～" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DishCard from '@/components/DishCard.vue'
import { listFavorites, removeFavorite } from '@/api/favorite'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await listFavorites()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
