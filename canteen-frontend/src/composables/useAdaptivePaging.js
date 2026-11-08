import { onBeforeUnmount } from 'vue'

// 与全局样式 .adaptive-table 里的行高/表头高保持一致
const HEADER_HEIGHT = 47
const ROW_HEIGHT = 51
const MIN_SIZE = 10
const MAX_SIZE = 60

/**
 * 高度自适应分页：根据表格区可视高度自动增减每页条数，让数据铺满屏幕、页数随之变少。
 * 用法：
 *   const { bind } = useAdaptivePaging(query, loadData)
 *   onMounted(async () => { await loadData(); await nextTick(); bind(tableWrapEl.value) })
 */
export function useAdaptivePaging(query, reload) {
  let ro = null
  let el = null

  function fit() {
    if (!el || el.clientHeight <= 0) return
    const rows = Math.max(MIN_SIZE, Math.floor((el.clientHeight - HEADER_HEIGHT) / ROW_HEIGHT))
    const next = Math.min(rows, MAX_SIZE)
    if (next !== query.size) {
      query.size = next
      query.current = 1
      reload()
    }
  }

  function bind(target) {
    if (!target) return
    el = target
    ro = new ResizeObserver(fit)
    ro.observe(el)
  }

  onBeforeUnmount(() => {
    if (ro) ro.disconnect()
  })

  return { bind }
}