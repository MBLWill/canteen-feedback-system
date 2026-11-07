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
        <div v-if="merchantMenus.length" class="menu-group">
          <div class="menu-group-title">档口经营</div>
          <el-menu :default-active="activePath" router class="app-menu">
            <el-menu-item
              v-for="item in merchantMenus"
              :key="item.path"
              :index="'/' + item.path"
            >
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </el-menu-item>
          </el-menu>
        </div>

        <div v-if="studentMenus.length" class="menu-group">
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

        <div v-if="adminMenus.length" class="menu-group">
          <div class="menu-group-title">后台管理</div>
          <el-menu :default-active="activePath" router class="app-menu">
            <el-menu-item
              v-for="item in adminMenus"
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
              <div class="avatar">
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
          <transition name="page-fade" mode="out-in">
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import ChatWidget from '@/components/ChatWidget.vue'
import CartBar from '@/components/cart/CartBar.vue'
import CartDrawer from '@/components/cart/CartDrawer.vue'
import PayDialog from '@/components/cart/PayDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => {
  const children = router.options.routes.find(r => r.path === '/')?.children || []
  return children.filter(item => {
    if (item.meta?.hidden) return false
    if (item.meta?.admin && !userStore.isAdmin) return false
    if (item.meta?.merchant && !userStore.isMerchant) return false
    // 商家只保留 merchant 菜单；学生/管理员看不到 merchant 菜单
    if (userStore.isMerchant !== !!item.meta?.merchant) return false
    return true
  })
})

const studentMenus = computed(() => menus.value.filter(i => !String(i.path).startsWith('admin') && !i.meta?.merchant))
const merchantMenus = computed(() => menus.value.filter(i => !!i.meta?.merchant))
const adminMenus = computed(() => menus.value.filter(i => String(i.path).startsWith('admin')))

const activePath = computed(() => '/' + (route.path.split('/').slice(1, 3).join('/') || 'dashboard'))
const currentTitle = computed(() => route.meta.title || '')

const avatarText = computed(() => (userStore.nickname || 'U').trim().slice(0, 1).toUpperCase())

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

.role-tag {
  margin-right: 14px;
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
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(255, 106, 0, 0.25);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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