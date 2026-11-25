const PHONE_REG = /^1[3-9]\d{9}$/
const EMAIL_REG = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

export function isValidPhone(phone) {
  return PHONE_REG.test(String(phone || '').trim())
}

export function isValidEmail(email) {
  return EMAIL_REG.test(String(email || '').trim())
}

/** 登录/注册/验证码相关字段 trim，减少前后空格导致的校验失败 */
export function trimAuthFields(fields) {
  const next = { ...fields }
  for (const key of ['username', 'password', 'phone', 'email', 'captchaCode']) {
    if (typeof next[key] === 'string') next[key] = next[key].trim()
  }
  return next
}
