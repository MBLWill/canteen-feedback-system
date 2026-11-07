<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header><span style="font-weight: 600">📢 食堂公告</span></template>

      <el-timeline>
        <el-timeline-item
          v-for="item in list"
          :key="item.id"
          :timestamp="item.createTime"
          placement="top"
          type="primary"
          size="large"
        >
          <el-card shadow="never" style="background: #fffaf5">
            <div style="font-size: 15px; font-weight: 600; margin-bottom: 6px">{{ item.title }}</div>
            <div style="line-height: 1.8; white-space: pre-wrap">{{ item.content }}</div>
            <div class="muted" style="font-size: 12px; margin-top: 8px">
              发布人：{{ item.publisherName }}
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-if="!list.length && !loading" description="暂无公告" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listNotices } from '@/api/notice'

const loading = ref(false)
const list = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await listNotices()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
