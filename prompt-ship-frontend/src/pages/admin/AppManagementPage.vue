<template>
  <main class="admin-page">
    <section class="admin-hero">
      <div>
        <span class="section-label">Admin Console</span>
        <h1>应用管理</h1>
        <p>查看应用列表，维护名称、封面、优先级和部署状态。</p>
      </div>
      <a-button type="primary" class="primary-button" @click="fetchApps">刷新应用</a-button>
    </section>
    <section class="summary-grid">
      <article class="summary-card">
        <span>应用总数</span><strong>{{ pagination.total }}</strong>
      </article>
      <article class="summary-card">
        <span>当前页应用</span><strong>{{ apps.length }}</strong>
      </article>
      <article class="summary-card">
        <span>已部署</span><strong>{{ deployedCount }}</strong>
      </article>
    </section>
    <section class="table-panel">
      <div class="panel-toolbar">
        <a-input-search
          v-model:value="keyword"
          class="search-input"
          placeholder="按应用名称搜索"
          allow-clear
          @search="handleSearch"
        /><a-select
          v-model:value="codeGenType"
          class="type-select"
          allow-clear
          placeholder="生成类型"
          @change="handleSearch"
          ><a-select-option value="html">html</a-select-option
          ><a-select-option value="multi_file">multi_file</a-select-option></a-select
        ><a-button @click="fetchApps">刷新</a-button>
      </div>
      <a-table
        row-key="id"
        :columns="columns"
        :data-source="apps"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'appInfo'"
            ><div class="app-cell">
              <img :src="getAppCover(record)" :alt="record.appName" />
              <div>
                <strong>{{ record.appName || '未命名应用' }}</strong
                ><span>ID {{ record.id }}</span>
              </div>
            </div></template
          >
          <template v-else-if="column.key === 'creator'"
            ><div class="creator-cell">
              <a-avatar :src="record.user?.userAvatar">{{ getAppCreatorInitial(record) }}</a-avatar
              ><span>{{ getAppCreatorName(record) }}</span>
            </div></template
          >
          <template v-else-if="column.key === 'codeGenType'"
            ><a-tag>{{ record.codeGenType || '-' }}</a-tag></template
          >
          <template v-else-if="column.key === 'deployStatus'"
            ><a-tag :color="record.deployKey ? 'success' : 'default'">{{
              record.deployKey ? '已部署' : '未部署'
            }}</a-tag></template
          >
          <template v-else-if="column.key === 'action'"
            ><a-space
              ><a-button type="link" @click="openEditModal(record)">编辑</a-button
              ><a-popconfirm
                title="确定删除该应用吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="handleRemove(record)"
                ><a-button type="link" danger>删除</a-button></a-popconfirm
              ></a-space
            ></template
          >
        </template>
      </a-table>
    </section>
    <a-modal
      v-model:open="modalOpen"
      title="编辑应用"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSave"
    >
      <a-form ref="formRef" :model="formState" layout="vertical">
        <a-form-item
          label="应用名称"
          name="appName"
          :rules="[{ required: true, message: '请输入应用名称' }]"
          ><a-input v-model:value="formState.appName" placeholder="请输入应用名称"
        /></a-form-item>
        <a-form-item label="封面 URL" name="cover"
          ><a-input v-model:value="formState.cover" placeholder="请输入封面 URL"
        /></a-form-item>
        <a-form-item label="优先级" name="priority"
          ><a-input-number
            v-model:value="formState.priority"
            class="number-input"
            :min="0"
            :max="999"
        /></a-form-item>
      </a-form>
    </a-modal>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { message, type FormInstance } from 'ant-design-vue'
import {
  DEFAULT_APP_COVER,
  deleteAdminApp,
  getAppCover,
  getAppCreatorInitial,
  getAppCreatorName,
  listAdminApps,
  updateAdminApp,
  type AppVO,
} from '@/api/services/appService'

type AppFormState = { id?: string; appName: string; cover: string; priority: number }
const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const apps = ref<AppVO[]>([])
const keyword = ref('')
const codeGenType = ref<string>()
const formRef = ref<FormInstance>()
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => '共 ' + total + ' 条',
})
const formState = reactive<AppFormState>({
  id: undefined,
  appName: '',
  cover: DEFAULT_APP_COVER,
  priority: 0,
})
const columns = [
  { title: '应用', key: 'appInfo' },
  { title: '创建人', key: 'creator', width: 190 },
  { title: '生成类型', key: 'codeGenType', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  { title: '部署', key: 'deployStatus', width: 110 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 190 },
  { title: '操作', key: 'action', width: 150 },
]
const deployedCount = computed(() => apps.value.filter((item) => item.deployKey).length)
const fetchApps = async () => {
  loading.value = true
  try {
    const res = await listAdminApps({
      pageNumber: pagination.current,
      pageSize: pagination.pageSize,
      appName: keyword.value.trim() || undefined,
      codeGenType: codeGenType.value,
    })
    if (res.data.code === 0 && res.data.data) {
      apps.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
      pagination.current = res.data.data.pageNumber || pagination.current
      pagination.pageSize = res.data.data.pageSize || pagination.pageSize
      return
    }
    message.error(res.data.message || '获取应用列表失败')
  } finally {
    loading.value = false
  }
}
const handleSearch = () => {
  pagination.current = 1
  fetchApps()
}
const handleTableChange = (page: TablePaginationConfig) => {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  fetchApps()
}
const openEditModal = (app: AppVO) => {
  Object.assign(formState, {
    id: app.id,
    appName: app.appName || '',
    cover: app.cover || DEFAULT_APP_COVER,
    priority: app.priority || 0,
  })
  formRef.value?.clearValidate()
  modalOpen.value = true
}
const handleSave = async () => {
  await formRef.value?.validate()
  if (!formState.id) {
    message.error('缺少应用 ID，无法更新')
    return
  }
  saving.value = true
  try {
    const res = await updateAdminApp({
      id: formState.id,
      appName: formState.appName,
      cover: formState.cover || DEFAULT_APP_COVER,
      priority: formState.priority,
    })
    if (res.data.code !== 0) {
      message.error(res.data.message || '更新应用失败')
      return
    }
    message.success('更新应用成功')
    modalOpen.value = false
    fetchApps()
  } finally {
    saving.value = false
  }
}
const handleRemove = async (app: AppVO) => {
  if (!app.id) return
  const res = await deleteAdminApp(app.id)
  if (res.data.code === 0) {
    message.success('删除应用成功')
    fetchApps()
    return
  }
  message.error(res.data.message || '删除应用失败')
}
fetchApps()
</script>

<style scoped>
.admin-page {
  min-height: calc(100vh - 72px);
  padding: 42px 24px 84px;
  background:
    radial-gradient(circle at 12% 12%, rgba(32, 211, 176, 0.12), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
}
.admin-hero,
.summary-grid,
.table-panel {
  width: min(1280px, 100%);
  margin: 0 auto;
}
.admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}
.section-label {
  color: #2f7cff;
  font-size: 14px;
  font-weight: 800;
}
.admin-hero h1 {
  margin: 8px 0 10px;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
}
.admin-hero p {
  margin: 0;
  color: #667085;
}
.primary-button {
  height: 44px;
  border-radius: 14px;
  font-weight: 800;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.summary-card,
.table-panel {
  border: 1px solid rgba(17, 24, 39, 0.06);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(17, 58, 113, 0.08);
}
.summary-card {
  padding: 22px;
  border-radius: 16px;
}
.summary-card span {
  display: block;
  color: #667085;
  margin-bottom: 8px;
}
.summary-card strong {
  font-size: 30px;
  font-weight: 900;
}
.table-panel {
  padding: 24px;
  border-radius: 20px;
}
.panel-toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 18px;
}
.search-input {
  max-width: 360px;
}
.type-select {
  width: 160px;
}
.app-cell,
.creator-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.app-cell img {
  width: 56px;
  height: 42px;
  border-radius: 6px;
  object-fit: cover;
}
.app-cell strong,
.app-cell span {
  display: block;
}
.app-cell span {
  color: #667085;
  font-size: 13px;
}
.number-input {
  width: 100%;
}
@media (max-width: 760px) {
  .admin-hero,
  .panel-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .summary-grid {
    grid-template-columns: 1fr;
  }
  .search-input,
  .type-select {
    max-width: none;
    width: 100%;
  }
}
</style>
