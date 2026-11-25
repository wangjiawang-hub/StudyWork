<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const pending = ref([])
const active = ref([])
const currentSession = ref(null)
const messages = ref([])
const input = ref('')
const ws = ref(null)
const msgRef = ref(null)

onMounted(init)
onUnmounted(() => ws.value?.close())

async function init() {
  await loadSessions()
  connectWs()
}

async function loadSessions() {
  const res = await request.get('/admin/customer/sessions')
  pending.value = res.data?.pending || []
  active.value = res.data?.active || []
}

function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${proto}://${location.host}/ws/chat?token=${userStore.token}`
  ws.value = new WebSocket(url)
  ws.value.onopen = () => console.log('[AdminChat] WS 已连接')
  ws.value.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.type === 'CHAT' && currentSession.value?.id === data.sessionId) {
        messages.value.push({
          id: Date.now(),
          senderId: data.senderId,
          senderType: data.senderType,
          content: data.content,
          createTime: data.createTime
        })
        scrollToBottom()
      } else if (data.type === 'JOIN') {
        loadSessions()
      } else if (data.type === 'CLOSE') {
        ElMessage.info('会话已关闭')
        if (currentSession.value?.id === data.sessionId) {
          currentSession.value = null
          messages.value = []
        }
        loadSessions()
      }
    } catch (err) {
      console.warn('[AdminChat] 解析失败', err)
    }
  }
  ws.value.onclose = () => console.log('[AdminChat] WS 关闭')
}

async function selectSession(s) {
  currentSession.value = s
  const m = await request.get('/admin/customer/messages', { params: { sessionId: s.id } })
  messages.value = m.data || []
  await scrollToBottom()
}

async function joinSession(s) {
  if (ws.value?.readyState !== WebSocket.OPEN) {
    ElMessage.warning('连接未就绪')
    return
  }
  ws.value.send(JSON.stringify({ type: 'JOIN', sessionId: s.id }))
  ElMessage.success('已接入')
  currentSession.value = s
  await nextTick()
  await selectSession(s)
}

function send() {
  const content = input.value.trim()
  if (!content || !currentSession.value) return
  if (ws.value?.readyState !== WebSocket.OPEN) {
    ElMessage.warning('连接未就绪')
    return
  }
  ws.value.send(JSON.stringify({
    type: 'CHAT',
    sessionId: currentSession.value.id,
    content
  }))
  messages.value.push({
    id: Date.now(),
    senderId: Number(userStore.user?.id || 0),
    senderType: 1,
    content,
    createTime: Date.now()
  })
  input.value = ''
  scrollToBottom()
}

async function closeSession() {
  if (!currentSession.value) return
  try {
    await ElMessageBox.confirm('确认结束当前会话？', '提示', { type: 'warning' })
    await request.post('/admin/customer/close', null, { params: { sessionId: currentSession.value.id } })
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type: 'CLOSE', sessionId: currentSession.value.id }))
    }
    ElMessage.success('已结束')
    currentSession.value = null
    messages.value = []
    await loadSessions()
  } catch {}
}

function scrollToBottom() {
  return nextTick(() => {
    if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
  })
}

function fmt(ts) {
  if (!ts) return ''
  const d = new Date(typeof ts === 'number' ? ts : Date.parse(ts))
  return d.toLocaleTimeString()
}
</script>

<template>
  <div class="chat-page">
    <h2>客服会话</h2>
    <div class="layout">
      <!-- 左侧：会话列表 -->
      <aside class="sidebar">
        <div class="sec-title">待接入 ({{ pending.length }})</div>
        <div v-for="s in pending" :key="s.id" class="session-item pending" @click="joinSession(s)">
          <div class="s-name">{{ s.username || '用户#' + s.userId }}</div>
          <div class="s-meta">未读 {{ s.unreadCount }} 条</div>
        </div>
        <el-empty v-if="!pending.length" :image-size="40" description="无待接入" />

        <div class="sec-title">已接入 ({{ active.length }})</div>
        <div
          v-for="s in active"
          :key="s.id"
          class="session-item"
          :class="{ active: currentSession?.id === s.id }"
          @click="selectSession(s)"
        >
          <div class="s-name">{{ s.username || '用户#' + s.userId }}</div>
          <div class="s-meta">{{ s.unreadCount > 0 ? s.unreadCount + ' 未读' : '无未读' }}</div>
        </div>
        <el-empty v-if="!active.length" :image-size="40" description="无活跃会话" />
      </aside>

      <!-- 右侧：消息区 -->
      <main class="main">
        <template v-if="currentSession">
          <div class="header">
            <span>{{ currentSession.username || '用户#' + currentSession.userId }}</span>
            <el-button size="small" type="danger" plain @click="closeSession">结束会话</el-button>
          </div>
          <div ref="msgRef" class="msg-list">
            <div
              v-for="m in messages"
              :key="m.id"
              class="msg-row"
              :class="m.senderType === 1 ? 'me' : 'other'"
            >
              <div class="bubble">
                <div class="meta">{{ m.senderType === 1 ? '客服' : '用户' }} · {{ fmt(m.createTime) }}</div>
                <div class="text">{{ m.content }}</div>
              </div>
            </div>
          </div>
          <div class="input-area">
            <el-input v-model="input" placeholder="输入回复，回车发送" @keyup.enter="send" />
            <el-button type="primary" @click="send">发送</el-button>
          </div>
        </template>
        <el-empty v-else description="选择左侧会话开始对话" :image-size="100" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.chat-page h2 { margin: 0 0 16px; }
.layout {
  display: flex;
  gap: 16px;
  height: 600px;
  background: #fff;
  border-radius: var(--mall-radius);
  border: 1px solid var(--mall-border);
  overflow: hidden;
}
.sidebar {
  width: 280px;
  border-right: 1px solid var(--mall-border);
  overflow-y: auto;
  padding: 12px;
  background: var(--mall-bg);
}
.sec-title {
  font-size: 12px;
  color: var(--mall-text-secondary);
  margin: 12px 0 6px;
  padding: 0 4px;
}
.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.15s;
}
.session-item:hover { background: #fff; }
.session-item.active { background: var(--el-color-primary-light-9); }
.session-item.pending { background: #fff7e6; }
.session-item.pending:hover { background: #ffeec0; }
.s-name { font-size: 14px; font-weight: 500; }
.s-meta { font-size: 12px; color: var(--mall-text-secondary); margin-top: 2px; }

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--mall-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
.msg-row { display: flex; margin: 10px 0; }
.msg-row.me { justify-content: flex-end; }
.msg-row.other { justify-content: flex-start; }
.bubble {
  max-width: 65%;
  padding: 8px 12px;
  border-radius: 12px;
  background: var(--mall-bg);
  border: 1px solid var(--mall-border);
}
.msg-row.me .bubble {
  background: var(--mall-primary);
  color: #fff;
  border-color: var(--mall-primary);
}
.meta { font-size: 11px; opacity: 0.7; margin-bottom: 4px; }
.text { font-size: 14px; word-break: break-word; white-space: pre-wrap; }
.input-area {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--mall-border);
}
</style>
