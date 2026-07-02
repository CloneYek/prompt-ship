import { message } from 'ant-design-vue'
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/pages/HomeView.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: {
        fullWidth: true,
      },
    },
    {
      path: '/app/my',
      name: 'app-my',
      component: () => import('@/pages/MyAppsPage.vue'),
      meta: {
        requiresLogin: true,
      },
    },
    {
      path: '/app/chat/new',
      name: 'app-chat-new',
      component: () => import('@/pages/AppChatPage.vue'),
      meta: {
        requiresLogin: true,
        fullWidth: true,
        hideFooter: true,
      },
    },
    {
      path: '/app/:id/chat',
      name: 'app-chat-detail',
      component: () => import('@/pages/AppChatPage.vue'),
      meta: {
        requiresLogin: true,
        fullWidth: true,
        hideFooter: true,
      },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/pages/admin/UserManagementPage.vue'),
      meta: {
        requiresLogin: true,
        requiredRole: 'admin',
      },
    },
    {
      path: '/admin/apps',
      name: 'admin-apps',
      component: () => import('@/pages/admin/AppManagementPage.vue'),
      meta: {
        requiresLogin: true,
        requiredRole: 'admin',
      },
    },
    {
      path: '/user/profile/edit',
      name: 'user-profile-edit',
      component: () => import('@/pages/user/ProfileEditPage.vue'),
      meta: {
        requiresLogin: true,
      },
    },
    {
      path: '/user/login',
      name: 'user-login',
      component: () => import('@/pages/user/LoginPage.vue'),
      meta: {
        authPage: true,
        hideHeader: true,
        hideFooter: true,
        fullWidth: true,
      },
    },
    {
      path: '/user/register',
      name: 'user-register',
      component: () => import('@/pages/user/RegisterPage.vue'),
      meta: {
        authPage: true,
        hideHeader: true,
        hideFooter: true,
        fullWidth: true,
      },
    },
    {
      path: '/user/register/success',
      name: 'user-register-success',
      component: () => import('@/pages/user/RegisterSuccessPage.vue'),
      meta: {
        authPage: true,
        hideHeader: true,
        hideFooter: true,
        fullWidth: true,
      },
    },
  ],
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const requiresLogin = Boolean(to.meta.requiresLogin)
  const requiredRole = to.meta.requiredRole as string | undefined

  if (!requiresLogin && !requiredRole) {
    return true
  }

  if (!userStore.loaded) {
    await userStore.fetchLoginUser().catch(() => {
      userStore.setLoginUser(null)
    })
  }

  if (!userStore.loginUser) {
    message.warning('请先登录')
    return {
      path: '/user/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  // 前端只做体验层拦截，真正权限仍由后端 @AuthCheck 兜底。
  if (requiredRole && userStore.loginUser.userRole !== requiredRole) {
    message.warning('当前账号没有访问权限')
    return '/'
  }

  return true
})

export default router
