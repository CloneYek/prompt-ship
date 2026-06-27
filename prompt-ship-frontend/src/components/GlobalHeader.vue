<template>
  <a-layout-header class="header">
    <div class="header-inner">
      <RouterLink to="/" class="brand-link">
        <div class="brand-mark">P</div>
        <h1 class="site-title">PromptShip</h1>
      </RouterLink>

      <nav class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <a href="#cases">案例广场</a>
        <a href="#prompt-box">开始创作</a>
      </nav>

      <div class="user-login-status">
        <template v-if="userStore.loginUser">
          <a-dropdown>
            <button class="user-button" type="button">
              <a-avatar :src="userStore.loginUser.userAvatar">
                {{ userInitial }}
              </a-avatar>
              <span>{{ displayName }}</span>
            </button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <template v-else>
          <a-button class="ghost-button" @click="goRegister">注册</a-button>
          <a-button type="primary" class="login-button" @click="goLogin">登录</a-button>
        </template>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const displayName = computed(
  () => userStore.loginUser?.userName || userStore.loginUser?.userAccount || '用户',
)
const userInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())

onMounted(() => {
  if (!userStore.loaded) {
    userStore.fetchLoginUser().catch(() => {
      userStore.setLoginUser(null)
    })
  }
})

const goLogin = () => {
  router.push('/user/login')
}

const goRegister = () => {
  router.push('/user/register')
}

const handleLogout = async () => {
  await userStore.logout()
  message.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.header {
  height: 72px;
  padding: 0;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  backdrop-filter: blur(16px);
}

.header-inner {
  width: min(1680px, calc(100% - 96px));
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
}

.brand-link,
.user-button {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
}

.brand-link {
  gap: 12px;
  color: #111827;
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #2f7cff, #20d3b0);
  color: #fff;
  font-weight: 800;
  box-shadow: 0 10px 26px rgba(47, 124, 255, 0.22);
}

.site-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #111827;
  letter-spacing: 0;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 34px;
  margin-right: auto;
}

.nav-links a {
  color: #4b5563;
  font-size: 15px;
  font-weight: 600;
}

.nav-links a:hover {
  color: #2f7cff;
}

.user-login-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.login-button {
  min-width: 84px;
  height: 42px;
  border-radius: 12px;
  font-weight: 700;
  background: #111827;
}

.ghost-button {
  height: 42px;
  border-radius: 12px;
  font-weight: 700;
  border-color: transparent;
}

.user-button {
  gap: 10px;
  height: 44px;
  padding: 0 12px 0 6px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  color: #111827;
  cursor: pointer;
}

@media (max-width: 768px) {
  .header-inner {
    width: calc(100% - 32px);
    gap: 16px;
  }

  .site-title {
    font-size: 20px;
  }

  .nav-links {
    display: none;
  }
}
</style>
