/**
 * 支付宝沙箱支付工具：发起支付、解析回跳参数、同步订单状态。
 * 流程：submitAlipayPay → 新窗口打开 HTML 表单 → 用户支付 → PayResult 调 syncPayStatus
 */

/** 将后端返回的支付宝 HTML 表单写入新窗口并自动提交 */
export function openPayForm(html) {
  const win = window.open('', '_blank')
  if (!win) return false
  win.document.open()
  win.document.write(html)
  win.document.close()
  return true
}

/** 请求 /alipay/pay 获取表单 HTML，traceNo 对应订单号 orderNo */
export async function submitAlipayPay(request, { orderNo, payAmount, subject = '商城订单' }) {
  const pay = await request.post('/alipay/pay', null, {
    params: {
      traceNo: orderNo,
      totalAmount: payAmount,
      subject
    }
  })
  if (!openPayForm(pay.data)) {
    throw new Error('无法打开支付页，请允许弹出窗口')
  }
  return pay
}

/** 支付宝浏览器回跳标准参数，过滤 Vue Router 混入的 query 字段 */
const ALIPAY_RETURN_KEYS = [
  'charset', 'out_trade_no', 'method', 'total_amount', 'sign', 'trade_no',
  'auth_app_id', 'version', 'app_id', 'sign_type', 'seller_id', 'timestamp'
]

/** 从 /pay/result 的 query 提取验签参数 */
export function extractAlipayReturnParams(query) {
  const params = {}
  for (const key of ALIPAY_RETURN_KEYS) {
    if (query[key] != null && query[key] !== '') {
      params[key] = String(query[key])
    }
  }
  return params
}

/** 同步支付：有 sign 则验签更新，否则仅按 out_trade_no 查单 */
export async function syncPayStatus(request, orderNo, returnParams) {
  const payload = returnParams && Object.keys(returnParams).length
    ? { ...returnParams, out_trade_no: returnParams.out_trade_no || orderNo }
    : { out_trade_no: orderNo }
  return request.post('/alipay/syncPay', payload)
}
