<template>
  <main class="my-apps-page">
    <section class="page-hero">
      <div>
        <span class="section-label">我的应用</span>
        <h1>管理你生成过的 Web 应用</h1>
        <p>这里展示当前账号创建的全部应用，可以搜索、分页查看，并继续进入对话和预览。</p>
      </div>
      <a-button type="primary" shape="round" @click="goHomeCreate">创建新应用</a-button>
    </section>

    <section class="apps-panel">
      <div class="toolbar">
        <a-input-search
          v-model:value="searchText"
          class="search-input"
          placeholder="按应用名称搜索"
          enter-button="搜索"
          allow-clear
          @search="handleSearch"
        />
        <a-button shape="round" :loading="loading" @click="fetchApps">刷新</a-button>
      </div>

      <div v-if="apps.length" class="apps-grid">
        <article v-for="app in apps" :key="app.id" class="app-card">
          <div class="app-cover" :class="getCoverClass(app)">
            <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
            <div v-else class="cover-content">
              <span>{{ app.codeGenType || 'Web App' }}</span>
              <strong>{{ getAppInitial(app) }}</strong>
            </div>
          </div>
          <div class="app-body">
            <div class="app-title-row">
              <h2>{{ app.appName || '未命名应用' }}</h2>
              <a-tag :color="app.deployKey ? 'success' : 'default'">
                {{ app.deployKey ? '已部署' : '未部署' }}
              </a-tag>
            </div>
            <p>{{ app.initPrompt || '暂无初始需求描述' }}</p>
            <div class="app-meta">
              <span>创建于 {{ formatDate(app.createTime) }}</span>
              <span>ID {{ app.id }}</span>
            </div>
            <div class="card-actions">
              <a-button type="primary" shape="round" @click="goAppChat(app)">查看对话</a-button>
            </div>
          </div>
        </article>
      </div>

      <a-empty v-else-if="!loading" class="empty-state" description="暂无应用" />

      <div class="pagination-row">
        <a-pagination
          v-model:current="pageNumber"
          v-model:page-size="pageSize"
          :total="total"
          :show-size-changer="true"
          :page-size-options="['6', '12', '20']"
          show-less-items
          @change="fetchApps"
          @show-size-change="handlePageSizeChange"
        />
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { listMyApps, type AppVO } from '@/api/services/appService'

const router = useRouter()

const apps = ref<AppVO[]>([])
const loading = ref(false)
const searchText = ref('')
const pageNumber = ref(1)
const pageSize = ref(6)
const total = ref(0)

const fetchApps = async () => {
  loading.value = true
  try {
    const res = await listMyApps({
      pageNumber: pageNumber.value,
      pageSize: pageSize.value,
      appName: searchText.value.trim() || undefined,
    })
    if (res.data.code === 0) {
      apps.value = res.data.data?.records || []
      total.value = res.data.data?.totalRow || 0
    } else {
      message.error(res.data.message || '应用列表加载失败')
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNumber.value = 1
  fetchApps()
}

const handlePageSizeChange = (_current: number, size: number) => {
  pageNumber.value = 1
  pageSize.value = size
  fetchApps()
}

const goHomeCreate = () => {
  router.push('/#prompt-box')
}

const goAppChat = (app: AppVO) => {
  if (!app.id) {
    return
  }
  router.push(`/app/${app.id}/chat`)
}

const getAppInitial = (app: AppVO) => (app.appName || '应用').slice(0, 1)

const getCoverClass = (app: AppVO) => {
  const seed = Number(app.id?.slice(-2) || 0)
  return ['cover-blue', 'cover-green', 'cover-ink', 'cover-coral'][seed % 4]
}

const formatDate = (date?: string) => {
  if (!date) {
    return '刚刚'
  }
  return date.slice(0, 10)
}

onMounted(fetchApps)
</script>

<style scoped>
.my-apps-page {
  color: #111827;
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 0 30px;
}

.section-label {
  display: inline-block;
  margin-bottom: 10px;
  color: #2f7cff;
  font-size: 14px;
  font-weight: 800;
}

.page-hero h1 {
  margin: 0;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
}

.page-hero p {
  max-width: 680px;
  margin: 14px 0 0;
  color: #667085;
  line-height: 1.8;
}

.apps-panel {
  padding: 28px;
  border: 1px solid #e5eaf3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(17, 58, 113, 0.08);
}

.toolbar,
.app-title-row,
.app-meta,
.card-actions,
.pagination-row {
  display: flex;
  align-items: center;
}

.toolbar {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.search-input {
  max-width: 420px;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.app-card {
  overflow: hidden;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 38px rgba(17, 24, 39, 0.07);
}

.app-cover {
  height: 180px;
  color: #fff;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
}

.cover-content span {
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-weight: 800;
}

.cover-content strong {
  font-size: 54px;
  line-height: 1;
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

.app-body {
  padding: 18px;
}

.app-title-row {
  justify-content: space-between;
  gap: 12px;
}

.app-title-row h2 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 18px;
  font-weight: 900;
}

.app-body p {
  height: 48px;
  display: -webkit-box;
  margin: 12px 0;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: #667085;
  line-height: 1.7;
}

.app-meta {
  justify-content: space-between;
  gap: 12px;
  color: #8a94a6;
  font-size: 12px;
}

.card-actions {
  justify-content: flex-end;
  margin-top: 18px;
}

.empty-state {
  padding: 64px 0;
}

.pagination-row {
  justify-content: flex-end;
  margin-top: 28px;
}

@media (max-width: 1100px) {
  .apps-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-hero,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .apps-panel {
    padding: 20px;
  }

  .apps-grid {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    justify-content: center;
  }
}
</style>