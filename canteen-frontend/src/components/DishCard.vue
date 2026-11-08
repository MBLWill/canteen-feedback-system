<template>
  <div class="dish-card" @click="goDetail">
    <!-- 图片区：正方形，右上角评分角标，左上角人气标签 -->
    <div class="dish-cover" :style="{ background: coverGradient(dish.category) }">
      <img
        v-if="dish.image && !broken"
        class="dish-photo"
        :src="dish.image"
        :alt="dish.name"
        loading="lazy"
        @error="broken = true"
      >
      <span v-else class="dish-initial">{{ dish.name.slice(0, 1) }}</span>

      <span v-if="isHot" class="dish-hot">人气推荐</span>
      <span v-if="dish.avgRating" class="dish-score">{{ Number(dish.avgRating).toFixed(1) }}</span>
    </div>

    <!-- 信息区 -->
    <div class="dish-body">
      <div class="dish-name" :title="dish.name">{{ dish.name }}</div>
      <div v-if="subtitle" class="dish-subtitle">{{ subtitle }}</div>
      <div class="dish-meta">
        <span class="meta-rating">
          <el-icon><StarFilled /></el-icon>
          <span>{{ ratingText }}</span>
        </span>
        <span class="meta-dot">·</span>
        <span>月售{{ dish.salesCount || 0 }}</span>
      </div>
      <div class="dish-footer">
        <span class="dish-price">
          <span class="price-symbol">¥</span>
          <span class="price-num">{{ priceInt }}</span>
          <span class="price-decimal">.{{ priceDecimal }}</span>
        </span>
        <button
          v-if="showAdd"
          class="dish-add"
          :class="{ loading: adding }"
          :disabled="adding"
          @click.stop="handleAdd"
        >
          <el-icon v-if="!adding"><Plus /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addToCart } from '@/api/cart'
import { coverGradient } from '@/utils/dishImage'

const props = defineProps({
  dish: { type: Object, required: true },
  // 美食广场「罗列菜品」去掉加购按钮；收藏页等处仍可保留
  showAdd: { type: Boolean, default: true }
})

const router = useRouter()
const broken = ref(false)
const adding = ref(false)
watch(() => props.dish.image, () => { broken.value = false })

const priceInt = computed(() => {
  const p = Number(props.dish.price) || 0
  return Math.floor(p)
})
const priceDecimal = computed(() => {
  const p = Number(props.dish.price) || 0
  return String(Math.round((p - Math.floor(p)) * 100)).padStart(2, '0')
})
const ratingText = computed(() => (props.dish.avgRating
  ? Number(props.dish.avgRating).toFixed(1)
  : '—'))
// 评分达到好评阈值（4）即展示「人气推荐」标签
const isHot = computed(() => (props.dish.avgRating != null && Number(props.dish.avgRating) >= 4))
const subtitle = computed(() => {
  if (props.dish.description) return props.dish.description
  if (props.dish.category) return `${props.dish.category} · 招牌美味`
  return ''
})

async function handleAdd() {
  adding.value = true
  try {
    await addToCart(props.dish.id, 1)
    window.dispatchEvent(new CustomEvent('canteen:cart-changed'))
    ElMessage.success(`已加入购物车：${props.dish.name}`)
  } finally {
    adding.value = false
  }
}

function goDetail() {
  router.push(`/dishes/${props.dish.id}`)
}
</script>

<style scoped>
.dish-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.dish-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md, 0 8px 22px rgba(0, 0, 0, 0.1));
}

.dish-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.dish-photo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.dish-card:hover .dish-photo {
  transform: scale(1.05);
}

.dish-initial {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 3px 10px rgba(180, 70, 0, 0.35);
  position: relative;
  z-index: 1;
}

.dish-hot {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  height: 20px;
  padding: 0 8px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ffb400, #ff7a00);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.16);
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
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
}

.dish-body {
  padding: 10px 12px 12px;
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

.dish-subtitle {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
}

.meta-rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #f59e0b;
  font-weight: 700;
}

.meta-dot {
  color: var(--border-strong);
}

.dish-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.dish-price {
  color: var(--brand);
  font-weight: 700;
  line-height: 1;
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 13px;
  font-weight: 600;
  margin-right: 1px;
}

.price-num {
  font-size: 20px;
  font-weight: 800;
}

.price-decimal {
  font-size: 13px;
  font-weight: 700;
}

.dish-add {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(140deg, #ff9a3d, var(--brand));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 3px 8px rgba(255, 122, 26, 0.35);
  transition: transform 0.15s ease;
}

.dish-add:hover {
  transform: scale(1.1);
}

.dish-add.loading {
  opacity: 0.6;
  pointer-events: none;
}
</style>