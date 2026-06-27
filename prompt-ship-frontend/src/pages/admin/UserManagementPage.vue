<template>
  <main class="admin-page">
    <section class="admin-hero">
      <div>
        <span class="section-label">Admin Console</span>
        <h1>用户管理</h1>
        <p>查看用户列表，维护昵称、头像、简介和角色。</p>
      </div>
      <a-button type="primary" class="primary-button" @click="openCreateModal">新增用户</a-button>
    </section>

    <section class="summary-grid">
      <article class="summary-card">
        <span>用户总数</span>
        <strong>{{ pagination.total }}</strong>
      </article>
      <article class="summary-card">
        <span>当前页用户</span>
        <strong>{{ users.length }}</strong>
      </article>
      <article class="summary-card">
        <span>管理员</span>
        <strong>{{ adminCount }}</strong>
      </article>
    </section>

    <section class="table-panel">
      <div class="panel-toolbar">
        <a-input-search
          v-model:value="keyword"
          class="search-input"
          placeholder="按账号或昵称筛选当前页"
          allow-clear
        />
        <a-button @click="fetchUsers">刷新</a-button>
      </div>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="filteredUsers"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userInfo'">
            <div class="user-cell">
              <a-avatar :src="record.userAvatar">{{ getInitial(record) }}</a-avatar>
              <div>
                <strong>{{ record.userName || record.userAccount || '未命名用户' }}</strong>
                <span>{{ record.userAccount }}</span>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'userRole'">
            <a-tag :color="record.userRole === 'admin' ? 'blue' : 'default'">
              {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'userProfile'">
            <span class="muted-text">{{ record.userProfile || '暂无简介' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm
                title="确定删除该用户吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="handleRemove(record)"
              >
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="modalOpen"
      :title="modalMode === 'create' ? '新增用户' : '编辑用户'"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSave"
    >
      <a-form ref="formRef" :model="formState" layout="vertical">
        <a-form-item
          v-if="modalMode === 'create'"
          label="账号"
          name="userAccount"
          :rules="[
            { required: true, message: '请输入账号' },
            { min: 4, max: 32, message: '账号长度需在 4-32 位之间' },
          ]"
        >
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
        </a-form-item>

        <a-form-item
          v-if="modalMode === 'create'"
          label="密码"
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, max: 32, message: '密码长度需在 8-32 位之间' },
          ]"
        >
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
        </a-form-item>

        <a-form-item
          label="昵称"
          name="userName"
          :rules="[{ required: modalMode === 'create', message: '请输入昵称' }]"
        >
          <a-input v-model:value="formState.userName" placeholder="请输入昵称" />
        </a-form-item>

        <a-form-item label="头像地址" name="userAvatar">
          <a-input v-model:value="formState.userAvatar" placeholder="请输入头像图片地址" />
        </a-form-item>

        <a-form-item label="简介" name="userProfile">
          <a-textarea v-model:value="formState.userProfile" :rows="3" placeholder="请输入用户简介" />
        </a-form-item>

        <a-form-item label="角色" name="userRole">
          <a-select v-model:value="formState.userRole">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { message, type FormInstance } from 'ant-design-vue'
import { createUser, listUsers, removeUser, updateUser } from '@/api/userController'

type ModalMode = 'create' | 'edit'

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const modalMode = ref<ModalMode>('create')
const users = ref<API.LoginUserVO[]>([])
const keyword = ref('')
const formRef = ref<FormInstance>()

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`,
})

const formState = reactive<API.UserCreateRequest & API.UserUpdateRequest>({
  id: undefined,
  userAccount: '',
  userPassword: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const columns = [
  { title: '用户', key: 'userInfo' },
  { title: '角色', key: 'userRole', width: 120 },
  { title: '简介', key: 'userProfile' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 190 },
  { title: '操作', key: 'action', width: 150 },
]

const filteredUsers = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) {
    return users.value
  }
  return users.value.filter((item) =>
    [item.userAccount, item.userName].some((text) => text?.toLowerCase().includes(value)),
  )
})

const adminCount = computed(() => users.value.filter((item) => item.userRole === 'admin').length)

const getInitial = (user: API.LoginUserVO) => {
  const name = user.userName || user.userAccount || 'U'
  return name.slice(0, 1).toUpperCase()
}

const resetForm = () => {
  Object.assign(formState, {
    id: undefined,
    userAccount: '',
    userPassword: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
  })
  formRef.value?.clearValidate()
}

const fetchUsers = async () => {
  loading.value = true
  try {
    // 后端接收 MyBatis-Flex Page 参数，字段名使用 pageNumber/pageSize。
    const res = await listUsers({
      pageNumber: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      users.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
      pagination.current = res.data.data.pageNumber || pagination.current
      pagination.pageSize = res.data.data.pageSize || pagination.pageSize
      return
    }
    message.error(res.data.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (page: TablePaginationConfig) => {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  fetchUsers()
}

const openCreateModal = () => {
  resetForm()
  modalMode.value = 'create'
  modalOpen.value = true
}

const openEditModal = (user: API.LoginUserVO) => {
  resetForm()
  modalMode.value = 'edit'
  Object.assign(formState, {
    id: user.id,
    userName: user.userName || '',
    userAvatar: user.userAvatar || '',
    userProfile: user.userProfile || '',
    userRole: user.userRole || 'user',
  })
  modalOpen.value = true
}

const handleSave = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (modalMode.value === 'create') {
      const res = await createUser({
        userAccount: formState.userAccount,
        userPassword: formState.userPassword,
        userName: formState.userName,
        userRole: formState.userRole,
      })
      if (res.data.code !== 0) {
        message.error(res.data.message || '创建用户失败')
        return
      }
      message.success('创建用户成功')
    } else {
      const res = await updateUser({
        id: formState.id,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
        userRole: formState.userRole,
      })
      if (res.data.code !== 0) {
        message.error(res.data.message || '更新用户失败')
        return
      }
      message.success('更新用户成功')
    }
    modalOpen.value = false
    fetchUsers()
  } finally {
    saving.value = false
  }
}

const handleRemove = async (user: API.LoginUserVO) => {
  if (!user.id) {
    return
  }
  const res = await removeUser(user.id)
  if (res.data.code === 0) {
    message.success('删除用户成功')
    fetchUsers()
    return
  }
  message.error(res.data.message || '删除用户失败')
}

fetchUsers()
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

.admin-hero p,
.muted-text {
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
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.search-input {
  max-width: 360px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-cell strong,
.user-cell span {
  display: block;
}

.user-cell span {
  color: #667085;
  font-size: 13px;
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

  .search-input {
    max-width: none;
  }
}
</style>
