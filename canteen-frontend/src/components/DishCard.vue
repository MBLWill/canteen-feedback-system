<template>
  <el-card class="dish-card" shadow="never" @click="goDetail">
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

      <span v-if="dish.avgRating" class="dish-score">
        <el-icon><Star /></el-icon>
        {{ Number(dish.avgRating).toFixed(1) }}
      </span>
    </div>

    <div class="dish-body">
      <div class="dish-name">{{ dish.name }}</div>
      <div class="dish-stall">
        <el-icon :size="12"><Shop /></el-icon>
        {{ dish.stallName }} · {{ dish.windowNo }}号窗口
      </div>
      <div class="dish-footer">
        <span class="dish-price">¥{{ dish.price }}</span>
        <div class="dish-actions">
          <span class="dish-reviews">{{ dish.reviewCount }} 条评价</span>
          <el-button
            class="dish-add"
            type="primary"
            circle
            size="small"
            :loading="adding"
            @click.stop="handleAdd"
          >
            <el-icon v-if="!adding"><Plus /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addToCart } from '@/api/cart'
import { coverGradient } from '@/utils/dishImage'

const props = defineProps({
  dish: { type: Object, required: true }
})

const router = useRouter()
const broken = ref(false)
const adding = ref(false)
watch(() => props.dish.image, () => { broken.value = false })

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
  border-radius: var(--radius-lg) !important;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dish-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md, 0 8px 22px rgba(0, 0, 0, 0.1));
}

.dish-cover {
  position: relative;
  height: 132px;
  border-radius: var(--radius);
  margin: -4px -4px 0;
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
  transform: scale(1.06);
}

.dish-initial {
  font-size: 46px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 3px 10px rgba(180, 70, 0, 0.35);
  position: relative;
  z-index: 1;
}

.dish-score {
  position: absolute;
  right: 10px;
  bottom: 10px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 9px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.dish-body {
  padding: 12px 4px 2px;
}

.dish-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-stall {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 10px;
}

.dish-footer {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.dish-reviews {
  font-size: 12px;
  color: var(--text-3);
}

.dish-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dish-add {
  width: 26px;
  height: 26px;
}

:deep(.el-card__body) {
  padding: 14px;
}
</style>
