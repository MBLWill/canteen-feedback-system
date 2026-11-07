import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/request'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Statistics.vue'), meta: { title: '数据看板', icon: 'DataLine' } },
      { path: 'dishes', name: 'DishList', component: () => import('@/views/DishList.vue'), meta: { title: '美食探索', icon: 'Shop' } },
      { path: 'stalls/south', redirect: '/dishes?area=south&floor=1', meta: { hidden: true } },
      { path: 'stalls/north', redirect: '/dishes?area=north&floor=1', meta: { hidden: true } },
      { path: 'dishes/:id', name: 'DishDetail', component: () => import('@/views/DishDetail.vue'), meta: { hidden: true, title: '菜品详情' } },
      { path: 'my/orders', name: 'MyOrders', component: () => import('@/views/MyOrders.vue'), meta: { title: '我的订单', icon: 'List' } },
      { path: 'my/reviews', name: 'MyReviews', component: () => import('@/views/MyReviews.vue'), meta: { title: '我的评价', icon: 'EditPen' } },
      { path: 'my/favorites', name: 'MyFavorites', component: () => import('@/views/MyFavorites.vue'), meta: { title: '我的收藏', icon: 'Star' } },
      { path: 'merchant/todos', name: 'MerchantTodos', component: () => import('@/views/merchant/MerchantTodos.vue'), meta: { title: '待办事项', icon: 'Bell', merchant: true } },
      { path: 'merchant/orders', name: 'MerchantOrders', component: () => import('@/views/merchant/MerchantOrders.vue'), meta: { title: '订单处理', icon: 'List', merchant: true } },
      { path: 'merchant/sales', name: 'MerchantSales', component: () => import('@/views/merchant/MerchantSales.vue'), meta: { title: '销售情况', icon: 'TrendCharts', merchant: true } },
      { path: 'merchant/dishes', name: 'MerchantDishes', component: () => import('@/views/merchant/MerchantDishes.vue'), meta: { title: '菜品管理', icon: 'Dish', merchant: true } },
      { path: 'merchant/messages', name: 'MerchantMessages', component: () => import('@/views/merchant/MerchantMessages.vue'), meta: { title: '给管理员留言', icon: 'Message', merchant: true } },
      { path: 'merchant/reviews', name: 'MerchantReviews', component: () => import('@/views/merchant/MerchantReviews.vue'), meta: { title: '学生评价', icon: 'Star', merchant: true } },
      { path: 'feedbacks', name: 'FeedbackList', component: () => import('@/views/FeedbackList.vue'), meta: { title: '留言反馈', icon: 'ChatDotRound' } },
      { path: 'notices', name: 'NoticeList', component: () => import('@/views/NoticeList.vue'), meta: { title: '食堂公告', icon: 'Bell' } },
      { path: 'admin/banners', name: 'BannerManage', component: () => import('@/views/admin/BannerManage.vue'), meta: { title: '轮播广告', icon: 'Picture', admin: true } },
      { path: 'admin/stalls', name: 'StallManage', component: () => import('@/views/admin/StallManage.vue'), meta: { title: '档口管理', icon: 'Shop', admin: true } },
      { path: 'admin/stalls/:id', name: 'AdminStallDishes', component: () => import('@/views/admin/AdminStallDishes.vue'), meta: { hidden: true, title: '档口菜单', admin: true } },
      { path: 'admin/reviews', name: 'ReviewManage', component: () => import('@/views/admin/ReviewManage.vue'), meta: { title: '评价审核', icon: 'Document', admin: true } },
      { path: 'admin/feedbacks', name: 'FeedbackManage', component: () => import('@/views/FeedbackList.vue'), meta: { title: '留言反馈', icon: 'Message', admin: true } },
      { path: 'admin/notices', name: 'NoticeManage', component: () => import('@/views/admin/NoticeManage.vue'), meta: { title: '公告管理', icon: 'Notification', admin: true } },
      { path: 'admin/orders', name: 'OrderManage', component: () => import('@/views/admin/OrderManage.vue'), meta: { title: '订单管理', icon: 'Calendar', admin: true } },
      { path: 'admin/merchant-messages', name: 'MerchantMessageManage', component: () => import('@/views/admin/MerchantMessageManage.vue'), meta: { title: '商家留言', icon: 'Message', admin: true } },
      { path: 'admin/users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理', icon: 'User', admin: true } },
      { path: 'admin/merchants', name: 'MerchantManage', component: () => import('@/views/admin/MerchantManage.vue'), meta: { title: '商家管理', icon: 'Shop', admin: true } },
      { path: 'admin/faqs', name: 'FaqManage', component: () => import('@/views/admin/FaqManage.vue'), meta: { title: '客服知识库', icon: 'Collection', admin: true } },
      { path: 'admin/chat-logs', name: 'ChatLogManage', component: () => import('@/views/admin/ChatLogManage.vue'), meta: { title: '客服问答记录', icon: 'ChatLineSquare', admin: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心', hidden: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    return next()
  }
  if (!getToken()) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  next()
})

export default router
