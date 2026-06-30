<template>
  <div class="auth-page register-page">
    <section class="auth-card">
      <div class="auth-visual">
        <RouterLink to="/" class="auth-brand">
          <span class="brand-icon">P</span>
          <span>PromptShip</span>
        </RouterLink>
        <h2>创建你的<br />提示词工作间</h2>
        <p>从第一条高质量提示词开始，沉淀一套可复用的方法。</p>
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
          <h1>注册 PromptShip</h1>
          <p>账号创建后即可登录使用</p>
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

          <a-form-item
            label="确认密码"
            name="checkPassword"
            :rules="[
              { required: true, message: '请再次输入密码' },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              size="large"
              placeholder="请再次输入密码"
            />
          </a-form-item>

          <a-button class="submit-button" type="primary" html-type="submit" :loading="submitting">
            注册
          </a-button>
        </a-form>

        <p class="switch-tip">
          已有账号？
          <RouterLink to="/user/login">去登录</RouterLink>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/services/userService'

const router = useRouter()
const submitting = ref(false)

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = async () => {
  if (!formState.checkPassword) {
    return Promise.reject(new Error('请再次输入密码'))
  }
  if (formState.checkPassword !== formState.userPassword) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }
  return Promise.resolve()
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await userRegister(formState)
    if (res.data.code === 0) {
      router.push('/user/register/success')
      return
    }
    message.error(res.data.message || '注册失败，请稍后重试')
  } catch (error) {
    message.error('注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@import './auth-shared.css';

.register-page .auth-visual {
  background: linear-gradient(135deg, #2563eb 0%, #3b86ff 54%, #20d3b0 100%);
}
</style>
