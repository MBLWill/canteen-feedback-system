<template>
  <div class="page-container profile-page">
    <!-- 顶部：个人信息卡（左头像+昵称+标签，右统计区） -->
    <div class="profile-hero">
      <div class="hero-left">
        <el-upload :show-file-list="false" :http-request="doUploadAvatar" accept="image/*">
          <div class="hero-avatar">
            <img v-if="form.avatar" :src="form.avatar" class="avatar-img" />
            <div v-else class="avatar-fallback">{{ avatarText }}</div>
            <div class="avatar-mask">换头像</div>
          </div>
        </el-upload>
        <div class="hero-info">
          <div class="hero-name">{{ userStore.nickname }}</div>
          <div class="hero-tags">
            <span class="role-tag" :class="'role-' + roleKey">{{ form.roleLabel }}</span>
            <span v-if="form.studentNo" class="no-tag">学号 {{ form.studentNo }}</span>
          </div>
        </div>
      </div>
      <div class="hero-right">
        <div class="hero-stat">
          <div class="hero-stat-num">{{ stats.orderCount ?? 0 }}</div>
          <div class="hero-stat-label">累计订单</div>
        </div>
        <div class="hero-divider"></div>
        <div class="hero-stat">
          <div class="hero-stat-num">¥{{ fmt(stats.totalSpent) }}</div>
          <div class="hero-stat-label">累计消费</div>
        </div>
        <div class="hero-divider"></div>
        <div class="hero-stat">
          <div class="hero-stat-num">{{ stats.completedCount ?? 0 }}</div>
          <div class="hero-stat-label">已完成</div>
        </div>
      </div>
    </div>

    <!-- 四个账户统计卡 -->
    <div class="stat-grid">
      <div class="stat-card" @click="activeMenu = 'base'">
        <div class="stat-card-value accent">¥{{ fmt(stats.balance) }}</div>
        <div class="stat-card-label">账户余额</div>
      </div>
      <div class="stat-card" @click="goOrders">
        <div class="stat-card-value">{{ stats.orderCount ?? 0 }}</div>
        <div class="stat-card-label">总订单</div>
      </div>
      <div class="stat-card">
        <div class="stat-card-value">¥{{ fmt(stats.totalSpent) }}</div>
        <div class="stat-card-label">总消费</div>
      </div>
      <div class="stat-card" @click="goOrders">
        <div class="stat-card-value">{{ stats.completedCount ?? 0 }}</div>
        <div class="stat-card-label">已完成</div>
      </div>
    </div>

    <!-- 功能菜单（网格） -->
    <div class="menu-grid">
      <div
        v-for="m in sideMenus"
        :key="m.key"
        class="menu-item"
        :class="{ active: activeMenu === m.key }"
        @click="onMenuClick(m)"
      >
        <span class="menu-icon" :style="{ background: m.color + '1a', color: m.color }">{{ m.icon }}</span>
        <span class="menu-label">{{ m.label }}</span>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content-area">
      <!-- 基本资料 -->
      <el-card v-show="activeMenu === 'base'" shadow="never" class="page-card">
        <template #header><span style="font-weight: 600">基本资料</span></template>
        <el-form label-position="top" style="max-width: 460px">
          <el-form-item label="姓名">
            <el-input v-model="form.realName" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="绑定邮箱">
            <el-input v-model="form.email" maxlength="100" placeholder="用于接收食堂通知与安全验证" />
          </el-form-item>
          <el-form-item label="绑定电话">
            <el-input v-model="form.phone" maxlength="20" placeholder="手机号或座机" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 余额管理（展示型） -->
      <el-card v-show="activeMenu === 'balance'" shadow="never" class="page-card">
        <template #header><span style="font-weight: 600">余额管理</span></template>
        <div class="balance-panel">
          <div class="balance-big">
            <div class="balance-big-label">当前余额</div>
            <div class="balance-big-num">¥{{ fmt(stats.balance) }}</div>
          </div>
          <div class="balance-desc">
            <p>账户余额用于食堂就餐消费记账展示，实际支付时仍通过收银台完成。</p>
            <p class="balance-tip">已消费 <b>¥{{ fmt(stats.totalSpent) }}</b> · 已完成订单 <b>{{ stats.completedCount ?? 0 }}</b> 单</p>
          </div>
        </div>
      </el-card>

      <!-- 账号安全 -->
      <el-card v-show="activeMenu === 'security'" shadow="never" class="page-card">
        <template #header><span style="font-weight: 600">账号安全</span></template>
        <div class="sec-item">
          <div class="sec-info">
            <div class="sec-name">登录密码</div>
            <div class="sec-desc">定期更换密码可以让账号更安全</div>
          </div>
          <el-button type="primary" plain @click="openPwdDialog">修改密码</el-button>
        </div>
        <div class="sec-item">
          <div class="sec-info">
            <div class="sec-name">绑定邮箱</div>
            <div class="sec-desc">{{ form.email ? '已绑定：' + form.email : '未绑定，绑定后可用于安全验证' }}</div>
          </div>
          <div class="sec-actions">
            <el-tag :type="form.email ? 'success' : 'info'" size="small" effect="plain">
              {{ form.email ? '已绑定' : '未绑定' }}
            </el-tag>
            <el-button type="primary" plain size="small" @click="activeMenu = 'base'">
              {{ form.email ? '修改邮箱' : '去绑定' }}
            </el-button>
          </div>
        </div>
        <div class="sec-item">
          <div class="sec-info">
            <div class="sec-name">绑定电话</div>
            <div class="sec-desc">{{ form.phone ? '已绑定：' + form.phone : '未绑定，绑定后可用于安全验证' }}</div>
          </div>
          <el-tag :type="form.phone ? 'success' : 'info'" size="small" effect="plain">
            {{ form.phone ? '已绑定' : '未绑定' }}
          </el-tag>
        </div>
      </el-card>

      <!-- 我的证照（商家） -->
      <el-card v-show="activeMenu === 'cert'" shadow="never" class="page-card">
        <template #header><span style="font-weight: 600">我的证照</span></template>
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="cert-title">健康证</div>
            <el-upload :show-file-list="false" :http-request="opt => doUploadCert(opt, 'health')" accept="image/*">
              <div class="cert-box">
                <img v-if="cert.healthCertUrl" :src="cert.healthCertUrl" class="cert-img" />
                <div v-else class="cert-empty"><span style="font-size: 26px">＋</span><span>上传健康证</span></div>
                <div class="cert-mask">点击更换</div>
              </div>
            </el-upload>
          </el-col>
          <el-col :span="12">
            <div class="cert-title">营业执照</div>
            <el-upload :show-file-list="false" :http-request="opt => doUploadCert(opt, 'license')" accept="image/*">
              <div class="cert-box">
                <img v-if="cert.licenseUrl" :src="cert.licenseUrl" class="cert-img" />
                <div v-else class="cert-empty"><span style="font-size: 26px">＋</span><span>上传营业执照</span></div>
                <div class="cert-mask">点击更换</div>
              </div>
            </el-upload>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="440px">
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码（6-20位）">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwd.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item :label="`验证码（发送至 ${verifyTarget || '绑定的邮箱/电话'}）`">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-input v-model="pwd.verifyCode" maxlength="6" placeholder="6 位验证码" />
            <el-button :disabled="countdown > 0" :loading="sending" style="width: 120px" @click="sendCode">
              {{ countdown > 0 ? countdown + 's 后重发' : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile, changePassword as changePwdApi, getAccountSummary } from '@/api/auth'
import { uploadImage } from '@/api/sales'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const isMerchant = computed(() => userStore.isMerchant)
const roleKey = computed(() => (isMerchant.value ? 'merchant' : userStore.isAdmin ? 'admin' : 'student'))
const avatarText = computed(() => (userStore.nickname || 'U').trim().slice(0, 1).toUpperCase())

const activeMenu = ref('base')
const stats = reactive({ balance: 0, orderCount: 0, totalSpent: 0, completedCount: 0 })

const menuBase = [
  { key: 'base', label: '编辑资料', icon: '✎', color: '#f97316' },
  { key: 'balance', label: '余额管理', icon: '◈', color: '#8b5cf6' },
  { key: 'orders', label: '我的订单', icon: '▤', color: '#3b82f6' },
  { key: 'security', label: '修改密码', icon: '🔒', color: '#e0353b' },
  { key: 'logout', label: '退出登录', icon: '⏻', color: '#64748b' }
]

const sideMenus = computed(() => {
  const menus = [...menuBase]
  if (isMerchant.value) {
    menus.splice(menus.length - 1, 0, { key: 'cert', label: '我的证照', icon: '◫', color: '#0d9488' })
  }
  return menus
})

const saving = ref(false)
const changingPwd = ref(false)
const form = reactive({
  avatar: '',
  realName: '',
  email: '',
  phone: '',
  roleLabel: '',
  studentNo: ''
})
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '', verifyCode: '' })
const cert = reactive({ healthCertUrl: '', licenseUrl: '' })
const pwdVisible = ref(false)
const countdown = ref(0)
const sending = ref(false)
const verifyTarget = computed(() => form.email || form.phone || '')

function fmt(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function fillForm(profile) {
  Object.assign(form, {
    avatar: profile.avatar || '',
    realName: profile.realName || '',
    email: profile.email || '',
    phone: profile.phone || '',
    roleLabel: profile.roleLabel || '',
    studentNo: profile.studentNo || ''
  })
}

async function loadProfile() {
  const res = await request.get('/auth/profile')
  fillForm(res.data)
  userStore.userInfo = res.data
  localStorage.setItem('canteen_user', JSON.stringify(res.data))
}

async function loadSummary() {
  try {
    const res = await getAccountSummary()
    Object.assign(stats, res.data || {})
  } catch {
    /* 忽略统计加载失败 */
  }
}

async function loadCert() {
  if (!isMerchant.value) return
  const res = await request.get('/merchant/certificates')
  cert.healthCertUrl = res.data.healthCertUrl || ''
  cert.licenseUrl = res.data.licenseUrl || ''
}

function onMenuClick(m) {
  if (m.key === 'orders') {
    goOrders()
    return
  }
  if (m.key === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '退出确认', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
    return
  }
  activeMenu.value = m.key
}

function goOrders() {
  router.push('/my/orders')
}

async function doUploadAvatar(option) {
  const res = await uploadImage(option.file)
  form.avatar = res.data
  await updateProfile({ avatar: form.avatar })
  ElMessage.success('头像已更新')
  loadProfile()
}

async function saveProfile() {
  if (!form.realName.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  saving.value = true
  try {
    const body = { realName: form.realName.trim(), email: form.email, phone: form.phone }
    const res = await updateProfile(body)
    fillForm(res.data)
    userStore.userInfo = res.data
    localStorage.setItem('canteen_user', JSON.stringify(res.data))
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

function openPwdDialog() {
  pwd.oldPassword = pwd.newPassword = pwd.confirm = pwd.verifyCode = ''
  pwdVisible.value = true
}

async function sendCode() {
  if (!verifyTarget.value) {
    ElMessage.warning('请先在「基本资料」里绑定邮箱或电话')
    return
  }
  sending.value = true
  try {
    const res = await request.post('/auth/verify-code', { target: verifyTarget.value })
    ElMessage.success('验证码已发送（演示模式）：' + res.data)
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function savePassword() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (pwd.newPassword.length < 6 || pwd.newPassword.length > 20) {
    ElMessage.warning('新密码长度为 6-20 位')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (!pwd.verifyCode.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  changingPwd.value = true
  try {
    await changePwdApi({
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword,
      verifyCode: pwd.verifyCode.trim()
    })
    ElMessage.success('密码已修改')
    pwdVisible.value = false
  } finally {
    changingPwd.value = false
  }
}

async function doUploadCert(option, type) {
  const res = await uploadImage(option.file)
  if (type === 'health') cert.healthCertUrl = res.data
  else cert.licenseUrl = res.data
  await request.put('/merchant/certificates', {
    healthCertUrl: cert.healthCertUrl,
    licenseUrl: cert.licenseUrl
  })
  ElMessage.success('证照已更新')
}

onMounted(() => {
  loadProfile()
  loadSummary()
  loadCert()
})
</script>

<style scoped>
.profile-page {
  max-width: 960px;
  margin: 0 auto;
}

/* 顶部信息卡 */
.profile-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  box-shadow: var(--shadow-xs);
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-avatar {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid var(--brand-line);
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
}

.avatar-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  font-size: 11px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  text-align: center;
  padding: 3px 0;
}

.hero-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-1);
}

.hero-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.role-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 600;
}
.role-student { background: #e7f7ec; color: #17803d; }
.role-merchant { background: #fdf1dc; color: #a55a00; }
.role-admin { background: #fdeaea; color: #b42318; }

.no-tag {
  font-size: 12px;
  color: var(--text-3);
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.hero-stat {
  text-align: center;
  min-width: 80px;
}

.hero-stat-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-1);
}

.hero-stat-label {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
}

.hero-divider {
  width: 1px;
  height: 36px;
  background: var(--border);
}

/* 统计卡 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 18px 20px;
  text-align: center;
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.stat-card-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-1);
}
.stat-card-value.accent { color: var(--brand-dark); }

.stat-card-label {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}

/* 功能菜单网格 */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 10px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all .18s ease;
}

.menu-item:hover {
  border-color: var(--brand-line);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}

.menu-item.active {
  border-color: var(--brand);
  background: var(--brand-soft);
}

.menu-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.menu-label {
  font-size: 13px;
  color: var(--text-2);
}

.content-area {
  margin-top: 16px;
}

/* 余额面板 */
.balance-panel {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 8px 4px;
}

.balance-big {
  flex-shrink: 0;
  min-width: 220px;
  padding: 24px 28px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff1e6 0%, #ffe3cc 100%);
  text-align: center;
}

.balance-big-label {
  font-size: 13px;
  color: var(--text-3);
}

.balance-big-num {
  font-size: 32px;
  font-weight: 800;
  color: var(--brand-dark);
  margin-top: 6px;
}

.balance-desc {
  font-size: 14px;
  color: var(--text-2);
  line-height: 1.8;
}

.balance-desc p { margin: 0 0 8px; }
.balance-tip { color: var(--text-3); font-size: 13px; }
.balance-tip b { color: var(--text-1); }

/* 安全项 */
.sec-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 4px;
  border-bottom: 1px dashed var(--border);
}
.sec-item:last-child { border-bottom: none; }
.sec-name { font-size: 14px; font-weight: 600; color: var(--text-1); }
.sec-desc { font-size: 12px; color: var(--text-3); margin-top: 4px; }
.sec-actions { display: flex; align-items: center; gap: 8px; }

.cert-title { font-size: 13px; color: var(--text-2); margin-bottom: 8px; }
.cert-box { position: relative; width: 100%; height: 140px; border-radius: 10px; overflow: hidden; cursor: pointer; border: 1px solid var(--border); }
.cert-img { width: 100%; height: 100%; object-fit: cover; }
.cert-empty { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; color: var(--text-3); border: 1px dashed var(--border); }
.cert-mask { position: absolute; bottom: 0; left: 0; right: 0; font-size: 12px; color: #fff; background: rgba(0, 0, 0, 0.5); text-align: center; padding: 4px 0; }

/* 响应式 */
@media (max-width: 900px) {
  .hero-right { display: none; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .menu-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
