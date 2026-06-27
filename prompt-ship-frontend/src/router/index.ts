import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/pages/HomeView.vue'

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

export default router
