<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">销售情况</span>
          <el-button size="small" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="12">
          <div class="chart-title">近 7 天各菜销量</div>
          <EChart :option="weeklyOption" height="320px" />
        </el-col>
        <el-col :span="12">
          <div class="chart-title">近 30 天各菜销量</div>
          <EChart :option="monthlyOption" height="320px" />
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 8px">
        <el-col :span="24">
          <div class="chart-head">
            <span class="chart-title">每日收入（元）</span>
            <el-radio-group v-model="dailyDays" size="small" @change="loadDaily">
              <el-radio-button :value="7">近7天</el-radio-button>
              <el-radio-button :value="30">近一月</el-radio-button>
              <el-radio-button :value="180">近半年</el-radio-button>
              <el-radio-button :value="365">近一年</el-radio-button>
            </el-radio-group>
          </div>
          <EChart :option="dailyOption" height="320px" />
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 8px">
        <el-col :span="12">
          <div class="chart-title">卖得好的菜（TOP3）</div>
          <EChart :option="bestOption" height="300px" />
        </el-col>
        <el-col :span="12">
          <div class="chart-title">卖得差的菜（BOTTOM3）</div>
          <EChart :option="worstOption" height="300px" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import EChart from '@/components/EChart.vue'
import { getSalesOverview, getDailyIncome } from '@/api/sales'

const weekly = ref([])
const monthly = ref([])
const daily = ref([])
const dailyDays = ref(7)

const ORANGE = '#f97316'

const weeklyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 60 },
  xAxis: { type: 'category', data: weekly.value.map(d => d.dishName), axisLabel: { rotate: 30 } },
  yAxis: { type: 'value', name: '份' },
  series: [{
    type: 'bar',
    data: weekly.value.map(d => d.quantity),
    itemStyle: { color: ORANGE, borderRadius: [4, 4, 0, 0] },
    barMaxWidth: 32
  }]
}))

const monthlyOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 份（{d}%）' },
  legend: { orient: 'vertical', right: 10, top: 'center' },
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    center: ['38%', '50%'],
    data: monthly.value.map(d => ({ name: d.dishName, value: d.quantity })),
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{c}份' }
  }]
}))

const dailyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 20, bottom: 40 },
  xAxis: { type: 'category', data: daily.value.map(d => d.saleDate) },
  yAxis: { type: 'value', name: '元' },
  series: [{
    type: 'line',
    smooth: true,
    data: daily.value.map(d => Number(d.amount)),
    itemStyle: { color: ORANGE },
    areaStyle: { color: 'rgba(249, 115, 22, 0.12)' }
  }]
}))

function rankOption(list, reverse) {
  const data = reverse ? [...list].reverse() : list
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 90, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', name: '份' },
    yAxis: { type: 'category', data: data.map(d => d.dishName) },
    series: [{
      type: 'bar',
      data: data.map(d => d.quantity),
      itemStyle: { color: reverse ? '#94a3b8' : '#e0353b', borderRadius: [0, 4, 4, 0] },
      barMaxWidth: 20
    }]
  }
}

const bestOption = computed(() => rankOption(monthly.value.slice(0, 3), false))
const worstOption = computed(() => rankOption(monthly.value.slice(-3), true))

async function loadData() {
  const res = await getSalesOverview()
  weekly.value = res.data.weekly || []
  monthly.value = res.data.monthly || []
  loadDaily()
}

async function loadDaily() {
  const res = await getDailyIncome(dailyDays.value)
  daily.value = res.data || []
}

onMounted(loadData)
</script>

<style scoped>
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}
</style>
