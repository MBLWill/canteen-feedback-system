<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, shallowRef, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '360px' }
})

const emit = defineEmits(['click'])

const chartRef = ref(null)
const instance = shallowRef(null)
let observer = null

function render() {
  if (!instance.value) return
  instance.value.setOption(props.option, true)
}

function handleResize() {
  instance.value && instance.value.resize()
}

onMounted(() => {
  instance.value = echarts.init(chartRef.value)
  // 点击图形时把事件透出给父组件（setOption 不会移除该监听）
  instance.value.on('click', params => emit('click', params))
  render()
  window.addEventListener('resize', handleResize)
  // 容器尺寸变化（进/退大屏、侧边栏收起等）不一定触发 window.resize，用 ResizeObserver 兜住
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => handleResize())
    observer.observe(chartRef.value)
  }
})

watch(() => props.option, render, { deep: true })
// 高度从固定像素切成 100% 时容器尺寸会变，补一次 resize 防止图表按旧高度绘制
watch(() => props.height, () => requestAnimationFrame(handleResize))

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (observer) observer.disconnect()
  instance.value && instance.value.dispose()
})
</script>
