<template>
  <div class="auth-page">
    <section class="auth-card">
      <div class="auth-visual">
        <RouterLink to="/" class="auth-brand">
          <span class="brand-icon">P</span>
          <span>PromptShip</span>
        </RouterLink>
        <h2>让好提示词<br />更快启航</h2>
        <p>整理、复用、协作，一处管理你的 AI 工作流。</p>
        <div class="board-illustration" aria-hidden="true">
          <div class="board-panel">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div class="person-shape"></div>
          <div class="plant-shape"></div>
        </div>
      </div>

      <div class="auth-form-wrap">
        <div class="form-header">
          <h1>登录 PromptShip</h1>
          <p>继续管理你的提示词资产</p>
        </div>

        <a-form :model="formState" layout="vertical" @finish="handleSubmit">
          <a-form-item
            label="账号"
            name="userAccount"
            :rules="[
              { required: true, message: '请输入账号' },
              { min: 4, max: 32, message: '账号长度需在 4-32 位之间' },
            ]"
          >
            <a-input v-model:value="formState.userAccount" size="large" placeholder="请输入账号" />
          </a-form-item>

          <a-form-item
            label="密码"
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, max: 32, message: '密码长度需在 8-32 位之间' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              size="large"
              placeholder="请输入密码"
            />
          </a-form-item>

          <div class="form-tools">
            <a-checkbox v-model:checked="rememberMe">记住我</a-checkbox>
            <span>忘记密码？</span>
          </div>

          <a-button class="submit-button" type="primary" html-type="submit" :loading="submitting">
            登录
          </a-button>
        </a-form>

        <p class="switch-tip">
          还没有账号？
          <RouterLink to="/user/register">立即注册</RouterLink>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { userLogin } from '@/api/userController'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const submitting = ref(false)
const rememberMe = ref(true)

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await userLogin(formState)
    if (res.data.code === 0 && res.data.data) {
      userStore.setLoginUser(res.data.data)
      message.success('登录成功')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      window.location.href = redirect
      return
    }
    message.error(res.data.message || '登录失败，请检查账号或密码')
  } catch (error) {
    message.error('登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 42px;
  background: linear-gradient(135deg, #f7fbff 0%, #eef6ff 48%, #f9fbff 100%);
}

.auth-card {
  min-height: calc(100vh - 84px);
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(360px, 1fr);
  overflow: hidden;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 28px 80px rgba(26, 71, 133, 0.08);
}

.auth-visual {
  position: relative;
  padding: 68px 64px;
  background: #3b86ff;
  color: #fff;
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  color: #fff;
  font-size: 26px;
  font-weight: 800;
  text-decoration: none;
}

.brand-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: #fff;
  color: #3b86ff;
}

.auth-visual h2 {
  margin: 54px 0 18px;
  font-size: 42px;
  line-height: 1.35;
  font-weight: 800;
  letter-spacing: 0;
}

.auth-visual p {
  max-width: 380px;
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
}

.board-illustration {
  position: absolute;
  left: 72px;
  right: 72px;
  bottom: 72px;
  height: 300px;
}

.board-panel {
  position: absolute;
  left: 42px;
  right: 70px;
  top: 40px;
  height: 178px;
  border: 6px solid #eaf3ff;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 22px 18px 0 rgba(255, 255, 255, 0.22);
}

.board-panel::before,
.board-panel::after {
  position: absolute;
  content: '';
  top: 0;
  bottom: 0;
  width: 3px;
  background: #3b86ff;
}

.board-panel::before {
  left: 34%;
}

.board-panel::after {
  left: 67%;
}

.board-panel span {
  position: absolute;
  width: 72px;
  height: 52px;
  border-radius: 4px;
  box-shadow: 0 12px 18px rgba(28, 65, 126, 0.12);
}

.board-panel span:nth-child(1) {
  left: 36px;
  top: 54px;
  background: #174d98;
}

.board-panel span:nth-child(2) {
  left: 146px;
  top: 68px;
  background: #f8fafc;
}

.board-panel span:nth-child(3) {
  right: 72px;
  top: 66px;
  background: #6b57d9;
}

.person-shape {
  position: absolute;
  right: 56px;
  bottom: 8px;
  width: 64px;
  height: 210px;
  border-radius: 36px 36px 16px 16px;
  background: linear-gradient(180deg, #1f2937 0 36%, #0f172a 36% 100%);
}

.person-shape::before {
  position: absolute;
  content: '';
  left: 8px;
  top: -38px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #a35f50;
  box-shadow: -12px -8px 0 #172554;
}

.person-shape::after {
  position: absolute;
  content: '';
  left: -18px;
  top: 42px;
  width: 96px;
  height: 62px;
  border-radius: 18px;
  background: #48a1ff;
}

.plant-shape {
  position: absolute;
  left: 2px;
  bottom: 22px;
  width: 90px;
  height: 150px;
  border-bottom: 8px solid rgba(255, 255, 255, 0.8);
}

.plant-shape::before,
.plant-shape::after {
  position: absolute;
  content: '';
  width: 46px;
  height: 76px;
  border-radius: 44px 44px 0 44px;
  background: rgba(230, 241, 255, 0.8);
}

.plant-shape::before {
  left: 10px;
  top: 18px;
  transform: rotate(-34deg);
}

.plant-shape::after {
  right: 4px;
  top: 52px;
  transform: rotate(24deg);
}

.auth-form-wrap {
  width: min(420px, calc(100% - 48px));
  margin: auto;
}

.form-header {
  margin-bottom: 36px;
  text-align: center;
}

.form-header h1 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0;
}

.form-header p,
.switch-tip,
.form-tools {
  color: #8a94a6;
}

.form-tools {
  display: flex;
  justify-content: space-between;
  margin: -2px 0 28px;
  font-size: 14px;
}

.submit-button {
  width: 176px;
  height: 48px;
  display: block;
  margin: 0 auto;
  border-radius: 14px;
  font-weight: 800;
  box-shadow: 0 14px 28px rgba(47, 124, 255, 0.24);
}

.switch-tip {
  margin: 22px 0 0;
  text-align: center;
}

.switch-tip a {
  color: #2f7cff;
  font-weight: 700;
}

:deep(.ant-input),
:deep(.ant-input-password) {
  border-radius: 14px;
}

@media (max-width: 900px) {
  .auth-page {
    padding: 20px;
  }

  .auth-card {
    min-height: calc(100vh - 40px);
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 320px;
    padding: 42px 32px;
  }

  .auth-visual h2 {
    margin-top: 34px;
    font-size: 34px;
  }

  .board-illustration {
    display: none;
  }

  .auth-form-wrap {
    padding: 48px 0;
  }
}
</style>
