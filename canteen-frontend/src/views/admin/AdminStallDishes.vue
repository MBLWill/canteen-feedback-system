<template>
  <div class="page-container stall-dishes-page" v-loading="loading">
    <div class="page-head">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/admin/stalls' }">商家管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ stall?.name || '档口菜单' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button @click="backToStalls">← 返回商家管理</el-button>
    </div>

    <el-card shadow="never" class="hero-card">
      <div class="hero-main">
        <span class="hero-icon"><img src="/ui/emoji/stall.png" alt=""></span>
        <div class="hero-text">
          <h1 class="hero-title">{{ stall?.name || '档口菜单' }}</h1>
          <div class="hero-sub">
            <el-tag size="small" effect="plain" type="warning">{{ stall?.windowNo }} 号窗口</el-tag>
            <span>{{ areaText }}</span>
            <span>{{ stall?.category || '暂无主营分类' }}</span>
            <span>评分 {{ stall?.avgRating || 0 }}</span>
            <span>销量 {{ stall?.salesCount || 0 }}</span>
          </div>
        </div>
      </div>
      <div class="hero-actions">
        <template v-if="manageMode">
          <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
          <span class="muted selected-hint">已选 {{ selectedIds.length }} 道</span>
          <el-button type="danger" plain size="small" :disabled="!selectedIds.length" :loading="batchDeleting" @click="deleteSelected">批量删除</el-button>
          <el-button size="small" @click="manageMode = false">退出管理</el-button>
        </template>
        <el-button v-else @click="manageMode = true">菜品管理</el-button>
        <el-button type="primary" @click="openCreate">＋ 新增菜品</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="cert-card">
      <template #header>
        <div class="cert-header">
          <span style="font-weight: 600">商家认证信息</span>
          <span class="muted" style="font-size: 12px">由商家在个人中心提交，管理员查看</span>
        </div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <div class="cert-title">健康证</div>
          <div class="cert-box">
            <el-image v-if="stall?.healthCertUrl" :src="stall.healthCertUrl" :preview-src-list="[stall.healthCertUrl]" fit="cover" class="cert-img" />
            <div v-else class="cert-empty">未上传</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="cert-title">营业执照</div>
          <div class="cert-box">
            <el-image v-if="stall?.licenseUrl" :src="stall.licenseUrl" :preview-src-list="[stall.licenseUrl]" fit="cover" class="cert-img" />
            <div v-else class="cert-empty">未上传</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="cert-title">商家账号信息</div>
          <div class="cert-info">
            <div class="info-row"><span class="info-label">商家账号</span><span>{{ stall?.merchantUsername || '未建' }}</span></div>
            <div class="info-row"><span class="info-label">负责人</span><span>{{ stall?.managerName || '-' }}</span></div>
            <div class="info-row"><span class="info-label">联系电话</span><span>{{ stall?.managerPhone || '-' }}</span></div>
            <div class="info-row"><span class="info-label">绑定邮箱</span><span>{{ stall?.merchantEmail || '未绑定' }}</span></div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索菜品名称 / 分类"
          clearable
          style="width: 240px"
          @keyup.enter="loadDishes"
          @clear="loadDishes"
        />
        <el-select v-model="sortBy" style="width: 150px" @change="loadDishes">
          <el-option label="好评优先" value="rating" />
          <el-option label="销量优先" value="sales" />
          <el-option label="价格从低到高" value="priceAsc" />
        </el-select>
        <el-button type="primary" @click="loadDishes">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
        <span class="total-hint">共 {{ total }} 道菜</span>
      </div>
    </el-card>

    <div class="dish-grid">
      <div v-for="dish in dishes" :key="dish.id" class="dish-card" :class="{ managing: manageMode }">
        <div class="dish-thumb">
          <img :src="dishImage(dish)" :alt="dish.name" loading="lazy">
          <span class="rating-badge">★ {{ dish.avgRating || 0 }}</span>
          <el-tag v-if="dish.featured" class="featured-badge" type="warning" size="small" effect="light">招牌</el-tag>
          <el-checkbox v-if="manageMode" v-model="selectedIds" :value="dish.id" class="select-box" @click.stop />
        </div>
        <div class="dish-body">
          <div class="dish-title">{{ dish.name }}</div>
          <div class="dish-meta">{{ stall?.windowNo }} 号窗口 · {{ dish.category || '未分类' }}</div>
          <div class="dish-foot">
            <span class="dish-price">¥{{ dish.price }}</span>
            <span class="dish-sales">{{ dish.salesCount || 0 }} 份售出 · {{ dish.reviewCount || 0 }} 条评价</span>
          </div>
          <div v-if="manageMode" class="dish-actions">
            <el-button link type="primary" @click="openEdit(dish)">编辑</el-button>
            <el-button link type="danger" @click="deleteDishItem(dish)">删除</el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !dishes.length" description="该档口暂无菜品" />

    <el-dialog v-model="formVisible" :title="form.id ? '编辑菜品' : '新增菜品'" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="form.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" maxlength="30" show-word-limit placeholder="如：川菜" />
        </el-form-item>
        <el-form-item label="图片地址" prop="image">
          <el-input v-model="form.image" maxlength="255" placeholder="可选，https://..." />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="标记">
          <el-space>
            <el-checkbox v-model="form.featuredBool">招牌菜</el-checkbox>
            <el-radio-group v-model="form.status">
              <el-radio :value="1">在售</el-radio>
              <el-radio :value="0">下架</el-radio>
            </el-radio-group>
          </el-space>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDish">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStall } from '@/api/stall'
import { pageDishes, createDish, updateDish, deleteDish } from '@/api/dish'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const stall = ref(null)
const dishes = ref([])
const total = ref(0)
const keyword = ref('')
const sortBy = ref('rating')
const manageMode = ref(false)
const selectedIds = ref([])
const batchDeleting = ref(false)

const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  stallId: null,
  name: '',
  price: 0,
  category: '',
  image: '',
  description: '',
  featuredBool: false,
  status: 1
})
const rules = {
  name: [{ required: true, message: '请填写菜品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请填写价格', trigger: 'blur' }],
  category: [{ required: true, message: '请填写分类', trigger: 'blur' }]
}

const areaText = computed(() => {
  if (!stall.value) return ''
  const area = stall.value.area === 'north' ? '北区食堂' : '南区食堂'
  return `${area} ${stall.value.floor || 1} 楼`
})
const allSelected = computed({
  get: () => dishes.value.length > 0 && selectedIds.value.length === dishes.value.length,
  set: val => {
    selectedIds.value = val ? dishes.value.map(d => d.id) : []
  }
})
const isIndeterminate = computed(() =>
  selectedIds.value.length > 0 && selectedIds.value.length < dishes.value.length
)

async function loadStall() {
  const res = await getStall(route.params.id)
  stall.value = res.data
}

async function loadDishes() {
  loading.value = true
  try {
    const res = await pageDishes({
      stallId: route.params.id,
      keyword: keyword.value.trim() || undefined,
      current: 1,
      size: 100,
      sortBy: sortBy.value
    })
    dishes.value = res.data?.records || []
    total.value = res.data?.total || dishes.value.length
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  keyword.value = ''
  sortBy.value = 'rating'
  loadDishes()
}

function dishImage(dish) {
  return dish.image || '/ui/emoji/dish.png'
}

function backToStalls() {
  router.push('/admin/stalls')
}

function openCreate() {
  Object.assign(form, {
    id: null,
    stallId: stall.value?.id,
    name: '',
    price: 0,
    category: stall.value?.category || '',
    image: '',
    description: '',
    featuredBool: false,
    status: 1
  })
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(dish) {
  Object.assign(form, {
    id: dish.id,
    stallId: dish.stallId || stall.value?.id,
    name: dish.name,
    price: Number(dish.price),
    category: dish.category || '',
    image: dish.image || '',
    description: dish.description || '',
    featuredBool: dish.featured === 1 || dish.featured === true,
    status: dish.status === 0 ? 0 : 1
  })
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function submitDish() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = {
      stallId: form.stallId || stall.value.id,
      name: form.name.trim(),
      price: form.price,
      category: form.category.trim(),
      image: form.image.trim(),
      description: form.description.trim(),
      featured: form.featuredBool ? 1 : 0,
      status: form.status
    }
    if (form.id) {
      await updateDish(form.id, payload)
    } else {
      await createDish(payload)
    }
    ElMessage.success('菜品保存成功')
    formVisible.value = false
    await Promise.all([loadDishes(), loadStall()])
  } finally {
    submitting.value = false
  }
}

function deleteDishItem(dish) {
  ElMessageBox.confirm(`确定删除菜品「${dish.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteDish(dish.id)
      ElMessage.success('删除成功')
      await Promise.all([loadDishes(), loadStall()])
    })
    .catch(() => {})
}

async function deleteSelected() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 道菜吗？删除后不可恢复`, '提示', { type: 'warning' })
  } catch {
    return
  }
  batchDeleting.value = true
  try {
    const results = await Promise.allSettled(selectedIds.value.map(id => deleteDish(id)))
    const success = results.filter(r => r.status === 'fulfilled').length
    const failed = results.length - success
    if (failed) ElMessage.warning(`成功删除 ${success} 项，${failed} 项删除失败`)
    else ElMessage.success(`成功删除 ${success} 项`)
    await Promise.all([loadDishes(), loadStall()])
  } finally {
    batchDeleting.value = false
  }
}

onMounted(async () => {
  await loadStall()
  await loadDishes()
})
</script>

<style scoped>
.stall-dishes-page {
  width: 100%;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.hero-card {
  margin-bottom: 16px;
  border: 1px solid #ffe0c7;
  background: linear-gradient(120deg, #fff8f2 0%, #ffffff 58%, #fff3e8 100%);
}

.hero-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-icon {
  width: 62px;
  height: 62px;
  border-radius: 18px;
  background: #fff1e4;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(255, 122, 26, 0.16);
}

.hero-icon img {
  width: 46px;
  height: 46px;
  object-fit: contain;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: var(--text-1);
  letter-spacing: 0.5px;
}

.hero-sub {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-3);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.selected-hint {
  font-size: 12px;
}

.filter-card {
  margin-bottom: 16px;
}

.cert-card {
  margin-bottom: 16px;
}

.cert-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cert-title {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}

.cert-box {
  width: 100%;
  height: 140px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border);
  background: #fafafa;
}

.cert-img {
  width: 100%;
  height: 100%;
}

.cert-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3);
  font-size: 13px;
}

.cert-info {
  padding: 8px 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 13px;
  color: var(--text-1);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--text-3);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.total-hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-3);
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(215px, 1fr));
  gap: 16px;
}

.dish-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-xs);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.dish-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px rgba(24, 32, 48, 0.1);
}

.dish-card.managing {
  border-color: #ffd7b8;
}

.dish-thumb {
  position: relative;
  height: 138px;
  background: #f6f7f9;
}

.dish-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rating-badge {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 2px 8px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  color: #e35f00;
  font-size: 12px;
  font-weight: 700;
}

.featured-badge {
  position: absolute;
  left: 8px;
  top: 8px;
}

.select-box {
  position: absolute;
  left: 8px;
  top: 8px;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dish-body {
  padding: 12px 14px 14px;
}

.dish-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-meta {
  margin-top: 5px;
  font-size: 12px;
  color: var(--text-3);
}

.dish-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.dish-price {
  font-size: 17px;
  font-weight: 800;
  color: #ff6a00;
}

.dish-sales {
  font-size: 11px;
  color: var(--text-3);
}

.dish-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--border);
}
</style>
