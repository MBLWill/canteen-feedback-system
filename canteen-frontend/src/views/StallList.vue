<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <div class="zone-head">
        <div class="zone-title">
          <span class="zone-emoji">{{ area === 'north' ? '🏫' : '🏢' }}</span>
          <div>
            <div class="zone-name">{{ areaLabel }}</div>
            <div class="zone-desc">{{ area === 'north' ? '共一层，直达档口' : '共两层，请选择楼层' }}</div>
          </div>
        </div>
        <el-radio-group v-if="area === 'south'" v-model="floor" class="floor-switch" @change="loadData">
          <el-radio-button :value="1">1 楼</el-radio-button>
          <el-radio-button :value="2">2 楼</el-radio-button>
        </el-radio-group>
      </div>

      <div class="filter-bar" style="margin-bottom: 0; margin-top: 14px">
        <el-input
          v-model="keyword"
          placeholder="搜索档口 / 窗口号 / 品类"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
        <el-select v-model="category" placeholder="全部分类" clearable style="width: 140px" @change="loadData">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="s in filtered" :key="s.id" :xs="24" :sm="12" :md="8" style="margin-bottom: 16px">
        <el-card shadow="never" class="stall-card" @click="goDishes(s)">
          <div class="stall-cover" :style="coverStyle(s)">
            <span v-if="!coverPhoto(s)" class="stall-emoji">{{ emojiOf(s) }}</span>
            <span class="stall-window">{{ s.windowNo }} 号窗口</span>
            <span v-if="isLowHygiene(s)" class="stall-warn">⚠ 卫生待整改</span>
          </div>

          <div class="stall-body">
            <div class="stall-head">
              <span class="stall-name">{{ s.name }}</span>
              <el-tag v-if="s.status !== 1" size="small" type="info">歇业</el-tag>
            </div>

            <div class="stall-meta">
              <el-tag v-if="area === 'south'" size="small" effect="plain" type="success">{{ s.floor }} 楼</el-tag>
              <el-tag size="small" effect="plain" type="warning">{{ s.category || '综合' }}</el-tag>
              <span class="muted">{{ s.location || '位置未标注' }}</span>
            </div>

            <div class="stall-manager">
              <el-icon :size="13"><User /></el-icon>
              负责人：{{ s.managerName || '待分配' }}
              <span v-if="s.managerPhone" class="muted">· {{ s.managerPhone }}</span>
            </div>

            <div class="stall-signature">
              <span class="sig-label">招牌菜</span>
              <el-tag
                v-for="d in s.signatureDishes || []"
                :key="d.id"
                size="small"
                type="danger"
                effect="light"
                round
              >
                {{ d.name }} ¥{{ d.price }}
              </el-tag>
              <span v-if="!s.signatureDishes?.length" class="muted">暂未设置</span>
            </div>

            <div class="stall-hygiene" :class="{ 'is-low': isLowHygiene(s) }">
              <span class="hygiene-label">卫生分</span>
              <b>{{ hygieneText(s) }}</b>
              <span class="hygiene-tag">{{ hygieneLevel(s) }}</span>
            </div>

            <div class="stall-stats">
              <span><b>{{ s.avgRating }}</b> 分</span>
              <span>{{ s.dishCount }} 道菜</span>
              <span>好评 {{ s.goodRate }}%</span>
            </div>

            <div class="stall-actions">
              <el-button type="primary" plain size="small" @click.stop="goDishes(s)">挑选菜品并加购</el-button>
              <el-button type="warning" size="small" @click.stop="openCart">打开购物车</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!filtered.length && !loading" description="暂无档口" />
  </div>

</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listStalls } from '@/api/stall'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const list = ref([])
const keyword = ref('')
const category = ref('')

// 从路由判断当前看的是南区还是北区食堂
const area = computed(() => (route.path.startsWith('/stalls/north') ? 'north' : 'south'))
const areaLabel = computed(() => (area.value === 'north' ? '北区食堂' : '南区食堂'))
const floor = ref(1)


const EMOJI_MAP = {
  川菜: '🌶️',
  粤菜: '🦆',
  面食: '🍜',
  香锅: '🥘',
  清真: '🍢',
  轻食: '🥗',
  茶点: '🥟',
  甜品: '🍰'
}
const GRADIENTS = [
  'linear-gradient(135deg, #ff9a4f, #ff6a00)',
  'linear-gradient(135deg, #ffb457, #f97316)',
  'linear-gradient(135deg, #f97362, #e0353b)',
  'linear-gradient(135deg, #ffa94d, #f59e0b)'
]

const categories = computed(() => [...new Set(list.value.map(s => s.category).filter(Boolean))])

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return list.value.filter(s => {
    const matchKw = !kw
      || s.name.includes(kw)
      || String(s.windowNo).includes(kw)
      || (s.category || '').includes(kw)
      || (s.managerName || '').includes(kw)
    const matchCat = !category.value || s.category === category.value
    return matchKw && matchCat
  })
})

function emojiOf(stall) {
  return EMOJI_MAP[stall.category] || '🍽️'
}

// 用招牌菜实拍图做封面，没有招牌菜时退回渐变色块
function coverPhoto(stall) {
  return stall.coverImage || stall.signatureDishes?.find(d => d.image)?.image || ''
}

function coverStyle(stall) {
  const photo = coverPhoto(stall)
  if (photo) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(0,0,0,0.05), rgba(0,0,0,0.42)), url(${photo})`
    }
  }
  return { backgroundImage: GRADIENTS[stall.id % GRADIENTS.length] }
}

const HYGIENE_WARN = 80

function hygieneText(stall) {
  return stall.hygieneScore == null ? '--' : Number(stall.hygieneScore).toFixed(1)
}

function isLowHygiene(stall) {
  return stall.hygieneScore != null && Number(stall.hygieneScore) < HYGIENE_WARN
}

function hygieneLevel(stall) {
  const v = Number(stall.hygieneScore)
  if (stall.hygieneScore == null) return '未评定'
  if (v >= 90) return '优秀'
  if (v >= 80) return '良好'
  return '待整改'
}

function goDishes(stall) {
  router.push({ path: '/dishes', query: { stallId: stall.id } })
}

function openCart() {
  window.dispatchEvent(new CustomEvent('canteen:open-cart'))
}

async function loadData() {
  loading.value = true
  try {
    const res = await listStalls({
      area: area.value,
      floor: area.value === 'south' ? floor.value : 1
    })
    list.value = res.data
  } catch {
    ElMessage.error('档口加载失败')
  } finally {
    loading.value = false
  }
}

// 南区/北区是同一个组件，切换菜单时复用实例，需要监听路由重新加载
watch(() => route.path, () => {
  floor.value = 1
  category.value = ''
  keyword.value = ''
  loadData()
})

onMounted(loadData)
</script>

<style scoped>
.zone-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.zone-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.zone-emoji {
  font-size: 34px;
  line-height: 1;
}

.zone-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-1);
}

.zone-desc {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
}

.floor-switch :deep(.el-radio-button__inner) {
  padding: 8px 22px;
}

.stall-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.stall-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md, 0 8px 24px rgba(0, 0, 0, 0.08));
}

.stall-cover {
  height: 104px;
  position: relative;
  color: #fff;
  background-size: cover;
  background-position: center;
}

.stall-emoji {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 38px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.25));
}

.stall-window {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.28);
  padding: 2px 10px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.stall-body {
  padding: 14px 16px 16px;
}

.stall-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.stall-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
}

.stall-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  margin-bottom: 8px;
}

.stall-manager {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-2);
  background: #fff7f1;
  border-radius: 8px;
  padding: 5px 9px;
  margin-bottom: 10px;
}

.stall-signature {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  min-height: 26px;
  margin-bottom: 10px;
}

.sig-label {
  color: var(--text-3);
  font-size: 12px;
}

.stall-stats {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--text-3);
  padding-top: 10px;
  border-top: 1px dashed var(--border);
  margin-bottom: 12px;
}

.stall-stats b {
  color: var(--brand-dark);
  font-size: 14px;
}

.stall-warn {
  position: absolute;
  right: 12px;
  top: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: rgba(220, 38, 38, 0.88);
  padding: 3px 10px;
  border-radius: 20px;
  animation: hygiene-blink 2s ease-in-out infinite;
}

@keyframes hygiene-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.62; }
}

.stall-hygiene {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #f0fdf4;
  color: #15803d;
}

.stall-hygiene b {
  font-size: 15px;
}

.stall-hygiene .hygiene-label {
  color: inherit;
  opacity: 0.75;
}

.stall-hygiene .hygiene-tag {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(21, 128, 61, 0.12);
}

.stall-hygiene.is-low {
  background: #fef2f2;
  color: #dc2626;
}

.stall-hygiene.is-low .hygiene-tag {
  background: rgba(220, 38, 38, 0.12);
}

.stall-actions {
  display: flex;
  gap: 8px;
}
</style>
