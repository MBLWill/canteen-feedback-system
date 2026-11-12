<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="login-left">
        <div class="brand-badge">
          <img src="/ui/emoji/title.png" alt="logo" />
        </div>
        <div class="login-title">小众点评系统</div>
        <div class="login-sub">Niche Review System</div>

        <div class="feature-list">
          <div v-for="f in features" :key="f.text" class="feature-item">
            <span class="feature-emoji">{{ f.emoji }}</span>
            <span>{{ f.text }}</span>
          </div>
        </div>
      </div>

      <div class="login-right">
        <div class="form-head">
          <div class="form-title">{{ activeTab === 'login' ? '欢迎回来 👋' : '创建学生账号' }}</div>
          <div class="form-desc">
            {{ activeTab === 'login' ? '登录后可以打分评价、收藏菜品、查看热销榜单' : '注册后即可参与菜品评价与留言反馈' }}
          </div>
        </div>

        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="账号登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" size="large">
              <el-form-item label="账号" prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入账号" :prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="学生注册" name="register">
            <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-position="top" size="large">
              <el-form-item label="账号" prop="username">
                <el-input v-model="regForm.username" placeholder="4-20位字母/数字/下划线" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="regForm.password" type="password" show-password placeholder="6-20位" />
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="regForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item label="学号（选填）">
                <el-input v-model="regForm.studentNo" placeholder="请输入学号" />
              </el-form-item>
              <el-form-item label="手机号（选填）">
                <el-input v-model="regForm.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-button type="success" size="large" class="submit-btn" :loading="loading" @click="handleRegister">
                注 册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="account-tip">
          <div class="account-tip-title">演示账号 · 点击自动填入</div>
          <div class="account-list">
            <button
              v-for="a in demoAccounts"
              :key="a.username"
              class="account-pill"
              type="button"
              @click="fillDemoAccount(a)"
            >
              <span class="pill-role" :class="a.tone">{{ a.role }}</span>
              <span class="pill-account"><b>{{ a.username }}</b> / <b>{{ a.password }}</b></span>
              <span class="pill-fill">填入</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { register } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const features = [
  { emoji: '🍜', text: '菜品浏览 · 按档口 / 分类 / 评分筛选' },
  { emoji: '⭐', text: '打分评价 · 一人一菜一评，支持匿名' },
  { emoji: '💬', text: '留言反馈 · 管理员在线受理回复' },
  { emoji: '📊', text: '热销榜单 · 菜品评分排行 / 好评分布' }
]

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const regFormRef = ref()

const demoAccounts = [
  { role: '管理员', tone: 'admin', username: 'admin', password: 'admin123' },
  { role: '学生', tone: 'student', username: 'student01', password: '123456' },
  { role: '商家', tone: 'merchant', username: '100012', password: '123456' }
]

const loginForm = reactive({ username: 'admin', password: 'admin123' })
const regForm = reactive({ username: '', password: '', realName: '', studentNo: '', phone: '' })

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4-20位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function fillDemoAccount(account) {
  activeTab.value = 'login'
  loginForm.username = account.username
  loginForm.password = account.password
  loginFormRef.value?.clearValidate()
  ElMessage.success(`已填入${account.role}账号，点击登录即可`)
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success(`欢迎回来，${userStore.nickname}`)
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else if (userStore.isMerchant) {
      router.push('/merchant/todos')
    } else if (userStore.isAdmin) {
      // 管理员默认落地数据大屏
      router.push('/admin/dashboard')
    } else {
      // 学生默认落地美食探索
      router.push('/dishes')
    }
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await regFormRef.value.validate()
  loading.value = true
  try {
    await register(regForm)
    ElMessage.success('注册成功，请登录')
    loginForm.username = regForm.username
    loginForm.password = ''
    activeTab.value = 'login'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-left {
  position: relative;
}

.brand-badge {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  backdrop-filter: blur(4px);
  overflow: hidden;
}

.brand-badge img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.feature-list {
  margin-top: 28px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
}

.feature-emoji {
  font-size: 16px;
}

.form-head {
  margin-bottom: 18px;
}

.form-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-1);
}

.form-desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-3);
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-3);
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: var(--brand-dark);
}

.login-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, #ff8f3c, #ff6a00);
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--border);
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 4px;
}

.account-tip {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: var(--text-2);
}

.account-tip-title {
  color: var(--text-3);
  font-weight: 600;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.account-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 12px;
  border-radius: 10px;
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  cursor: pointer;
  text-align: left;
  font-size: 12px;
  color: var(--text-2);
  transition: all 0.16s ease;
}

.account-pill:hover {
  border-color: var(--brand);
  background: #fff1e4;
  transform: translateX(2px);
}

.pill-account {
  flex: 1;
}

.pill-fill {
  font-size: 11px;
  color: var(--brand-dark);
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.account-pill:hover .pill-fill {
  opacity: 1;
}

.pill-role {
  flex-shrink: 0;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  color: #fff;
  font-weight: 600;
}

.pill-role.admin {
  background: #ef4444;
}

.pill-role.student {
  background: #22c55e;
}

.pill-role.merchant {
  background: #2563eb;
}
</style>