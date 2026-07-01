<template>
  <main class="chat-page">
    <aside class="chat-sidebar">
      <div class="app-title-row">
        <RouterLink to="/" class="back-link">返回首页</RouterLink>
        <a-tag :color="generationStatus.color">{{ generationStatus.text }}</a-tag>
      </div>

      <h1>{{ appTitle }}</h1>
      <p class="app-subtitle">用自然语言描述需求，AI 会为你生成可预览的 Web 应用。</p>

      <div class="message-list">
        <article class="message-card user-message">
          <div class="message-role">你</div>
          <p>{{ initialPrompt || '还没有输入应用需求' }}</p>
        </article>

        <article class="message-card ai-message">
          <div class="message-role">PromptShip AI</div>
          <p v-if="aiOutput">{{ aiOutput }}</p>
          <p v-else class="muted-text">{{ emptyAiText }}</p>
        </article>
      </div>

      <div class="chat-input-box">
        <a-textarea
          v-model:value="draftPrompt"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          placeholder="继续补充细节的能力将在后续接口支持后开放"
          disabled
        />
        <div class="chat-actions">
          <span>{{ helperText }}</span>
          <a-button type="primary" shape="round" :loading="generating" :disabled="!canStart" @click="startGeneration">
            {{ startButtonText }}
          </a-button>
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
          <a-button type="primary" shape="round" :loading="deploying" :disabled="!currentAppId || generating" @click="handleDeploy">
            部署
          </a-button>
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
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  chatToGenerateApp,
  deployMyApp,
  getAppDetail,
  getGeneratedPreviewUrl,
  INITIAL_PROMPT_STORAGE_KEY,
  type AppId,
  type AppVO,
} from '@/api/services/appService'

const route = useRoute()
const router = useRouter()

const currentAppId = ref<AppId>()
const appDetail = ref<AppVO>()
const initialPrompt = ref('')
const draftPrompt = ref('')
const aiOutput = ref('')
const generating = ref(false)
const generated = ref(false)
const deploying = ref(false)
const deployUrl = ref('')
const previewKey = ref(0)

const appTitle = computed(() => appDetail.value?.appName || initialPrompt.value.slice(0, 18) || '新应用')
const previewUrl = computed(() => getGeneratedPreviewUrl(currentAppId.value, appDetail.value?.codeGenType))
const previewTitle = computed(() => (currentAppId.value ? `App #${currentAppId.value}` : '生成中的应用'))
const canStart = computed(() => Boolean(initialPrompt.value.trim()) && !generating.value && !generated.value)
const emptyAiText = computed(() => (generating.value ? 'AI 正在理解需求并生成代码...' : '提交需求后，这里会显示 AI 的生成过程。'))
const helperText = computed(() => (currentAppId.value ? `当前应用 ID：${currentAppId.value}` : '即将创建新应用'))
const startButtonText = computed(() => (generated.value ? '已生成' : '开始生成'))
const generationStatus = computed(() => {
  if (generating.value) {
    return { color: 'processing', text: '生成中' }
  }
  if (generated.value) {
    return { color: 'success', text: '已完成' }
  }
  return { color: 'default', text: '待生成' }
})

const loadExistingApp = async (id: AppId) => {
  const res = await getAppDetail(id)
  if (res.data.code === 0 && res.data.data) {
    appDetail.value = res.data.data
    initialPrompt.value = res.data.data.initPrompt || ''
    draftPrompt.value = initialPrompt.value
    currentAppId.value = id
    generated.value = true
    return
  }
  message.error(res.data.message || '应用加载失败')
}

const startGeneration = async () => {
  if (!initialPrompt.value.trim()) {
    message.warning('请先输入你想生成的应用需求')
    return
  }

  generating.value = true
  aiOutput.value = ''
  deployUrl.value = ''

  try {
    await chatToGenerateApp(
      {
        initPrompt: initialPrompt.value.trim(),
      },
      {
        onAppId: (appId) => {
          currentAppId.value = appId
          // 后端会在 SSE 首条消息返回 appId，这时把临时地址替换成真实应用地址。
          router.replace(`/app/${appId}/chat`)
        },
        onChunk: (chunk) => {
          aiOutput.value += chunk
        },
        onDone: () => {
          generated.value = true
        },
      },
    )
    generated.value = true
    message.success('应用生成完成')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '生成失败，请稍后重试')
  } finally {
    generating.value = false
    sessionStorage.removeItem(INITIAL_PROMPT_STORAGE_KEY)
    if (currentAppId.value) {
      await loadExistingApp(currentAppId.value).catch(() => undefined)
      previewKey.value += 1
    }
  }
}

const handleDeploy = async () => {
  if (!currentAppId.value) {
    return
  }

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
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

onMounted(async () => {
  const routeId = route.params.id
  if (typeof routeId === 'string' && routeId) {
    await loadExistingApp(routeId)
    return
  }

  const storedPrompt = sessionStorage.getItem(INITIAL_PROMPT_STORAGE_KEY) || ''
  initialPrompt.value = storedPrompt
  draftPrompt.value = storedPrompt

  if (storedPrompt) {
    await startGeneration()
  }
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 72px);
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(360px, 28vw) minmax(0, 1fr);
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
  background: rgba(255, 255, 255, 0.92);
}

.app-title-row,
.preview-header,
.preview-actions,
.chat-actions {
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
.chat-actions span {
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
  gap: 14px;
  padding-right: 4px;
}

.message-card {
  padding: 16px;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(17, 24, 39, 0.05);
}

.ai-message {
  background: #f8fbff;
}

.message-role {
  margin-bottom: 8px;
  color: #2f7cff;
  font-size: 13px;
  font-weight: 900;
}

.message-card p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.8;
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
