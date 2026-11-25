/** 商城全局配置：站点名、包邮门槛、运费；结算页 calcFreight 依赖此 store */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

export const useMallStore = defineStore('mall', () => {
  const siteName = ref('购物商城')
  const freeShippingAmount = ref(99)
  const shippingFee = ref(10)
  const loaded = ref(false)

  const freeShippingTip = computed(() => `满${freeShippingAmount.value}元包邮`)

  /** 满 freeShippingAmount 包邮，否则收 shippingFee（与后端 MallConfigReader 规则一致） */
  function calcFreight(merchandiseTotal) {
    const total = Number(merchandiseTotal) || 0
    return total >= freeShippingAmount.value ? 0 : shippingFee.value
  }

  function applyDocumentTitle() {
    document.title = `${siteName.value} 购物商城`
  }

  async function load() {
    try {
      const res = await request.get('/admin/config/public')
      const data = res.data || {}
      if (data.siteName) siteName.value = data.siteName
      if (data.freeShippingAmount != null) freeShippingAmount.value = Number(data.freeShippingAmount)
      if (data.shippingFee != null) shippingFee.value = Number(data.shippingFee)
      applyDocumentTitle()
    } catch {
      applyDocumentTitle()
    } finally {
      loaded.value = true
    }
  }

  return {
    siteName,
    freeShippingAmount,
    shippingFee,
    loaded,
    freeShippingTip,
    calcFreight,
    load,
    applyDocumentTitle
  }
})
