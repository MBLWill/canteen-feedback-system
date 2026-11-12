<template>
  <el-container class="app-shell">
    <el-aside width="232px" class="app-aside">
      <div class="brand">
        <div class="brand-logo">
          <img src="/ui/emoji/title.png" alt="logo" />
        </div>
        <div class="brand-text">
          <div class="brand-name">小众点评系统</div>
          <div class="brand-sub">Niche Review</div>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <!-- 管理员：后台管理 / 日常使用 两栏可切换（互斥） -->
        <div v-if="userStore.isAdmin" class="menu-group">
          <div class="menu-group-title menu-group-title--switch">
            <span>{{ adminMode === 'admin' ? '后台管理' : '日常使用' }}</span>
            <button
              class="mode-switch"
              type="button"
              :title="adminMode === 'admin' ? '切换到日常使用' : '切换到后台管理'"
              @click="toggleAdminMode"
            >
              <el-icon :size="13"><component :is="adminMode === 'admin' ? 'Refresh' : 'RefreshLeft'" /></el-icon>
              <span>{{ adminMode === 'admin' ? '日常使用' : '后台管理' }}</span>
            </button>
          </div>
          <el-menu :default-active="activePath" router class="app-menu">
            <template v-if="adminMode === 'admin'">
              <el-menu-item
                v-for="item in adminMenus"
                :key="item.path"
                :index="'/' + item.path"
              >
                <el-icon><component :is="item.meta.icon" /></el-icon>
                <span>{{ item.meta.title }}</span>
              </el-menu-item>
            </template>
            <template v-else>
              <el-menu-item
                v-for="item in studentMenus"
                :key="item.path"
                :index="'/' + item.path"
              >
                <el-icon><component :is="item.meta.icon" /></el-icon>
                <span>{{ item.meta.title }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </div>

        <!-- 商家：档口经营 / 日常使用 两栏可切换（互斥） -->
        <div v-else-if="userStore.isMerchant" class="menu-group">
          <div class="menu-group-title menu-group-title--switch">
            <span>{{ merchantMode === 'merchant' ? '档口经营' : '日常使用' }}</span>
            <button
              class="mode-switch"
              type="button"
              :title="merchantMode === 'merchant' ? '切换到日常使用' : '切换到档口经营'"
              @click="toggleMerchantMode"
            >
              <el-icon :size="13"><component :is="merchantMode === 'merchant' ? 'Refresh' : 'RefreshLeft'" /></el-icon>
              <span>{{ merchantMode === 'merchant' ? '日常使用' : '档口经营' }}</span>
            </button>
          </div>
          <el-menu :default-active="activePath" router class="app-menu">
            <template v-if="merchantMode === 'merchant'">
              <el-menu-item
                v-for="item in merchantMenus"
                :key="item.path"
                :index="'/' + item.path"
              >
                <el-icon><component :is="item.meta.icon" /></el-icon>
                <span>{{ item.meta.title }}</span>
              </el-menu-item>
            </template>
            <template v-else>
              <el-menu-item
                v-for="item in studentMenus"
                :key="item.path"
                :index="'/' + item.path"
              >
                <el-icon><component :is="item.meta.icon" /></el-icon>
                <span>{{ item.meta.title }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </div>

        <!-- 学生：日常使用 -->
        <div v-else-if="studentMenus.length" class="menu-group">
          <div class="menu-group-title">日常使用</div>
          <el-menu :default-active="activePath" router class="app-menu">
            <el-menu-item
              v-for="item in studentMenus"
              :key="item.path"
              :index="'/' + item.path"
            >
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-scrollbar>
    </el-aside>

    <el-container class="app-body">
      <el-header height="64px" class="app-header">
        <div class="header-left">
          <div class="page-heading">{{ currentTitle || '首页' }}</div>
          <div class="page-crumb">
            <el-icon :size="12"><Location /></el-icon>
            首页 / {{ currentTitle || '首页' }}
          </div>
        </div>

        <div class="header-right">
          <el-popover placement="bottom-end" :width="330" trigger="click" @show="loadNotifications">
            <template #reference>
              <div class="notice-btn">
                <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
                  <el-icon :size="20"><Bell /></el-icon>
                </el-badge>
              </div>
            </template>
            <div class="notice-panel">
              <div class="notice-head">
                <span class="notice-title">消息通知</span>
                <el-button v-if="unreadCount" link type="primary" size="small" @click="readAll">全部已读</el-button>
              </div>
              <el-empty v-if="!notifications.length" description="暂无消息" :image-size="56" />
              <div v-else class="notice-list">
                <div
                  v-for="n in notifications"
                  :key="n.id"
                  class="notice-item"
                  :class="{ unread: !n.isRead }"
                  @click="openNotice(n)"
                >
                  <span class="notice-icon">{{ typeIcon(n.type) }}</span>
                  <div class="notice-body">
                    <div class="notice-content">{{ n.content }}</div>
                    <div class="notice-time">{{ n.createTime }}</div>
                  </div>
                  <span v-if="!n.isRead" class="notice-dot"></span>
                </div>
              </div>
            </div>
          </el-popover>

          <el-tag
            :type="userStore.isAdmin ? 'danger' : 'success'"
            size="small"
            effect="light"
            round
            class="role-tag"
          >
            {{ userStore.userInfo?.roleLabel }}
          </el-tag>

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-chip">
              <div class="avatar" :class="{ 'avatar--logo': userStore.isMerchant }">
                <img v-if="userStore.userInfo?.avatar" :src="userStore.userInfo.avatar" class="avatar-img" />
                <template v-else>{{ avatarText }}</template>
              </div>
              <span class="nickname">{{ userStore.nickname }}</span>
              <el-icon :size="12" class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ userStore.userInfo?.realName || userStore.nickname }}
                </el-dropdown-item>
                <el-dropdown-item command="profile" divided>
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <!-- 显式给时长：避免页面根元素上的无限动画让离开过渡挂住、旧页面残留在 DOM 里 -->
          <transition name="page-fade" :duration="{ enter: 140, leave: 100 }">
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
  <ChatWidget />
  <CartBar />
  <CartDrawer />
  <PayDialog />
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import ChatWidget from '@/components/ChatWidget.vue'
import CartBar from '@/components/cart/CartBar.vue'
import CartDrawer from '@/components/cart/CartDrawer.vue'
import PayDialog from '@/components/cart/PayDialog.vue'
import { listNotifications, unreadCount as fetchUnread, readAllNotifications } from '@/api/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const unreadCount = ref(0)
const notifications = ref([])

async function loadUnread() {
  try {
    const res = await fetchUnread()
    unreadCount.value = res.data?.count || 0
  } catch {
    // 忽略：未登录或接口异常时不显示红点
  }
}

async function loadNotifications() {
  try {
    const res = await listNotifications(20)
    notifications.value = res.data || []
  } catch {
    notifications.value = []
  }
}

async function readAll() {
  try {
    await readAllNotifications()
    unreadCount.value = 0
    notifications.value = notifications.value.map(n => ({ ...n, isRead: 1 }))
  } catch {
    // 忽略
  }
}

function typeIcon(type) {
  return { LIKE: '👍', REPLY: '💬', MERCHANT_REPLY: '🏪' }[type] || '🔔'
}

function openNotice(n) {
  if (!n.dishId) return
  // 带 review / reply 定位参数跳菜品详情，详情页会自动展开并高亮对应评价/回复
  const query = {}
  if (n.reviewId) query.review = n.reviewId
  if (n.replyId) query.reply = n.replyId
  router.push({ path: `/dishes/${n.dishId}`, query })
}

onMounted(() => {
  loadUnread()
  // 首次进入时让侧边栏分组与当前路由一致（管理员默认落在数据大屏）
  if (userStore.isAdmin) {
    adminMode.value = route.path.startsWith('/admin') ? 'admin' : 'daily'
  } else if (userStore.isMerchant) {
    merchantMode.value = route.path.startsWith('/merchant') ? 'merchant' : 'daily'
  }
})

// 管理员侧边栏切换：'admin' 后台管理（默认），'daily' 日常使用（与学生端一致）
const adminMode = ref('admin')
// 商家侧边栏切换：'merchant' 档口经营（默认），'daily' 日常使用（与学生端一致）
const merchantMode = ref('merchant')

const allChildren = computed(() => router.options.routes.find(r => r.path === '/')?.children || [])

// 学生日常使用菜单（非 admin、非 merchant），管理员/商家「日常使用」也复用这一组
const studentMenus = computed(() => allChildren.value.filter(i =>
  !i.meta?.hidden && !i.meta?.admin && !i.meta?.merchant
))
// 商家档口经营菜单
const merchantMenus = computed(() => allChildren.value.filter(i =>
  !i.meta?.hidden && !!i.meta?.merchant
))
// 管理员后台管理菜单
const adminMenus = computed(() => allChildren.value.filter(i =>
  !i.meta?.hidden && !!i.meta?.admin
))

const activePath = computed(() => '/' + (route.path.split('/').slice(1, 3).join('/') || 'dashboard'))
const currentTitle = computed(() => route.meta.title || '')

const avatarText = computed(() => (userStore.nickname || 'U').trim().slice(0, 1).toUpperCase())

function toggleAdminMode() {
  adminMode.value = adminMode.value === 'admin' ? 'daily' : 'admin'
  // 跨区切换时跟随落地页，避免菜单与内容不匹配
  if (adminMode.value === 'admin' && !route.path.startsWith('/admin')) {
    router.push('/admin/dashboard')
  } else if (adminMode.value === 'daily' && route.path.startsWith('/admin')) {
    router.push('/dashboard')
  }
}

function toggleMerchantMode() {
  merchantMode.value = merchantMode.value === 'merchant' ? 'daily' : 'merchant'
  if (merchantMode.value === 'merchant' && !route.path.startsWith('/merchant')) {
    router.push('/merchant/todos')
  } else if (merchantMode.value === 'daily' && route.path.startsWith('/merchant')) {
    router.push('/dashboard')
  }
}

function handleCommand(command) {
  if (command === 'logout') {
    doLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

function doLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消'
  })
    .then(() => {
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
    .catch(() => {})
}
</script>

<style scoped>
.app-shell {
  height: 100%;
}

/* 侧边栏 */
.app-aside {
  background: #fff;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  flex-shrink: 0;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand-name {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-1);
  letter-spacing: 0.3px;
}

.brand-sub {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.6px;
}

.menu-scroll {
  flex: 1;
  padding-bottom: 16px;
}

.menu-group-title {
  font-size: 11px;
  color: var(--text-3);
  font-weight: 600;
  letter-spacing: 1.2px;
  padding: 14px 22px 6px;
}

.menu-group-title--switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff;
  color: var(--brand);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-switch:hover {
  background: #fff4ec;
  border-color: var(--brand);
}

.app-menu {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  margin: 3px 12px;
  border-radius: 10px;
  color: var(--text-2);
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.el-menu-item .el-icon) {
  font-size: 17px;
  margin-right: 10px;
}

:deep(.el-menu-item:hover) {
  background: #fff4ec;
  color: var(--brand-dark);
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #fff1e6 0%, #fff9f5 100%);
  color: var(--brand-dark);
  font-weight: 600;
  box-shadow: inset 3px 0 0 var(--brand);
}

/* 顶栏 */
.app-body {
  height: 100%;
}

.app-header {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.page-heading {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-1);
  line-height: 1.2;
}

.page-crumb {
  margin-top: 3px;
  font-size: 12px;
  color: var(--text-3);
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-tag {
  flex-shrink: 0;
}

.notice-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  color: var(--text-2);
  transition: background 0.2s ease;
}

.notice-btn:hover {
  background: #fff4ec;
  color: var(--brand);
}

.notice-panel {
  max-height: 360px;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 8px;
}

.notice-title {
  font-weight: 600;
  color: var(--text-1);
}

.notice-list {
  max-height: 300px;
  overflow-y: auto;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.notice-item:hover {
  background: #f8f9fa;
}

.notice-item.unread {
  background: #fff8f0;
}

.notice-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-content {
  font-size: 13px;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-time {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 2px;
}

.notice-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4d4f;
  flex-shrink: 0;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 5px;
  border-radius: 30px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.user-chip:hover {
  background: #fff4ec;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(255, 106, 0, 0.25);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 商家 logo 去背景后四周留白较多，等比放大填满圆形头像 */
.avatar--logo .avatar-img {
  transform: scale(1.28);
}

.nickname {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-1);
}

.arrow {
  color: var(--text-3);
}

/* 内容区 */
.app-main {
  background: transparent;
  padding: 0;
  overflow-x: hidden;
}
</style>