<template>
  <div v-if="slides.length" class="banner-block" :class="{ 'banner-block--hero': variant === 'hero' }">
    <div class="banner-wrap">
      <div
        v-for="(s, i) in slides"
        :key="s.key"
        class="banner-slide"
        :class="{ active: i === activeIndex, clickable: !!s.link }"
        @click="go(s)"
      >
        <img
          v-if="s.type === 'local'"
          :src="s.image"
          :alt="s.alt"
          class="banner-img"
          :loading="i === 0 ? 'eager' : 'lazy'"
        >
        <!-- 后台广告：图片铺底 + 文案层，避免 1:1 菜品图被直接裁成一条 -->
        <div v-else class="banner-ad" :style="{ backgroundImage: s.background }">
          <span class="banner-ad-emoji">{{ s.emoji }}</span>
          <span class="banner-ad-text">
            <span class="banner-ad-title">{{ s.alt }}</span>
            <span v-if="s.subtitle" class="banner-ad-subtitle">{{ s.subtitle }}</span>
          </span>
        </div>
      </div>
    </div>

    <div class="banner-dots">
      <span
        v-for="(_, i) in slides"
        :key="'dot-' + i"
        class="dot"
        :class="{ active: i === activeIndex }"
        @click.stop="jump(i)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listBanners } from '@/api/banner'

const router = useRouter()
const activeIndex = ref(0)
let timer = null

/**
 * variant: 'normal'（默认，首页看板等紧凑横幅）| 'hero'（美食广场等大屏主视觉）
 * 素材 banner*.jpg 为 1600×460，hero 模式按原始比例铺满，避免被裁成一条。
 */
defineProps({
  variant: { type: String, default: 'normal' }
})

const INTERVAL = 5000

/**
 * 内置横幅（设计稿素材）。这些图自带完整文案与按钮，直接铺满展示，不叠加文字层。
 * 仅在后台没有任何启用中的广告时使用。
 */
const LOCAL_SLIDES = [
  { key: 'local-1', type: 'local', image: '/ui/banner/banner1.jpg', alt: '美味食堂，从一份好评开始', link: '/stalls/south' },
  { key: 'local-2', type: 'local', image: '/ui/banner/banner2.jpg', alt: '美味食堂，从一份好评开始', link: '/dishes' },
  { key: 'local-3', type: 'local', image: '/ui/banner/banner3.jpg', alt: '吃得开心，才是正经事', link: '/stalls/north' }
]

const remoteBanners = ref([])

// 后台启用的广告优先展示；后台没有启用中的广告时回退到内置设计图
const slides = computed(() => {
  if (!remoteBanners.value.length) return LOCAL_SLIDES
  return remoteBanners.value.map(b => {
    const base = b.bgColor || '#ff7a1a'
    return {
      key: 'b' + b.id,
      type: 'remote',
      background: b.image
        ? `linear-gradient(100deg, rgba(0,0,0,.62), rgba(0,0,0,.12)), url("${b.image}")`
        : `linear-gradient(120deg, ${base}, rgba(0,0,0,.55))`,
      emoji: b.emoji || '🍽️',
      alt: b.title || '食堂推广',
      subtitle: b.subtitle || '',
      link: b.link
    }
  })
})

function startTimer() {
  stopTimer()
  if (slides.value.length <= 1) return
  timer = setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % slides.value.length
  }, INTERVAL)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function jump(i) {
  activeIndex.value = i
  startTimer()
}

function go(s) {
  const link = (s.link || '').trim()
  if (!link) return
  // 后台可能填外链，路由跳转处理不了绝对地址
  if (/^https?:\/\//i.test(link)) {
    window.open(link, '_blank', 'noopener')
    return
  }
  // 只允许站内已知路径，非法路径（如 /stalls/5、/foo）会命中 catch-all 重定向到 /dashboard
  // 这里做白名单校验，避免用户点了广告却跳去看板
  const path = link.startsWith('/') ? link : '/' + link
  const allowed = /^\/(dishes|stalls\/south|stalls\/north|my\/(orders|reviews|favorites)|feedbacks|notices|profile)(\?.*)?$/.test(path)
  if (allowed) {
    router.push(path)
  } else {
    router.push('/dishes')
  }
}

onMounted(async () => {
  // 先启动轮播，避免被后面的网络请求阻塞（后端慢或挂起时轮播会停住）
  startTimer()

  // 拉取后台启用的广告；失败时保留内置设计图
  try {
    const res = await listBanners()
    remoteBanners.value = res.data || []
    startTimer()
  } catch {
    remoteBanners.value = []
  }
})

// 页面被 keep-alive 缓存时，切回来要恢复轮播
onActivated(startTimer)
onDeactivated(stopTimer)
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.banner-block {
  margin-bottom: 16px;
}

.banner-wrap {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(255, 140, 40, 0.14);
  height: 140px;
  background: #fff7ef;
}

.banner-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity .5s ease;
  /* 非当前帧必须屏蔽点击，否则叠在最上层的最后一帧会吞掉所有点击 */
  pointer-events: none;
}

.banner-slide.active {
  opacity: 1;
  pointer-events: auto;
}

.banner-slide.clickable {
  cursor: pointer;
}

.banner-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.banner-ad {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 4%;
  color: #fff;
  background-size: cover;
  background-position: center;
  background-color: #ff7a1a;
}

.banner-ad-emoji {
  font-size: clamp(20px, 3vw, 34px);
  margin-right: 14px;
}

.banner-ad-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.banner-ad-title {
  font-size: clamp(14px, 1.6vw, 20px);
  font-weight: 700;
  text-shadow: 0 1px 6px rgba(0, 0, 0, .35);
}

.banner-ad-subtitle {
  font-size: clamp(11px, 1.1vw, 14px);
  opacity: .92;
  text-shadow: 0 1px 6px rgba(0, 0, 0, .35);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-dots {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  padding: 10px 0 0;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d4c8bd;
  cursor: pointer;
  transition: all .25s ease;
}

.dot:hover {
  background: #ff7a1a;
}

.dot.active {
  width: 22px;
  border-radius: 4px;
  background: #ff7a1a;
}

@media (max-width: 1280px) {
  .banner-wrap {
    height: 120px;
  }
}

/* 美食广场主视觉：靠父容器 flex stretch 拉伸到与 AI 小助手等高，min-height 兜底避免塌陷 */
.banner-block--hero {
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  position: relative;
}
.banner-block--hero .banner-wrap {
  flex: 1;
  min-height: 200px;
  height: auto;
  aspect-ratio: auto;
}
.banner-block--hero .banner-dots {
  position: absolute;
  bottom: 14px;
  left: 0;
  right: 0;
}
</style>