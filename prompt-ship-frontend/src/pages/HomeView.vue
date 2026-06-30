<template>
  <main class="home-page">
    <section class="hero-section">
      <div class="hero-inner">
        <div class="hero-kicker">PromptShip</div>
        <h1>一句话生成你的 Web 应用</h1>
        <p class="hero-subtitle">描述你想要的页面、功能和风格，PromptShip 会帮你创建应用并实时生成代码。</p>

        <div id="prompt-box" class="prompt-box">
          <a-textarea
            v-model:value="promptText"
            :auto-size="{ minRows: 4, maxRows: 5 }"
            placeholder="例如：帮我创建一个活动宣传页，包含主视觉、报名按钮、活动日程和常见问题"
            @press-enter="handleEnter"
          />
          <div class="prompt-actions">
            <div class="action-buttons">
              <a-button shape="round" disabled>上传</a-button>
              <a-button shape="round" disabled>优化</a-button>
            </div>
            <a-button class="send-button" type="primary" shape="circle" :loading="submitting" @click="handleCreateApp">↑</a-button>
          </div>
        </div>

        <div class="quick-prompts">
          <button v-for="item in quickPrompts" :key="item" type="button" @click="useQuickPrompt(item)">
            {{ item }}
          </button>
        </div>
      </div>
    </section>

    <section id="my-apps" class="works-section">
      <div class="section-panel works-panel">
        <div class="section-heading">
          <div>
            <span class="section-label">我的作品</span>
            <h2>最近创建的应用</h2>
          </div>
          <div v-if="userStore.loginUser" class="section-actions">
            <a-button shape="round" :loading="myAppsLoading" @click="fetchMyApps">刷新</a-button>
            <a-button type="primary" shape="round" @click="goMyApps">查看更多</a-button>
          </div>
        </div>

        <div v-if="!userStore.loginUser" class="empty-panel">
          <h3>登录后查看你的作品集</h3>
          <p>你创建的应用会出现在这里，方便继续查看对话和预览效果。</p>
          <a-button type="primary" shape="round" @click="goLogin">去登录</a-button>
        </div>

        <div v-else-if="myApps.length" class="work-grid">
          <article v-for="app in myApps" :key="app.id" class="work-card">
            <div class="work-cover">
              <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
              <div v-else class="cover-placeholder">{{ getAppInitial(app) }}</div>
              <a-button shape="round" @click="goAppChat(app)">查看对话</a-button>
            </div>
            <div class="work-body">
              <h3>{{ app.appName || '未命名应用' }}</h3>
              <p>创建于 {{ formatDate(app.createTime) }}</p>
            </div>
          </article>
        </div>

        <div v-else class="empty-panel compact-empty">
          <h3>还没有作品</h3>
          <p>从上方输入一句需求，创建你的第一个 Web 应用。</p>
        </div>
      </div>
    </section>

    <section id="cases" class="cases-section">
      <div class="section-panel cases-panel">
        <div class="cases-header">
          <div>
            <span class="section-label">案例广场</span>
            <h2>看看别人生成了什么</h2>
          </div>
          <a-button shape="round" :loading="goodAppsLoading" @click="fetchGoodApps">全部案例</a-button>
        </div>

        <div class="case-toolbar">
          <a-select v-model:value="sortValue" class="sort-select" :bordered="false">
            <a-select-option value="default">默认排序</a-select-option>
            <a-select-option value="newest">最新发布</a-select-option>
          </a-select>
          <div class="category-tabs">
            <button
              v-for="category in categories"
              :key="category"
              :class="{ active: activeCategory === category }"
              type="button"
              @click="activeCategory = category"
            >
              {{ category }}
            </button>
          </div>
        </div>

        <div v-if="displayCases.length" class="case-grid">
          <article v-for="item in displayCases" :key="item.id || item.appName" class="case-card" @click="goAppChat(item)">
            <div class="case-cover" :class="getCoverClass(item)">
              <img v-if="item.cover" :src="item.cover" :alt="item.appName" />
              <span v-else>{{ item.codeGenType || 'Web App' }}</span>
            </div>
            <div class="case-body">
              <h3>{{ item.appName || '精选应用' }}</h3>
              <p>{{ item.initPrompt || '一个由自然语言生成的 Web 应用。' }}</p>
            </div>
          </article>
        </div>

        <div v-else class="empty-panel compact-empty">
          <h3>暂无精选案例</h3>
          <p>后续管理员设置精选应用后，这里会自动展示案例。</p>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { INITIAL_PROMPT_STORAGE_KEY, listGoodApps, listMyApps, type AppVO } from '@/api/services/appService'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const promptText = ref('')
const submitting = ref(false)
const sortValue = ref('default')
const activeCategory = ref('全部')
const myApps = ref<AppVO[]>([])
const goodApps = ref<AppVO[]>([])
const myAppsLoading = ref(false)
const goodAppsLoading = ref(false)

const quickPrompts = ['活动宣传页', '企业官网', '电商运营后台', '个人博客网站']
const categories = ['全部', '工具', '网站', '数据分析', '活动页面', '管理平台', '用户应用', '个人管理', '游戏']
const fallbackCases: AppVO[] = [
  {
    id: 'demo-0',
    appName: 'AI Coding 校园俱乐部',
    initPrompt: '创建一个校园社团官网，包含活动介绍、报名入口和成员展示。',
    codeGenType: '网站',
  },
  {
    id: 'demo-1',
    appName: '活动挑战赛官网',
    initPrompt: '生成一个挑战赛活动页面，包含奖项、规则、日程和报名按钮。',
    codeGenType: '活动页面',
  },
  {
    id: 'demo-2',
    appName: '咖啡工作室主页',
    initPrompt: '做一个咖啡工作室官网，风格温暖，展示菜单、环境和预约方式。',
    codeGenType: '网站',
  },
]

const displayCases = computed(() => {
  const source = goodApps.value.length ? goodApps.value : fallbackCases
  if (activeCategory.value === '全部') {
    return source
  }
  return source.filter((item) => item.codeGenType === activeCategory.value)
})

const handleEnter = (event: KeyboardEvent) => {
  if (!event.shiftKey) {
    event.preventDefault()
    handleCreateApp()
  }
}

const handleCreateApp = async () => {
  const prompt = promptText.value.trim()
  if (!prompt) {
    message.warning('请先描述你想生成的应用')
    return
  }

  if (!userStore.loginUser) {
    message.warning('请先登录后再创建应用')
    router.push({ path: '/user/login', query: { redirect: '/' } })
    return
  }

  submitting.value = true
  sessionStorage.setItem(INITIAL_PROMPT_STORAGE_KEY, prompt)
  await router.push('/app/chat/new')
  submitting.value = false
}

const useQuickPrompt = (item: string) => {
  promptText.value = `帮我创建一个${item}，页面要美观、结构清晰，并包含关键操作入口。`
}

const fetchMyApps = async () => {
  if (!userStore.loginUser) {
    return
  }

  myAppsLoading.value = true
  try {
    const res = await listMyApps({ pageNumber: 1, pageSize: 4 })
    if (res.data.code === 0) {
      myApps.value = res.data.data?.records || []
    }
  } finally {
    myAppsLoading.value = false
  }
}

const fetchGoodApps = async () => {
  goodAppsLoading.value = true
  try {
    const res = await listGoodApps({ pageNumber: 1, pageSize: 8 })
    if (res.data.code === 0) {
      goodApps.value = res.data.data?.records || []
    }
  } finally {
    goodAppsLoading.value = false
  }
}

const goLogin = () => {
  router.push('/user/login')
}

const goMyApps = () => {
  router.push('/app/my')
}

const goAppChat = (app: AppVO) => {
  if (app.id) {
    router.push(`/app/${app.id}/chat`)
  }
}

const getAppInitial = (app: AppVO) => (app.appName || '应用').slice(0, 1)

const getCoverClass = (item: AppVO) => {
  const seed = Number(item.id?.slice(-2) || 0)
  return ['cover-blue', 'cover-green', 'cover-ink', 'cover-coral'][seed % 4]
}

const formatDate = (date?: string) => {
  if (!date) {
    return '刚刚'
  }
  return date.slice(0, 10)
}

onMounted(async () => {
  if (!userStore.loaded) {
    await userStore.fetchLoginUser().catch(() => {
      userStore.setLoginUser(null)
    })
  }
  await Promise.all([fetchGoodApps(), fetchMyApps()])
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  color: #111827;
  background:
    radial-gradient(circle at 18% 34%, rgba(32, 211, 176, 0.2), transparent 32%),
    radial-gradient(circle at 82% 58%, rgba(47, 124, 255, 0.24), transparent 34%),
    linear-gradient(180deg, #fbfdfb 0%, #e9fbfb 42%, #dbeafe 100%);
}

.hero-section {
  min-height: 690px;
  display: flex;
  align-items: center;
  padding: 72px 24px 128px;
}

.hero-inner {
  width: min(980px, 100%);
  margin: 0 auto;
  text-align: center;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 22px;
  border-radius: 18px;
  background: #20d3b0;
  color: #07111f;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 18px 42px rgba(32, 211, 176, 0.22);
}

.hero-inner h1 {
  max-width: 820px;
  margin: 0 auto;
  font-size: 48px;
  line-height: 1.2;
  font-weight: 900;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 660px;
  margin: 22px auto 40px;
  color: #536174;
  font-size: 17px;
  line-height: 1.8;
}

.prompt-box {
  max-width: 880px;
  margin: 0 auto;
  padding: 18px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 26px 70px rgba(17, 58, 113, 0.13);
  text-align: left;
}

.prompt-box :deep(textarea.ant-input) {
  resize: none;
  border: none;
  box-shadow: none;
  color: #111827;
  background: transparent;
  font-size: 18px;
}

.prompt-actions,
.section-heading,
.section-actions,
.cases-header,
.case-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.prompt-actions {
  gap: 16px;
  margin-top: 14px;
}

.action-buttons,
.section-actions,
.quick-prompts,
.category-tabs,
.work-grid,
.case-grid {
  display: flex;
}

.action-buttons,
.section-actions {
  gap: 10px;
}

.send-button {
  width: 42px;
  height: 42px;
  font-size: 20px;
  font-weight: 900;
  background: #111827;
}

.quick-prompts {
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.quick-prompts button,
.category-tabs button {
  border: none;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.quick-prompts button {
  padding: 9px 18px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  color: #536174;
  font-weight: 600;
}

.quick-prompts button:hover,
.case-card:hover,
.work-card:hover {
  transform: translateY(-2px);
}

.works-section,
.cases-section {
  padding: 0 24px;
}

.cases-section {
  padding-bottom: 84px;
}

.section-panel {
  width: min(1680px, 100%);
  margin: 0 auto;
  padding: 52px;
  background: #fff;
  box-shadow: 0 -20px 60px rgba(17, 58, 113, 0.08);
}

.works-panel {
  margin-top: -64px;
  border-radius: 26px 26px 0 0;
}

.cases-panel {
  border-radius: 0;
}

.section-label {
  display: inline-block;
  margin-bottom: 10px;
  color: #2f7cff;
  font-size: 14px;
  font-weight: 800;
}

.section-heading h2,
.cases-header h2 {
  margin: 0;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0;
}

.work-grid {
  flex-wrap: wrap;
  gap: 22px;
  margin-top: 24px;
}

.work-card {
  width: 330px;
  transition: transform 0.2s ease;
}

.work-cover {
  position: relative;
  height: 188px;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid #e5eaf3;
  background: linear-gradient(135deg, #dbeafe, #f8fbff);
}

.work-cover img,
.case-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  height: 100%;
  display: grid;
  place-items: center;
  color: #2f7cff;
  font-size: 48px;
  font-weight: 900;
}

.work-cover .ant-btn {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  height: 40px;
  background: rgba(255, 255, 255, 0.92);
}

.work-body {
  padding: 14px 8px 0;
}

.work-body h3,
.case-body h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0;
}

.work-body p,
.case-body p,
.empty-panel p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.empty-panel {
  margin-top: 24px;
  padding: 32px;
  border: 1px dashed #cfd8e6;
  border-radius: 18px;
  background: #f8fbff;
  text-align: center;
}

.empty-panel h3 {
  margin: 0 0 8px;
  font-size: 20px;
}

.empty-panel .ant-btn {
  margin-top: 18px;
}

.compact-empty {
  padding: 24px;
}

.case-toolbar {
  margin: 32px 0 24px;
}

.sort-select {
  min-width: 132px;
  height: 42px;
  border-radius: 999px;
  background: #f6f8fb;
}

.category-tabs {
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.category-tabs button {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  background: #f6f8fb;
  color: #667085;
  font-weight: 700;
}

.category-tabs button.active {
  background: #111827;
  color: #fff;
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
}

.case-card {
  overflow: hidden;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(17, 24, 39, 0.07);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.case-cover {
  height: 170px;
  display: flex;
  align-items: flex-end;
  padding: 18px;
  color: #fff;
  font-weight: 900;
}

.case-cover span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
}

.cover-blue {
  background: linear-gradient(135deg, #111827, #2f7cff);
}

.cover-green {
  background: linear-gradient(135deg, #0f766e, #84cc16);
}

.cover-ink {
  background: linear-gradient(135deg, #111827, #64748b);
}

.cover-coral {
  background: linear-gradient(135deg, #f97316, #db2777);
}

.case-body {
  padding: 20px;
}

.case-body p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 1100px) {
  .case-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-section {
    min-height: 640px;
    padding-top: 54px;
  }

  .hero-inner h1 {
    font-size: 34px;
  }

  .prompt-box {
    border-radius: 20px;
  }

  .prompt-actions,
  .section-heading,
  .cases-header,
  .case-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .category-tabs {
    justify-content: flex-start;
  }

  .section-panel {
    padding: 32px 20px;
  }

  .case-grid {
    grid-template-columns: 1fr;
  }

  .work-card {
    width: 100%;
  }
}
</style>