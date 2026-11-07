<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header><span style="font-weight: 600">我的评价记录</span></template>

      <el-empty v-if="!records.length && !loading" description="你还没有评价过菜品，快去逛逛吧～" />

      <div v-for="r in records" :key="r.id" class="review-item">
        <el-row :gutter="12" align="middle">
          <el-col :span="16">
            <div style="font-weight: 600; margin-bottom: 4px">
              {{ r.dishName }}
              <el-tag size="small" type="info" effect="plain">{{ r.stallName }}</el-tag>
            </div>
            <div class="muted" style="font-size: 12px">{{ r.createTime }}</div>
            <div style="margin-top: 6px">{{ r.content || '（未填写文字评价）' }}</div>
          </el-col>
          <el-col :span="4">
            <StarRating :value="r.rating" readonly />
          </el-col>
          <el-col :span="4" style="text-align: right">
            <el-tag v-if="r.status === 2" type="danger" size="small">已被屏蔽</el-tag>
            <el-tag v-else-if="r.status === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else type="success" size="small">已发布</el-tag>
            <el-button link type="danger" style="margin-left: 8px" @click="handleDelete(r)">删除</el-button>
          </el-col>
        </el-row>
      </div>

      <div style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StarRating from '@/components/StarRating.vue'
import { pageMyReviews, deleteReview } from '@/api/review'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await pageMyReviews(query)
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('删除后不可恢复，确定删除这条评价吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteReview(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.review-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f1f3;
}

.review-item:last-child {
  border-bottom: none;
}
</style>
