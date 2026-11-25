import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const BACK_LABELS = {
  '/checkout': '返回结算',
  '/profile': '返回个人中心',
  '/cart': '返回购物车',
  '/orders': '返回订单列表',
  '/products': '返回商品列表',
  '/': '返回首页'
}

/** 页面返回：优先 query.from，其次浏览器历史，最后 fallback */
export function usePageBack(fallback = '/') {
  const route = useRoute()
  const router = useRouter()

  const returnPath = computed(() => {
    const q = route.query.from || route.query.redirect
    return typeof q === 'string' && q.startsWith('/') ? q : null
  })

  const backLabel = computed(() => {
    if (returnPath.value && BACK_LABELS[returnPath.value]) {
      return BACK_LABELS[returnPath.value]
    }
    return '返回'
  })

  function goBack() {
    if (returnPath.value) {
      router.push(returnPath.value)
    } else if (window.history.length > 1) {
      router.back()
    } else {
      router.push(fallback)
    }
  }

  return { goBack, backLabel, returnPath }
}
