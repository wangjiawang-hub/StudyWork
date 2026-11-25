<script setup>
/**
 * AI 智能客服页面：
 * - 流式输出：fetch + ReadableStream 解析 SSE（EventSource 不支持 POST，故不用）
 * - 多模态：选择图片转 data URL 随问题一起发送，由视觉模型理解
 * - 会话管理：登录用户历史存云端；匿名会话用 localStorage 记录 id/标题
 * - RAG：开关打开后请求带 useKb，后端先检索知识库再回答
 */
import { ref, nextTick, onMounted } from 'vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const isLogin = () => !!userStore.token

const sessions = ref([])          // [{id, title}]
const currentId = ref('')
const messages = ref([])          // [{role:'user'|'assistant', msgType:'text'|'image', content}]
const input = ref('')
const loading = ref(false)
const useKb = ref(false)
const pendingImages = ref([])     // 待发送图片 data URL

/** @type {import('vue').Ref<HTMLElement|null>} */
const msgBox = ref(null)
const fileInput = ref(null)
const ANON_KEY = 'ai_anon_sessions'

onMounted(loadSessions)

// ==================== 会话 ====================

async function loadSessions() {
  if (isLogin()) {
    const res = await request.get('/ai/sessions')
    sessions.value = res.data || []
  } else {
    sessions.value = JSON.parse(localStorage.getItem(ANON_KEY) || '[]')
  }
  // 默认选中最近一个会话，没有则新建
  if (sessions.value.length) {
    await selectSession(sessions.value[0].id)
  } else {
    await createSession()
  }
}

async function createSession() {
  const res = await request.post('/ai/sessions')
  const s = res.data
  sessions.value.unshift(s)
  if (!isLogin()) persistAnon()
  await selectSession(s.id)
}

async function selectSession(id) {
  if (loading.value) return
  currentId.value = id
  const res = await request.get(`/ai/sessions/${id}/messages`)
  messages.value = res.data || []
  await scrollToBottom()
}

async function deleteSession(id) {
  await ElMessageBox.confirm('确定删除该会话及全部消息？', '提示', { type: 'warning' })
  await request.delete(`/ai/sessions/${id}`)
  sessions.value = sessions.value.filter(s => s.id !== id)
  if (!isLogin()) persistAnon()
  if (currentId.value === id) {
    if (sessions.value.length) await selectSession(sessions.value[0].id)
    else await createSession()
  }
}

function persistAnon() {
  localStorage.setItem(ANON_KEY, JSON.stringify(
    sessions.value.map(s => ({ id: s.id, title: s.title }))))
}

// ==================== 图片（多模态输入） ====================

function triggerPick() {
  fileInput.value?.click()
}

function onPick(e) {
  const files = Array.from(e.target.files || [])
  for (const f of files) {
    if (!f.type.startsWith('image/')) continue
    if (f.size > 4 * 1024 * 1024) {
      ElMessage.warning(`图片 ${f.name} 超过 4MB，已忽略`)
      continue
    }
    if (pendingImages.value.length >= 3) {
      ElMessage.warning('最多发送 3 张图片')
      break
    }
    const reader = new FileReader()
    reader.onload = () => pendingImages.value.push(reader.result)
    reader.readAsDataURL(f)
  }
  e.target.value = ''
}

function removeImage(i) {
  pendingImages.value.splice(i, 1)
}

// ==================== 发送 + SSE 流式接收 ====================

async function scrollToBottom() {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

async function send() {
  const q = input.value.trim()
  if ((!q && !pendingImages.value.length) || loading.value) return
  if (!currentId.value) await createSession()

  // 本地先展示用户消息
  if (q) messages.value.push({ role: 'user', msgType: 'text', content: q })
  for (const img of pendingImages.value) {
    messages.value.push({ role: 'user', msgType: 'image', content: img })
  }
  // 同步侧栏标题（首条提问）
  const cur = sessions.value.find(s => s.id === currentId.value)
  if (cur && (!cur.title || cur.title === '新会话') && q) {
    cur.title = q.length > 20 ? q.slice(0, 20) : q
    if (!isLogin()) persistAnon()
  }

  const payload = {
    sessionId: currentId.value,
    question: q,
    images: pendingImages.value.slice(),
    useKb: useKb.value
  }
  input.value = ''
  pendingImages.value = []
  loading.value = true
  // 占位的 AI 气泡，delta 不断追加
 // const aiMsg = { role: 'assistant', msgType: 'text', content: '' }
//  messages.value.push(aiMsg)
  messages.value.push({ role: 'assistant', msgType: 'text', content: '' })
  const aiIndex = messages.value.length - 1
  await scrollToBottom()

  try {
    const headers = { 'Content-Type': 'application/json' }
    if (userStore.token) headers.Authorization = userStore.token
    const resp = await fetch('/ai/chat/stream', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload)
    })
    if (!resp.ok || !resp.body) throw new Error('流式连接失败：' + resp.status)

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 以空行（\n\n）分隔事件
      const frames = buffer.split('\n\n')
      buffer = frames.pop()
      for (const frame of frames) {
        const line = frame.split('\n').find(l => l.startsWith('data:'))
        if (!line) continue
        const evt = JSON.parse(line.slice(5).trim())
        console.log('收到事件:', evt.type, evt.content || evt.message, Date.now())

        if (evt.type === 'delta') {
          messages.value[aiIndex].content += evt.content
          await scrollToBottom()
          //aiMsg.content += evt.content
         // await new Promise(r => requestAnimationFrame(r))
          //await scrollToBottom()
        } else if (evt.type === 'done') {
          messages.value[aiIndex].content = evt.content
        } else if (evt.type === 'error') {
          messages.value[aiIndex].content = '⚠️ ' + (evt.message || 'AI 暂时无法回复')
        }
      }
    }
    if (! messages.value[aiIndex].content)  messages.value[aiIndex].content = '（AI 未返回内容，请稍后再试）'
  } catch (e) {
    messages.value[aiIndex].content = '⚠️ AI 服务暂时不可用：' + (e.message || '网络错误')
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}
</script>

<template>
  <div class="ai-wrap">
    <!-- 会话侧栏 -->
    <aside class="session-bar">
      <el-button type="primary" class="new-btn" @click="createSession">
        <el-icon><Plus /></el-icon> 新会话
      </el-button>
      <div class="session-list">
        <div
          v-for="s in sessions"
          :key="s.id"
          :class="['session-item', { active: s.id === currentId }]"
          @click="selectSession(s.id)"
        >
          <el-icon class="s-icon"><ChatDotRound /></el-icon>
          <span class="s-title">{{ s.title || '新会话' }}</span>
          <el-icon class="s-del" @click.stop="deleteSession(s.id)"><Delete /></el-icon>
        </div>
      </div>
    </aside>

    <!-- 聊天主区 -->
    <div class="page-card chat-page">
      <div class="chat-header">
        <div class="avatar ai-avatar">
          <el-icon :size="24"><Service /></el-icon>
        </div>
        <div class="hdr-text">
          <h2>AI 智能咨询</h2>
          <p>豆包大模型 · 流式回答 · 支持图片提问 · 知识库增强</p>
        </div>
        <el-tooltip content="开启后，回答会优先参考管理员维护的商城知识库" placement="bottom">
          <el-switch v-model="useKb" active-text="知识库" inline-prompt class="kb-switch" />
        </el-tooltip>
      </div>

      <div ref="msgBox" class="messages">
        <div v-for="(m, i) in messages" :key="i" :class="['msg-row', m.role]">
          <div v-if="m.role === 'assistant'" class="avatar ai-avatar small">
            <el-icon><Service /></el-icon>
          </div>
          <div class="bubble-pkg">
            <div v-if="m.msgType === 'image'" class="bubble img-bubble">
              <el-image :src="m.content" :preview-src-list="[m.content]" fit="cover" class="chat-img" />
            </div>
            <div v-else class="bubble" :class="m.role">{{ m.content }}</div>
          </div>
          <div v-if="m.role === 'user'" class="avatar user-avatar small">
            <el-icon><User /></el-icon>
          </div>
        </div>
<!--        <div v-if="loading && !messages[messages.length - 1]?.content" class="msg-row assistant">-->
<!--          <div class="avatar ai-avatar small"><el-icon><Service /></el-icon></div>-->
<!--          <div class="bubble assistant typing">正在思考...</div>-->
<!--        </div>-->
      </div>

      <!-- 待发送图片缩略图 -->
      <div v-if="pendingImages.length" class="pending-imgs">
        <div v-for="(img, i) in pendingImages" :key="i" class="pending-item">
          <img :src="img" alt="待发送" />
          <el-icon class="rm" @click="removeImage(i)"><Close /></el-icon>
        </div>
      </div>

      <div class="input-row">
        <el-icon class="img-btn" :size="22" @click="triggerPick"><Picture /></el-icon>
        <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="onPick" />
        <el-input
          v-model="input"
          placeholder="输入问题，Enter 发送；可附图片提问（Shift+Enter 换行）"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          :disabled="loading"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" size="large" round :loading="loading" @click="send">
          <el-icon v-if="!loading"><Promotion /></el-icon> 发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-wrap {
  display: flex;
  gap: 16px;
  align-items: stretch;
}
.session-bar {
  width: 220px;
  flex-shrink: 0;
  background: var(--mall-surface);
  border: 1px solid var(--mall-border);
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
}
.new-btn {
  width: 100%;
  margin-bottom: 12px;
}
.session-list {
  flex: 1;
  overflow-y: auto;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--mall-text);
}
.session-item:hover { background: var(--mall-bg); }
.session-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--mall-primary);
}
.s-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.s-del { opacity: 0; transition: opacity .2s; }
.session-item:hover .s-del { opacity: .6; }
.s-del:hover { color: var(--el-color-danger); }

.chat-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 560px;
  padding: 0;
  overflow: hidden;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--mall-primary) 0%, #8b5cf6 100%);
  color: #fff;
}
.hdr-text { flex: 1; }
.chat-header h2 { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.chat-header p { font-size: 13px; opacity: .85; }
.kb-switch { --el-switch-on-color: #fff; }
.avatar {
  width: 48px; height: 48px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.avatar.small { width: 32px; height: 32px; }
.ai-avatar { background: rgba(255,255,255,.2); color: #fff; }
.user-avatar { background: var(--el-color-primary-light-9); color: var(--mall-primary); }

.messages {
  flex: 1; overflow-y: auto; padding: 24px;
  background: var(--mall-bg); min-height: 380px; max-height: 520px;
}
.msg-row { display: flex; align-items: flex-end; gap: 10px; margin-bottom: 16px; }
.msg-row.user { justify-content: flex-end; }
.bubble {
  max-width: 100%; padding: 12px 16px; border-radius: 16px;
  line-height: 1.6; font-size: 14px; white-space: pre-wrap; word-break: break-word;
}
.bubble-pkg { max-width: 70%; }
.bubble.assistant {
  background: var(--mall-surface);
  border: 1px solid var(--mall-border);
  border-bottom-left-radius: 4px;
}
.bubble.user {
  background: linear-gradient(135deg, var(--mall-primary), #8b5cf6);
  color: #fff; border-bottom-right-radius: 4px;
}
.bubble.typing { color: var(--mall-text-secondary); }
.img-bubble { padding: 4px; background: transparent; border: none; }
.chat-img { width: 200px; height: 150px; border-radius: 12px; }

.pending-imgs { display: flex; gap: 10px; padding: 10px 24px 0; }
.pending-item { position: relative; width: 64px; height: 64px; }
.pending-item img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.pending-item .rm {
  position: absolute; top: -6px; right: -6px;
  background: rgba(0,0,0,.6); color: #fff; border-radius: 50%; padding: 2px; cursor: pointer;
}
.input-row {
  display: flex; align-items: flex-end; gap: 12px;
  padding: 16px 24px 20px; border-top: 1px solid var(--mall-border); background: var(--mall-surface);
}
.img-btn { cursor: pointer; color: var(--mall-text-secondary); margin-bottom: 10px; }
.img-btn:hover { color: var(--mall-primary); }
.input-row .el-input { flex: 1; }
</style>
