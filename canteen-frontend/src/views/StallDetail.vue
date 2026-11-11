<template>
  <div class="page-container stall-detail" v-loading="loading">
    <el-button class="back-btn" round @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      返回美食探索
    </el-button>

    <el-empty v-if="!stall && !loading" description="档口不存在或已被删除">
      <el-button type="primary" @click="goBack">返回美食探索</el-button>
    </el-empty>

    <template v-if="stall">
      <!-- 档口信息头 -->
      <el-card shadow="never" class="stall-hero">
        <div class="hero-row">
          <div class="hero-avatar">
            <img v-if="stall.merchantAvatar" :src="stall.merchantAvatar" :alt="stall.name" @error="$event.target.style.display='none'">
            <img v-else-if="stall.coverImage" :src="stall.coverImage" :alt="stall.name" @error="$event.target.style.display='none'">
            <span v-else class="hero-initial">{{ (stall.name || '档').slice(0, 1) }}</span>
          </div>
          <div class="hero-info">
            <h1 class="hero-name">{{ stall.name }}</h1>
            <div class="hero-tags">
              <el-tag size="small" effect="plain">{{ stall.windowNo }} 号窗口</el-tag>
              <el-tag size="small" effect="plain">{{ zoneText }}</el-tag>
              <el-tag v-if="stall.category" size="small" effect="plain" type="warning">{{ stall.category }}</el-tag>
            </div>
            <div class="hero-stats">
              <span class="stat">
                <el-icon><StarFilled /></el-icon>
                {{ stall.avgRating ? Number(stall.avgRating).toFixed(1) : '—' }} 分
              </span>
              <span class="stat">月售 {{ stall.salesCount || 0 }}</span>
              <span class="stat">好评率 {{ stall.goodRate ? Number(stall.goodRate).toFixed(0) : 0 }}%</span>
              <span v-if="stall.hygieneScore" class="stat hygiene" :class="{ 'hygiene-low': stall.hygieneScore < 80 }">
                卫生 {{ Number(stall.hygieneScore).toFixed(0) }} 分
              </span>
            </div>
            <div v-if="stall.signatureDishes && stall.signatureDishes.length" class="hero-signature">
              <span class="sig-label">招牌菜</span>
              <el-tag v-for="d in stall.signatureDishes.slice(0, 3)" :key="d.id" size="small" effect="plain" type="warning">{{ d.name }}</el-tag>
            </div>
            <p v-if="stall.description" class="hero-desc">{{ stall.description }}</p>
          </div>
        </div>
      </el-card>

      <!-- 菜品列表 -->
      <el-card shadow="never" class="dish-list-card">
        <template #header>
          <span style="font-weight: 600">全部菜品 · {{ dishes.length }} 道</span>
        </template>

        <div v-loading="dishesLoading" class="dish-grid">
          <div
            v-for="dish in dishes"
            :key="dish.id"
            class="dish-item"
            :class="{ highlighted: highlightId === dish.id }"
          >
            <div class="dish-cover" :style="{ background: coverGradient(dish.category) }" @click="goDish(dish)">
              <img v-if="dish.image" :src="dish.image" :alt="dish.name" loading="lazy" @error="$event.target.style.display='none'">
              <span v-else class="dish-initial">{{ (dish.name || '菜').slice(0, 1) }}</span>
              <span v-if="dish.avgRating" class="dish-score">{{ Number(dish.avgRating).toFixed(1) }}</span>
              <div v-if="dishTags(dish).length" class="dish-tags">
                <span v-for="t in dishTags(dish)" :key="t.text" class="dish-tag" :class="'tag-' + t.type">{{ t.text }}</span>
              </div>
            </div>
            <div class="dish-info" @click="goDish(dish)">
              <div class="dish-name" :title="dish.name">{{ dish.name }}</div>
              <div class="dish-meta">月售 {{ dish.salesCount || 0 }}</div>
              <div class="dish-price">¥{{ priceText(dish.price) }}</div>
            </div>
            <button class="dish-add" title="加入购物车" @click.stop="addCart(dish)">
              <el-icon><Plus /></el-icon>
            </button>
          </div>
        </div>
        <el-empty v-if="!dishesLoading && !dishes.length" description="该档口暂无菜品" />
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStall } from '@/api/stall'
import { pageDishes } from '@/api/dish'
import { addToCart } from '@/api/cart'
import { coverGradient } from '@/utils/dishImage'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const dishesLoading = ref(false)
const stall = ref(null)
const dishes = ref([])
const addingId = ref(null)

// 从哪个菜品点进来的，用于高亮
const highlightId = computed(() => {
  const h = route.query.highlight
  return h ? Number(h) : null
})

const zoneText = computed(() => {
  const s = stall.value
  if (!s) return ''
  if (s.area === 'north') return '北区'
  return `南区 ${s.floor || 1} 楼`
})

async function loadStall() {
  loading.value = true
  try {
    const res = await getStall(route.params.id)
    stall.value = res.data
  } catch {
    stall.value = null
  } finally {
    loading.value = false
  }
}

async function loadDishes() {
  dishesLoading.value = true
  try {
    const res = await pageDishes({
      stallId: route.params.id,
      current: 1,
      size: 100,
      sortBy: 'rating'
    })
    dishes.value = res.data?.records || []
  } catch {
    dishes.value = []
  } finally {
    dishesLoading.value = false
  }
}

function goBack() {
  router.push('/dishes')
}

function goDish(dish) {
  if (dish && dish.id) router.push(`/dishes/${dish.id}`)
}

async function addCart(dish) {
  if (addingId.value) return
  addingId.value = dish.id
  try {
    await addToCart(dish.id, 1)
    window.dispatchEvent(new CustomEvent('canteen:cart-changed'))
    ElMessage.success(`已加入购物车：${dish.name}`)
  } catch {
    // 请求层已统一提示
  } finally {
    addingId.value = null
  }
}

function priceText(p) {
  if (p == null) return '—'
  return Number(p).toFixed(2)
}

// 根据菜品数据生成特色标签，最多 2 个，展现每个商家的菜品特点
function dishTags(dish) {
  const tags = []
  if (dish.featured === 1) tags.push({ text: '招牌', type: 'gold' })
  if (dish.goodRate != null && dish.goodRate >= 85 && dish.reviewCount >= 5) tags.push({ text: '好评如潮', type: 'green' })
  if (dish.salesCount >= 200) tags.push({ text: '热销', type: 'orange' })
  else if (dish.reviewCount != null && dish.reviewCount <= 3) tags.push({ text: '新品', type: 'blue' })
  return tags.slice(0, 2)
}

onMounted(async () => {
  await loadStall()
  await loadDishes()
})

// keep-alive 复用：切换档口 id 时重新加载
watch(
  () => (route.name === 'StallDetail' ? route.params.id : null),
  async (id) => {
    if (!id) return
    stall.value = null
    dishes.value = []
    await loadStall()
    await loadDishes()
  }
)
</script>

<style scoped>
.stall-detail {
  padding: 16px 22px 24px;
}

.back-btn {
  margin-bottom: 12px;
}

/* 档口信息头 */
.stall-hero {
  margin-bottom: 16px;
}

.hero-row {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.hero-avatar {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--border);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-initial {
  font-size: 32px;
  font-weight: 700;
  color: var(--brand);
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-name {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 800;
  color: var(--text-1);
}

.hero-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.hero-stats {
  display: flex;
  gap: 18px;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-2);
}

.hero-stats .stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.hero-stats .stat .el-icon {
  color: #f59e0b;
}

.hero-desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-3);
}

.hygiene {
  color: #16a34a;
  font-weight: 700;
}

.hygiene-low {
  color: #e11d48;
}

.hero-signature {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.sig-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand);
}

/* 菜品网格 */
.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}

.dish-item {
  position: relative;
  background: var(--surface);
  border: 2px solid transparent;
  border-radius: var(--radius);
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.dish-item:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md, 0 8px 22px rgba(0, 0, 0, 0.1));
}

/* 从美食广场点进来的菜品：橙色边框高亮 */
.dish-item.highlighted {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-soft);
}

.dish-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
}

.dish-cover img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-initial {
  font-size: 34px;
  font-weight: 700;
  color: #fff;
  position: relative;
  z-index: 1;
}

.dish-score {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  min-width: 34px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: rgba(255, 122, 26, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}

/* 菜品特色标签 */
.dish-tags {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  max-width: calc(100% - 48px);
}

.dish-tag {
  height: 20px;
  padding: 0 7px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
  color: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.16);
}

.dish-tag.tag-gold { background: linear-gradient(135deg, #ffb400, #ff7a00); }
.dish-tag.tag-green { background: linear-gradient(135deg, #22c55e, #16a34a); }
.dish-tag.tag-orange { background: linear-gradient(135deg, #fb7185, #e11d48); }
.dish-tag.tag-blue { background: linear-gradient(135deg, #38bdf8, #0284c7); }

.dish-info {
  padding: 10px 12px 12px;
  cursor: pointer;
}

.dish-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

.dish-meta {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}

.dish-price {
  font-size: 16px;
  font-weight: 800;
  color: var(--brand);
  margin-top: 6px;
}

.dish-add {
  position: absolute;
  right: 10px;
  bottom: 10px;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(140deg, #ff9a3d, var(--brand));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 3px 8px rgba(255, 122, 26, 0.35);
  transition: transform 0.15s ease;
}

.dish-add:hover {
  transform: scale(1.1);
}
</style>
