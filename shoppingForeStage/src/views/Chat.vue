<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import PageHeader from '../components/PageHeader.vue'

const userStore = useUserStore()
const session = ref(null)
const messages = ref([])
const input = ref('')
const loading = ref(true)
const ws = ref(null)
const scrollRef = ref(null)

onMounted(init)
onUnmounted(() => ws.value?.close())

async function init() {
  loading.value = true
  try {
    // 1. 拿自己的会话（没有则后端自动建一个待接入）
    const res = await request.get('/admin/customer/my-session')
    session.value = res.data
    // 2. 拉历史消息
    const m = await request.get('/admin/customer/messages', { params: { sessionId: session.value.id } })
    messages.value = m.data || []
    await nextTick()
    scrollToBottom()
    // 3. 建立 WebSocket
    connectWs()
  } finally {
    loading.value = false
  }
}

function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${proto}://${location.host}/ws/chat?token=${userStore.token}`
  ws.value = new WebSocket(url)
  ws.value.onopen = () => console.log('[Chat] WS 已连接')
  ws.value.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.type === 'CHAT') {
        messages.value.push({
          id: Date.now(),
          senderId: data.senderId,
          senderType: data.senderType,
          content: data.content,
          createTime: data.createTime
        })
        scrollToBottom()
      } else if (data.type === 'JOIN') {
        ElMessage.success('客服已接入')
        session.value.status = 1
      } else if (data.type === 'CLOSE') {
        ElMessage.info('会话已结束')
        session.value.status = 2
      }
    } catch (err) {
      console.warn('[Chat] 消息解析失败', err)
    }
  }
  ws.value.onclose = () => console.log('[Chat] WS 已关闭')
  ws.value.onerror = (e) => console.error('[Chat] WS 错误', e)
}

function send() {
  const content = input.value.trim()
  if (!content || !session.value) return
  if (ws.value?.readyState !== WebSocket.OPEN) {
    ElMessage.warning('连接未就绪')
    return
  }
  ws.value.send(JSON.stringify({
    type: 'CHAT',
    sessionId: session.value.id,
    content
  }))
  input.value = ''
}

function scrollToBottom() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  })
}

function fmt(ts) {
  if (!ts) return ''
  const d = new Date(typeof ts === 'number' ? ts : Date.parse(ts))
  return d.toLocaleTimeString()
}
</script>

<template>
  <div v-loading="loading" class="page-card">
    <PageHeader title="人工客服" icon="Service" fallback="/" back-text="返回首页" />

    <div class="chat-wrap">
      <div ref="scrollRef" class="msg-list">
        <el-empty v-if="!messages.length" description="暂无消息，向客服说点什么吧" :image-size="80" />
        <div
          v-for="m in messages"
          :key="m.id"
          class="msg-row"
          :class="m.senderType === 0 ? 'me' : 'cs'"
        >
          <div class="bubble">
            <div class="meta">{{ m.senderType === 0 ? '我' : '客服' }} · {{ fmt(m.createTime) }}</div>
            <div class="text">{{ m.content }}</div>
          </div>
        </div>
        <div v-if="session?.status === 0" class="tip">客服尚未接入，您的消息已留言，客服上线后将看到</div>
        <div v-else-if="session?.status === 2" class="tip">会话已结束</div>
      </div>

      <div class="input-area">
        <el-input
          v-model="input"
          placeholder="输入消息，回车发送"
          :disabled="session?.status === 2"
          @keyup.enter="send"
        />
        <el-button type="primary" :disabled="session?.status === 2" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-wrap {
  background: var(--mall-bg);
  border-radius: var(--mall-radius-sm);
  padding: 16px;
  display: flex;
  flex-direction: column;
  height: 540px;
}
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
}
.msg-row {
  display: flex;
  margin: 10px 0;
}
.msg-row.me { justify-content: flex-end; }
.msg-row.cs { justify-content: flex-start; }
.bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--mall-border);
}
.msg-row.me .bubble {
  background: var(--mall-primary);
  color: #fff;
  border-color: var(--mall-primary);
}
.meta { font-size: 11px; opacity: 0.7; margin-bottom: 4px; }
.text { font-size: 14px; word-break: break-word; white-space: pre-wrap; }
.tip {
  text-align: center;
  color: var(--mall-text-secondary);
  font-size: 12px;
  margin: 12px 0;
}
.input-area {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--mall-border);
}
</style>
