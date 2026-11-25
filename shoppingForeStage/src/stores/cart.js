/** 购物车角标：汇总 /cart/list 各商品 quantity，供 AppHeader 展示 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'

export const useCartStore = defineStore('cart', () => {
  const count = ref(0)

  async function refreshCount() {
    try {
      const res = await request.get('/cart/list')
      count.value = (res.data || []).reduce((s, i) => s + i.quantity, 0)
    } catch {
      count.value = 0
    }
  }

  return { count, refreshCount }
})
