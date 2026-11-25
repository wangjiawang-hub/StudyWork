/**
 * 路由表与导航守卫：
 * - meta.auth：需登录（购物车、订单、结算等）
 * - meta.admin：需 role===1（后台子路由）
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  { path: '/products', component: () => import('../views/ProductList.vue') },
  { path: '/product/:id', component: () => import('../views/ProductDetail.vue') },
  { path: '/cart', component: () => import('../views/Cart.vue'), meta: { auth: true } },
  { path: '/checkout', component: () => import('../views/Checkout.vue'), meta: { auth: true } },
  { path: '/pay/result', component: () => import('../views/PayResult.vue'), meta: { auth: true } },
  { path: '/orders', component: () => import('../views/Orders.vue'), meta: { auth: true } },
  { path: '/order/:id', component: () => import('../views/OrderDetail.vue'), meta: { auth: true } },
  { path: '/address', component: () => import('../views/Address.vue'), meta: { auth: true } },
  { path: '/favorite', component: () => import('../views/Favorite.vue'), meta: { auth: true } },
  { path: '/reviews', component: () => import('../views/MyReviews.vue'), meta: { auth: true } },
  { path: '/coupon', component: () => import('../views/Coupon.vue'), meta: { auth: true } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { auth: true } },
  { path: '/ai', component: () => import('../views/AiChat.vue') },
  { path: '/chat', component: () => import('../views/Chat.vue'), meta: { auth: true } },
  { path: '/admin/login', component: () => import('../views/admin/AdminLogin.vue') },
  { path: '/admin', component: () => import('../views/admin/Layout.vue'), meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { admin: true } },
      { path: 'users', component: () => import('../views/admin/Users.vue'), meta: { admin: true } },
      { path: 'categories', component: () => import('../views/admin/Categories.vue'), meta: { admin: true } },
      { path: 'products', component: () => import('../views/admin/Products.vue'), meta: { admin: true } },
      { path: 'coupons', component: () => import('../views/admin/Coupons.vue'), meta: { admin: true } },
      { path: 'orders', component: () => import('../views/admin/Orders.vue'), meta: { admin: true } },
      { path: 'config', component: () => import('../views/admin/Config.vue'), meta: { admin: true } },
      { path: 'logs', component: () => import('../views/admin/Logs.vue'), meta: { admin: true } },
      { path: 'chat', component: () => import('../views/admin/Chat.vue'), meta: { admin: true } },
      { path: 'knowledge', component: () => import('../views/admin/Knowledge.vue'), meta: { admin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0, left: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isAdminRoute = to.path.startsWith('/admin')
  const isAdminLogin = to.path === '/admin/login'

  // 后台路由：须 token 且管理员角色
  if (isAdminRoute && !isAdminLogin) {
    if (!userStore.token) {
      next('/admin/login')
      return
    }
    if (userStore.user?.role !== 1) {
      userStore.logout()
      next('/admin/login')
      return
    }
  }

  if (isAdminLogin && userStore.token && userStore.user?.role === 1) {
    next('/admin/users')
    return
  }

  // 前台需登录页面（cart/checkout/orders 等）
  if (to.meta.auth && !userStore.token) {
    next('/login')
    return
  }
  if (to.meta.admin && userStore.user?.role !== 1) {
    next('/')
    return
  }
  next()
})

export default router
