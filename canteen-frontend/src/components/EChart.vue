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
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  instance.value && instance.value.dispose()
})
</script>
