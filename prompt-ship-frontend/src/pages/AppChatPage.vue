<template>
  <main class="chat-page">
    <aside class="chat-sidebar">
      <div class="app-title-row">
        <RouterLink to="/" class="back-link">返回首页</RouterLink>
        <a-tag :color="generationStatus.color">{{ generationStatus.text }}</a-tag>
      </div>
      <h1>{{ appTitle }}</h1>
      <p class="app-subtitle">用自然语言描述需求，AI 会为你生成可预览的 Web 应用。</p>
      <div ref="messageListRef" class="message-list">
        <div v-if="loadingHistory" class="message-state">正在加载历史对话...</div>
        <div v-else-if="!chatMessages.length" class="message-state">还没有对话，输入需求后开始生成。</div>
        <article
          v-for="chatMessage in chatMessages"
          :key="chatMessage.id"
          class="message-row"
          :class="chatMessage.role === 'user' ? 'user-row' : 'ai-row'"
        >
          <template v-if="chatMessage.role === 'user'">
            <div class="message-bubble user-bubble">
              <p>{{ chatMessage.content }}</p>
            </div>
            <a-avatar :src="userStore.loginUser?.userAvatar">{{ userInitial }}</a-avatar>
          </template>
          <template v-else>
            <a-avatar :src="assistantAvatar" />
            <div class="message-bubble ai-bubble">
              <div class="message-role">PromptShip AI</div>
              <p v-if="!chatMessage.content" class="muted-text">
                {{ chatMessage.streaming ? 'AI 正在生成回复...' : 'AI 暂无回复内容' }}
              </p>
              <div v-else class="step-list">
                <section
                  v-for="step in parseAssistantSteps(chatMessage.content)"
                  :key="step.key"
                  class="step-card"
                >
                  <div class="step-card-header">
                    <span>{{ step.label }}</span>
                    <strong>{{ step.title }}</strong>
                  </div>
                  <p>{{ step.content }}</p>
                </section>
              </div>
            </div>
          </template>
        </article>
      </div>
      <div class="chat-input-box">
        <a-textarea
          v-model:value="draftPrompt"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          :placeholder="inputPlaceholder"
          :disabled="loadingHistory"
          @keydown.ctrl.enter.prevent="handleSubmit"
          @keydown.meta.enter.prevent="handleSubmit"
        />
        <div class="chat-actions">
          <span>{{ helperText }}</span>
          <a-button
            type="primary"
            shape="round"
            :loading="generating"
            :disabled="!canSubmit"
            @click="handleSubmit"
            >{{ submitButtonText }}</a-button
          >
        </div>
      </div>
    </aside>
    <section class="preview-panel">
      <header class="preview-header">
        <div>
          <span class="section-label">应用预览</span>
          <h2>{{ previewTitle }}</h2>
        </div>
        <div class="preview-actions">
          <a-button :disabled="!previewUrl" shape="round" @click="reloadPreview">刷新预览</a-button>
          <a-button :disabled="!previewUrl" shape="round" @click="openPreview">新窗口打开</a-button>
          <a-button
            type="primary"
            shape="round"
            :loading="deploying"
            :disabled="!currentAppId || generating"
            @click="handleDeploy"
            >部署</a-button
          >
        </div>
      </header>
      <div class="preview-frame-wrap">
        <iframe v-if="previewUrl" :key="previewKey" :src="previewUrl" title="生成应用预览" />
        <div v-else class="preview-empty">
          <h3>等待生成应用</h3>
          <p>AI 开始生成后，这里会显示本地预览页面。</p>
        </div>
      </div>
      <a-alert
        v-if="deployUrl"
        class="deploy-alert"
        type="success"
        show-icon
        message="部署成功"
        :description="deployUrl"
      />
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  chatContinueApp,
  chatToGenerateApp,
  deployMyApp,
  getAppDetail,
  getGeneratedPreviewUrl,
  INITIAL_PROMPT_STORAGE_KEY,
  type AppId,
  type AppVO,
} from '@/api/services/appService'
import {
  listAppChatHistory,
  type ChatHistoryRecord,
  type ChatRole,
} from '@/api/services/chatHistoryService'
import { useUserStore } from '@/stores/user'
import assistantAvatar from '@/resource/assistant-avatar.png'

type ChatMessage = {
  id: string
  role: ChatRole
  content: string
  streaming?: boolean
}

type AssistantStep = {
  key: string
  label: string
  title: string
  content: string
}

const HISTORY_PAGE_SIZE = 20

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentAppId = ref<AppId>()
const appDetail = ref<AppVO>()
const initialPrompt = ref('')
const draftPrompt = ref('')
const chatMessages = ref<ChatMessage[]>([])
const loadingHistory = ref(false)
const generating = ref(false)
const generated = ref(false)
const deploying = ref(false)
const deployUrl = ref('')
const previewKey = ref(0)
const messageListRef = ref<HTMLElement>()
let localMessageId = 0

const userInitial = computed(() =>
  (userStore.loginUser?.userName || userStore.loginUser?.userAccount || '你')
    .slice(0, 1)
    .toUpperCase(),
)
const appTitle = computed(
  () => appDetail.value?.appName || initialPrompt.value.slice(0, 18) || '新应用',
)
const previewUrl = computed(() =>
  getGeneratedPreviewUrl(currentAppId.value, appDetail.value?.codeGenType),
)
const previewTitle = computed(() =>
  currentAppId.value ? 'App #' + currentAppId.value : '生成中的应用',
)
const canSubmit = computed(() => Boolean(draftPrompt.value.trim()) && !generating.value && !loadingHistory.value)
const inputPlaceholder = computed(() =>
  currentAppId.value ? '继续描述你想调整的功能、样式或内容' : '描述你想生成的 Web 应用',
)
const helperText = computed(() =>
  currentAppId.value ? '当前应用 ID：' + currentAppId.value : '即将创建新应用',
)
const submitButtonText = computed(() => (currentAppId.value ? '发送' : '开始生成'))
const generationStatus = computed(() =>
  generating.value
    ? { color: 'processing', text: '生成中' }
    : loadingHistory.value
      ? { color: 'processing', text: '加载历史' }
      : generated.value || currentAppId.value
        ? { color: 'success', text: '可续聊' }
        : { color: 'default', text: '待生成' },
)

const createLocalMessageId = () => 'local-' + ++localMessageId

const scrollMessagesToBottom = async () => {
  await nextTick()
  const messageList = messageListRef.value
  if (messageList) {
    messageList.scrollTop = messageList.scrollHeight
  }
}

const toChatMessage = (record: ChatHistoryRecord, index: number): ChatMessage | undefined => {
  if (record.role !== 'user' && record.role !== 'assistant') {
    return undefined
  }
  return {
    id: record.id || 'history-' + (record.createTime || index) + '-' + index,
    role: record.role,
    content: record.content || '',
  }
}

const appendMessage = (role: ChatRole, content: string, streaming = false) => {
  const chatMessage: ChatMessage = {
    id: createLocalMessageId(),
    role,
    content,
    streaming,
  }
  chatMessages.value.push(chatMessage)
  void scrollMessagesToBottom()
  return chatMessage
}

const updateMessage = (id: string, updater: (message: ChatMessage) => ChatMessage) => {
  const index = chatMessages.value.findIndex((message) => message.id === id)
  if (index < 0) {
    return
  }
  chatMessages.value.splice(index, 1, updater(chatMessages.value[index]))
  void scrollMessagesToBottom()
}

const parseAssistantSteps = (content: string): AssistantStep[] => {
  const normalizedContent = content.trim()
  if (!normalizedContent) {
    return []
  }

  const stepPattern = /(^|\n)\s*STEP\s+(\d+)\s*(?::|：|-|—)?\s*([^\n]*)/gi
  const matches = [...normalizedContent.matchAll(stepPattern)]

  if (!matches.length) {
    return [
      {
        key: 'plain',
        label: 'AI',
        title: '回复内容',
        content: normalizedContent,
      },
    ]
  }

  const steps = matches.map((match, index) => {
    const nextMatch = matches[index + 1]
    const contentStart = (match.index ?? 0) + match[0].length
    const contentEnd = nextMatch?.index ?? normalizedContent.length
    const stepNumber = match[2]
    const title = match[3]?.trim() || '步骤 ' + stepNumber
    const stepContent = normalizedContent.slice(contentStart, contentEnd).trim()

    return {
      key: stepNumber + '-' + index,
      label: 'STEP ' + stepNumber,
      title,
      content: stepContent || '该步骤暂无详细内容',
    }
  })

  const introContent = normalizedContent.slice(0, matches[0]?.index ?? 0).trim()
  if (introContent) {
    steps.unshift({
      key: 'intro',
      label: 'AI',
      title: '回复说明',
      content: introContent,
    })
  }

  return steps
}

const loadChatHistory = async (appId: AppId) => {
  loadingHistory.value = true
  const allRecords: ChatHistoryRecord[] = []
  let cursor: string | undefined

  try {
    while (true) {
      const res = await listAppChatHistory({
        appId,
        cursor,
        pageSize: HISTORY_PAGE_SIZE,
      })

      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '历史对话加载失败')
      }

      const page = res.data.data
      allRecords.push(...page.records)

      if (!page.hasMore || !page.nextCursor || page.nextCursor === cursor) {
        break
      }
      cursor = page.nextCursor
    }

    chatMessages.value = allRecords
      .map((record, index) => toChatMessage(record, index))
      .filter(Boolean) as ChatMessage[]

    if (!chatMessages.value.length && initialPrompt.value.trim()) {
      chatMessages.value = [
        {
          id: 'fallback-init-prompt',
          role: 'user',
          content: initialPrompt.value.trim(),
        },
      ]
    }
    await scrollMessagesToBottom()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '历史对话加载失败')
  } finally {
    loadingHistory.value = false
  }
}

const loadExistingApp = async (id: AppId, shouldLoadHistory = true) => {
  const res = await getAppDetail(id)
  if (res.data.code === 0 && res.data.data) {
    appDetail.value = res.data.data
    initialPrompt.value = res.data.data.initPrompt || ''
    draftPrompt.value = ''
    currentAppId.value = id
    generated.value = true
    if (shouldLoadHistory) {
      await loadChatHistory(id)
    }
    return
  }
  message.error(res.data.message || '应用加载失败')
}

const startGeneration = async () => {
  const prompt = draftPrompt.value.trim()
  if (!prompt) {
    message.warning('请先输入你想生成的应用需求')
    return
  }

  initialPrompt.value = prompt
  chatMessages.value = []
  appendMessage('user', prompt)
  const assistantMessage = appendMessage('assistant', '', true)
  const assistantMessageId = assistantMessage.id
  generating.value = true
  deployUrl.value = ''
  draftPrompt.value = ''

  try {
    await chatToGenerateApp(
      { initPrompt: prompt },
      {
        onAppId: (appId) => {
          currentAppId.value = appId
        },
        onChunk: (chunk) => {
          updateMessage(assistantMessageId, (message) => ({
            ...message,
            content: message.content + chunk,
          }))
        },
        onDone: () => {
          updateMessage(assistantMessageId, (message) => ({
            ...message,
            streaming: false,
          }))
          generated.value = true
        },
      },
    )
    updateMessage(assistantMessageId, (message) => ({
      ...message,
      streaming: false,
    }))
    generated.value = true
    message.success('应用生成完成')
  } catch (error) {
    updateMessage(assistantMessageId, (message) => ({
      ...message,
      streaming: false,
    }))
    message.error(error instanceof Error ? error.message : '生成失败，请稍后重试')
  } finally {
    generating.value = false
    sessionStorage.removeItem(INITIAL_PROMPT_STORAGE_KEY)
    if (currentAppId.value) {
      await loadExistingApp(currentAppId.value, false).catch(() => undefined)
      previewKey.value += 1
      if (route.params.id !== currentAppId.value) {
        await router.replace('/app/' + currentAppId.value + '/chat')
      }
    }
  }
}

const continueChat = async () => {
  const appId = currentAppId.value
  const prompt = draftPrompt.value.trim()
  if (!appId || !prompt) {
    return
  }

  appendMessage('user', prompt)
  const assistantMessage = appendMessage('assistant', '', true)
  const assistantMessageId = assistantMessage.id
  generating.value = true
  deployUrl.value = ''
  draftPrompt.value = ''

  try {
    await chatContinueApp(appId, prompt, {
      onChunk: (chunk) => {
        updateMessage(assistantMessageId, (message) => ({
          ...message,
          content: message.content + chunk,
        }))
      },
      onDone: () => {
        updateMessage(assistantMessageId, (message) => ({
          ...message,
          streaming: false,
        }))
      },
    })
    updateMessage(assistantMessageId, (message) => ({
      ...message,
      streaming: false,
    }))
    message.success('回复生成完成')
    previewKey.value += 1
  } catch (error) {
    updateMessage(assistantMessageId, (message) => ({
      ...message,
      streaming: false,
    }))
    message.error(error instanceof Error ? error.message : '继续对话失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

const handleSubmit = async () => {
  if (!canSubmit.value) {
    return
  }
  if (currentAppId.value) {
    await continueChat()
  } else {
    await startGeneration()
  }
}

const handleDeploy = async () => {
  if (!currentAppId.value) return
  deploying.value = true
  try {
    const res = await deployMyApp(currentAppId.value)
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      message.success('部署成功')
    } else {
      message.error(res.data.message || '部署失败')
    }
  } finally {
    deploying.value = false
  }
}
const reloadPreview = () => {
  previewKey.value += 1
}
const openPreview = () => {
  if (previewUrl.value) window.open(previewUrl.value, '_blank')
}

onMounted(async () => {
  if (!userStore.loaded) await userStore.fetchLoginUser().catch(() => userStore.setLoginUser(null))
  const routeId = route.params.id
  if (typeof routeId === 'string' && routeId) {
    await loadExistingApp(routeId)
    return
  }
  const storedPrompt = sessionStorage.getItem(INITIAL_PROMPT_STORAGE_KEY) || ''
  initialPrompt.value = storedPrompt
  draftPrompt.value = storedPrompt
  if (storedPrompt) await startGeneration()
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 72px);
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(390px, 32vw) minmax(0, 1fr);
  background: #f7faff;
  color: #111827;
  overflow: hidden;
}
.chat-sidebar {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 28px 22px;
  border-right: 1px solid #e5eaf3;
  background: rgba(255, 255, 255, 0.95);
}
.app-title-row,
.preview-header,
.preview-actions,
.chat-actions,
.message-row {
  display: flex;
  align-items: center;
}
.app-title-row,
.preview-header,
.chat-actions {
  justify-content: space-between;
  gap: 16px;
}
.back-link {
  color: #536174;
  font-weight: 700;
  text-decoration: none;
}
.chat-sidebar h1,
.preview-header h2 {
  margin: 0;
  font-weight: 900;
  letter-spacing: 0;
}
.chat-sidebar h1 {
  font-size: 26px;
}
.app-subtitle,
.muted-text,
.chat-actions span,
.message-state {
  color: #667085;
}
.app-subtitle {
  margin: -8px 0 0;
  line-height: 1.7;
}
.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-right: 4px;
}
.message-state {
  margin: auto;
  text-align: center;
  line-height: 1.7;
}
.message-row {
  gap: 10px;
}
.user-row {
  justify-content: flex-end;
}
.ai-row {
  justify-content: flex-start;
  align-items: flex-start;
}
.message-bubble {
  max-width: min(560px, calc(100% - 56px));
  padding: 14px 16px;
  border-radius: 16px;
  line-height: 1.8;
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.06);
}
.user-bubble {
  color: #fff;
  background: #1687f7;
  border-top-right-radius: 6px;
}
.ai-bubble {
  color: #111827;
  background: #f3f5f8;
  border-top-left-radius: 6px;
}
.message-role {
  margin-bottom: 8px;
  color: #2f7cff;
  font-size: 13px;
  font-weight: 900;
}
.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.step-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.step-card {
  padding: 12px;
  border: 1px solid #dce5f2;
  border-radius: 10px;
  background: #fff;
}
.step-card-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.step-card-header span {
  padding: 2px 8px;
  border-radius: 999px;
  color: #1769e0;
  background: #eaf2ff;
  font-size: 12px;
  font-weight: 900;
}
.step-card-header strong {
  color: #111827;
  font-size: 14px;
}
.chat-input-box {
  padding: 14px;
  border: 1px solid #e5eaf3;
  border-radius: 18px;
  background: #fff;
}
.chat-input-box :deep(textarea.ant-input) {
  resize: none;
  border: none;
  box-shadow: none;
  background: transparent;
}
.chat-actions {
  margin-top: 12px;
}
.preview-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}
.section-label {
  display: inline-block;
  margin-bottom: 6px;
  color: #2f7cff;
  font-size: 13px;
  font-weight: 900;
}
.preview-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}
.preview-frame-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dce4f0;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 22px 60px rgba(17, 58, 113, 0.08);
}
.preview-frame-wrap iframe {
  width: 100%;
  height: 100%;
  border: 0;
  background: #fff;
}
.preview-empty {
  height: 100%;
  display: grid;
  place-content: center;
  text-align: center;
  color: #667085;
}
.preview-empty h3 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 22px;
}
.deploy-alert {
  border-radius: 12px;
}
@media (max-width: 980px) {
  .chat-page {
    height: auto;
    min-height: calc(100vh - 72px);
    grid-template-columns: 1fr;
    overflow: visible;
  }
  .chat-sidebar {
    min-height: 520px;
    border-right: none;
    border-bottom: 1px solid #e5eaf3;
  }
  .preview-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .preview-actions {
    justify-content: flex-start;
  }
}
</style>
