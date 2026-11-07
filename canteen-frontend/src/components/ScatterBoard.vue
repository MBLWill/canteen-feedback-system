<template>
  <EChart :option="option" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import EChart from './EChart.vue'

const props = defineProps({
  // dish-美食红黑榜（x=单价 y=评分）；stall-综合档口评价（x=学生评分 y=卫生分）
  mode: { type: String, default: 'dish' },
  data: { type: Array, default: () => [] },
  height: { type: String, default: '400px' }
})

// 涟漪类用 effectScatter 吸引视线，普通类用静态散点避免图面过噪
const STYLE = {
  dish: {
    value: { name: '高性价比（便宜好吃）', color: '#f5b301', ripple: true },
    avoid: { name: '避雷（贵且难吃）', color: '#ef4444', ripple: true },
    new: { name: '新菜品（样本较少）', color: '#3b82f6', ripple: true },
    normal: { name: '一般', color: '#c9ccd4', ripple: false }
  },
  stall: {
    quality: { name: '优质档口（评分卫生双高）', color: '#ef4444', ripple: true },
    bad: { name: '劣质档口（评分卫生双低）', color: '#1f2937', ripple: true },
    normal: { name: '一般档口', color: '#f59e0b', ripple: false }
  }
}

const RIPPLE_ORDER = {
  dish: ['value', 'avoid', 'new', 'normal'],
  stall: ['quality', 'bad', 'normal']
}

const option = computed(() => (props.mode === 'dish' ? dishOption() : stallOption()))

function symbolSize(count, max) {
  if (!count || !max) return 9
  return 9 + Math.sqrt(count / max) * 21
}

function group(list) {
  const map = new Map()
  for (const item of list) {
    const key = item.tag || 'normal'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(item)
  }
  return map
}

function buildSeries(list, styles, order, toPoint, maxCount) {
  const grouped = group(list)
  return order
    .filter(key => grouped.has(key))
    .map(key => {
      const style = styles[key]
      return {
        name: style.name,
        type: style.ripple ? 'effectScatter' : 'scatter',
        rippleEffect: style.ripple ? { brushType: 'stroke', scale: 3.2, period: 3.5 } : undefined,
        symbolSize: val => val[2],
        itemStyle: {
          color: style.ripple ? style.color : style.color,
          opacity: style.ripple ? 0.92 : 0.5,
          borderColor: '#fff',
          borderWidth: 1
        },
        emphasis: { focus: 'series', scale: 1.15 },
        z: style.ripple ? 10 : 3,
        data: grouped.get(key).map(item => ({
          ...toPoint(item, maxCount),
          raw: item
        }))
      }
    })
}

function dishOption() {
  const list = props.data || []
  const prices = list.map(d => Number(d.price) || 0)
  const ratings = list.map(d => Number(d.avgRating) || 0)
  const counts = list.map(d => Number(d.reviewCount) || 0)
  const avgPrice = prices.length ? prices.reduce((a, b) => a + b, 0) / prices.length : 0
  const maxCount = Math.max(1, ...counts)
  const xMin = prices.length ? Math.min(...prices) : 0
  const xMax = prices.length ? Math.max(...prices) : 1
  const xRange = Math.max(1, xMax - xMin)
  const yMin = ratings.length ? Math.max(0, Math.floor(Math.min(...ratings) * 10) / 10 - 0.2) : 0
  const styles = STYLE.dish

  const series = buildSeries(
    list,
    styles,
    RIPPLE_ORDER.dish,
    item => ({
      value: [Number(item.price) || 0, Number(item.avgRating) || 0, symbolSize(item.reviewCount, maxCount)],
      name: item.dishName
    })
  )

  // 参考线：均价竖线 + 3.5 分横线，切出四个象限
  if (series.length) {
    series[0].markLine = {
      silent: true,
      symbol: 'none',
      lineStyle: { type: 'dashed', color: '#c0c4cc', width: 1 },
      label: { fontSize: 11, color: '#909399' },
      data: [
        { xAxis: Number(avgPrice.toFixed(1)), label: { formatter: `均价 ¥${avgPrice.toFixed(1)}`, position: 'end' } },
        { yAxis: 3.5, label: { formatter: '3.5 分', position: 'insideEndTop' } }
      ]
    }
    series[0].markArea = {
      silent: true,
      itemStyle: { opacity: 0.05 },
      data: [
        [
          { xAxis: 'min', yAxis: 3.5, itemStyle: { color: '#f5b301' } },
          { xAxis: avgPrice, yAxis: 'max' }
        ],
        [
          { xAxis: avgPrice, yAxis: 'min', itemStyle: { color: '#ef4444' } },
          { xAxis: 'max', yAxis: 3.5 }
        ]
      ]
    }
    // 象限文字用 markPoint 钉在数据坐标上：不占轴范围、位置可控，远离两线交点
    series[0].markPoint = {
      symbol: 'circle',
      symbolSize: 0.1,
      itemStyle: { opacity: 0 },
      label: { show: true, fontSize: 11, fontWeight: 600, position: 'top', distance: 2 },
      data: [
        { coord: [xMin + 0.04 * xRange, 4.86], label: { formatter: '便宜好吃', color: '#b58a00' } },
        {
          coord: [xMax - 0.02 * xRange, yMin + 0.16 * (5 - yMin)],
          label: { formatter: '贵且难吃', color: '#dc2626', position: 'bottom' }
        }
      ]
    }
  }

  return {
    tooltip: {
      trigger: 'item',
      borderWidth: 0,
      padding: [10, 14],
      formatter: p => {
        const d = p.data.raw || {}
        const color = p.color
        return `<div style="font-weight:600;margin-bottom:4px">${d.dishName || ''}</div>
          <div style="color:#909399;font-size:12px;margin-bottom:6px">${d.stallZone || ''} · ${d.windowNo || ''}号 ${d.stallName || ''}</div>
          <div>单价：<b style="color:#ff6a00">¥${Number(d.price || 0).toFixed(2)}</b></div>
          <div>评分：<b>${Number(d.avgRating || 0).toFixed(2)}</b> 分</div>
          <div>评价：<b>${d.reviewCount || 0}</b> 条 · 好评率 ${d.goodRate || 0}%</div>
          <div style="margin-top:6px"><span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${color};margin-right:6px"></span>${d.tagLabel || ''}</div>`
      }
    },
    legend: {
      bottom: 0,
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 18,
      textStyle: { fontSize: 12, color: '#606266' }
    },
    grid: { left: 56, right: 36, top: 30, bottom: 84 },
    xAxis: {
      type: 'value',
      name: '菜品单价（元）',
      nameLocation: 'middle',
      nameGap: 32,
      nameTextStyle: { fontSize: 12, color: '#909399' },
      scale: true,
      splitLine: { lineStyle: { color: '#f0f1f3' } }
    },
    yAxis: {
      type: 'value',
      name: '平均评分',
      nameTextStyle: { fontSize: 12, color: '#909399' },
      min: v => Math.max(0, Math.floor(v.min * 10) / 10 - 0.2),
      max: 5,
      splitLine: { lineStyle: { color: '#f0f1f3' } }
    },
    series
  }
}

function stallOption() {
  const list = props.data || []
  const ratingXs = list.map(s => Number(s.avgRating) || 0)
  const counts = list.map(s => Number(s.reviewCount) || 0)
  const maxCount = Math.max(1, ...counts)
  const xMin = ratingXs.length ? Math.min(...ratingXs) : 0
  const xMax = ratingXs.length ? Math.max(...ratingXs) : 1
  const xRange = Math.max(0.5, xMax - xMin)
  const styles = STYLE.stall

  const series = buildSeries(
    list,
    styles,
    RIPPLE_ORDER.stall,
    item => ({
      value: [Number(item.avgRating) || 0, Number(item.hygieneScore) || 0, symbolSize(item.reviewCount, maxCount)],
      name: item.stallName
    })
  )

  // 参考线：x=3.5 分口碑线、y=80 分卫生合格线，右上为优质区、左下为劣质区
  if (series.length) {
    series[0].markLine = {
      silent: true,
      symbol: 'none',
      lineStyle: { type: 'dashed', color: '#c0c4cc', width: 1 },
      label: { fontSize: 11, color: '#909399' },
      data: [
        { xAxis: 3.5, label: { formatter: '口碑线 3.5', position: 'end' } },
        { yAxis: 80, label: { formatter: '卫生合格线 80', position: 'insideEndTop' } }
      ]
    }
    series[0].markArea = {
      silent: true,
      itemStyle: { opacity: 0.06 },
      data: [
        [
          { xAxis: 3.5, yAxis: 80, itemStyle: { color: '#22c55e' } },
          { xAxis: 'max', yAxis: 'max' }
        ],
        [
          { xAxis: 'min', yAxis: 'min', itemStyle: { color: '#ef4444' } },
          { xAxis: 3.5, yAxis: 80 }
        ]
      ]
    }
    // 区域文字钉在数据坐标上，远离两线交点
    series[0].markPoint = {
      symbol: 'circle',
      symbolSize: 0.1,
      itemStyle: { opacity: 0 },
      label: { show: true, fontSize: 11, fontWeight: 600, position: 'bottom', distance: 2 },
      data: [
        { coord: [xMax - 0.03 * xRange, 95], label: { formatter: '优质区', color: '#15803d' } },
        { coord: [xMin + 0.03 * xRange, 64], label: { formatter: '劣质区', color: '#b91c1c' } }
      ]
    }
  }

  return {
    tooltip: {
      trigger: 'item',
      borderWidth: 0,
      padding: [10, 14],
      formatter: p => {
        const d = p.data.raw || {}
        const color = p.color
        return `<div style="font-weight:600;margin-bottom:4px">${d.stallName || ''}</div>
          <div style="color:#909399;font-size:12px;margin-bottom:6px">${d.stallZone || ''} · ${d.windowNo || ''}号窗口 · ${d.location || ''} · 负责人 ${d.managerName || '待分配'}</div>
          <div>学生评分：<b style="color:#ff6a00">${Number(d.avgRating || 0).toFixed(2)}</b> 分</div>
          <div>卫生评分：<b>${Number(d.hygieneScore || 0).toFixed(1)}</b> 分</div>
          <div>评价数：<b>${d.reviewCount || 0}</b> 条 · 好评率 ${d.goodRate || 0}%</div>
          <div style="margin-top:6px"><span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${color};margin-right:6px"></span>${d.tagLabel || ''}</div>`
      }
    },
    legend: {
      bottom: 0,
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 18,
      textStyle: { fontSize: 12, color: '#606266' }
    },
    grid: { left: 60, right: 36, top: 30, bottom: 84 },
    xAxis: {
      type: 'value',
      name: '学生评分',
      nameLocation: 'middle',
      nameGap: 32,
      nameTextStyle: { fontSize: 12, color: '#909399' },
      min: v => Math.max(0, Math.floor(v.min * 10) / 10 - 0.2),
      max: 5,
      splitLine: { lineStyle: { color: '#f0f1f3' } }
    },
    yAxis: {
      type: 'value',
      name: '学校评定卫生分',
      nameTextStyle: { fontSize: 12, color: '#909399' },
      min: 60,
      max: 100,
      splitLine: { lineStyle: { color: '#f0f1f3' } }
    },
    series
  }
}
</script>
