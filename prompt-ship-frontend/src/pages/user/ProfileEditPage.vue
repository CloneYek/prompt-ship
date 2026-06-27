<template>
  <main class="profile-page">
    <section class="profile-hero">
      <div class="hero-avatar-wrap">
        <a-avatar :src="formState.userAvatar" :size="118">{{ userInitial }}</a-avatar>
      </div>
      <h1>{{ previewName }}</h1>
      <p>{{ formState.userProfile || '介绍一下自己，让团队更快认识你。' }}</p>
    </section>

    <section class="profile-panel">
      <div class="panel-title">
        <span class="active-tab">个人编辑</span>
      </div>

      <a-form class="profile-form" :model="formState" layout="vertical" @finish="handleSubmit">
        <a-form-item label="账号">
          <a-input :value="userStore.loginUser?.userAccount" disabled />
        </a-form-item>

        <a-form-item label="昵称" name="userName">
          <a-input v-model:value="formState.userName" placeholder="请输入昵称" />
        </a-form-item>

        <a-form-item label="头像地址" name="userAvatar">
          <a-input v-model:value="formState.userAvatar" placeholder="请输入头像图片地址" />
        </a-form-item>

        <a-form-item label="个人简介" name="userProfile">
          <a-textarea
            v-model:value="formState.userProfile"
            :rows="4"
            placeholder="写一句简短的个人介绍"
          />
        </a-form-item>

        <div class="form-actions">
          <a-button @click="resetForm">重置</a-button>
          <a-button type="primary" html-type="submit" :loading="saving">保存修改</a-button>
        </div>
      </a-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { updateMyUser } from '@/api/userController'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const saving = ref(false)

const formState = reactive<API.UserUpdateMyRequest>({
  userName: userStore.loginUser?.userName || '',
  userAvatar: userStore.loginUser?.userAvatar || '',
  userProfile: userStore.loginUser?.userProfile || '',
})

const previewName = computed(
  () => formState.userName || userStore.loginUser?.userAccount || '未命名用户',
)
const userInitial = computed(() => previewName.value.slice(0, 1).toUpperCase())

const resetForm = () => {
  Object.assign(formState, {
    userName: userStore.loginUser?.userName || '',
    userAvatar: userStore.loginUser?.userAvatar || '',
    userProfile: userStore.loginUser?.userProfile || '',
  })
}

const handleSubmit = async () => {
  saving.value = true
  try {
    const res = await updateMyUser({
    // 个人资料接口只允许修改昵称、头像、简介。
    userName: formState.userName,
    userAvatar: formState.userAvatar,
    userProfile: formState.userProfile,
  })
  if (res.data.code === 0 && res.data.data) {
    userStore.setLoginUser(res.data.data)
    resetForm()
    message.success('个人资料已更新')
    return
  }
    message.error(res.data.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 72px);
  padding: 62px 24px 84px;
  background:
    radial-gradient(circle at 50% 8%, rgba(255, 186, 73, 0.18), transparent 18%),
    linear-gradient(180deg, #f7fbff 0%, #eef6ff 48%, #f8fbff 100%);
}

.profile-hero {
  width: min(980px, 100%);
  margin: 0 auto 48px;
  text-align: center;
}

.hero-avatar-wrap {
  width: 132px;
  height: 132px;
  display: grid;
  place-items: center;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff7a7a, #ffd86b);
  box-shadow: 0 24px 54px rgba(255, 122, 122, 0.24);
}

.profile-hero h1 {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0;
}

.profile-hero p {
  max-width: 560px;
  margin: 0 auto;
  color: #667085;
  line-height: 1.8;
}

.profile-panel {
  width: min(1160px, 100%);
  margin: 0 auto;
  padding: 34px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 42px rgba(17, 58, 113, 0.08);
}

.panel-title {
  display: flex;
  justify-content: center;
  margin-bottom: 34px;
}

.active-tab {
  position: relative;
  color: #111827;
  font-size: 17px;
  font-weight: 800;
}

.active-tab::after {
  position: absolute;
  left: 50%;
  bottom: -12px;
  width: 42px;
  height: 4px;
  content: '';
  border-radius: 999px;
  background: #20a6b8;
  transform: translateX(-50%);
}

.profile-form {
  width: min(640px, 100%);
  margin: 0 auto;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.form-actions :deep(.ant-btn) {
  height: 42px;
  border-radius: 12px;
  font-weight: 700;
}

@media (max-width: 700px) {
  .profile-page {
    padding-top: 42px;
  }

  .profile-panel {
    padding: 26px 18px;
  }

  .form-actions {
    flex-direction: column-reverse;
  }
}
</style>

