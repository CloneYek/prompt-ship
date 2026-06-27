<template>
  <div class="success-page">
    <section class="success-card">
      <RouterLink to="/" class="success-brand">
        <span class="brand-icon">P</span>
        <span>PromptShip</span>
      </RouterLink>

      <div class="success-illustration" aria-hidden="true">
        <div class="board-panel">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <div class="person-shape"></div>
        <div class="plant-shape"></div>
      </div>

      <h1>你已成功注册！</h1>
      <a-button class="start-button" type="primary" size="large" @click="goLogin">开始使用</a-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goLogin = () => {
  router.push('/user/login')
}
</script>

<style scoped>
.success-page {
  min-height: 100vh;
  padding: 42px;
  background: linear-gradient(135deg, #f7fbff 0%, #eef6ff 48%, #f9fbff 100%);
}

.success-card {
  position: relative;
  min-height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 28px 80px rgba(26, 71, 133, 0.08);
}

.success-brand {
  position: absolute;
  left: 52px;
  top: 42px;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #111827;
  font-size: 22px;
  font-weight: 800;
  text-decoration: none;
}

.brand-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #2f7cff, #20d3b0);
  color: #fff;
}

.success-illustration {
  position: relative;
  width: min(520px, 78vw);
  height: 330px;
  margin-bottom: 28px;
}

.board-panel {
  position: absolute;
  left: 76px;
  right: 86px;
  top: 42px;
  height: 178px;
  border: 6px solid #3b86ff;
  border-radius: 8px;
  background: #f8fbff;
  box-shadow: 20px 16px 0 #e8f1ff;
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
  width: 68px;
  height: 50px;
  border-radius: 4px;
  box-shadow: 0 12px 18px rgba(28, 65, 126, 0.1);
}

.board-panel span:nth-child(1) {
  left: 32px;
  top: 54px;
  background: #174d98;
}

.board-panel span:nth-child(2) {
  left: 136px;
  top: 66px;
  background: #fff;
}

.board-panel span:nth-child(3) {
  right: 60px;
  top: 64px;
  background: #6b57d9;
}

.person-shape {
  position: absolute;
  right: 86px;
  bottom: 18px;
  width: 62px;
  height: 208px;
  border-radius: 36px 36px 16px 16px;
  background: linear-gradient(180deg, #1f2937 0 36%, #0f172a 36% 100%);
}

.person-shape::before {
  position: absolute;
  content: '';
  left: 8px;
  top: -38px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #a35f50;
  box-shadow: -12px -8px 0 #172554;
}

.person-shape::after {
  position: absolute;
  content: '';
  left: -18px;
  top: 42px;
  width: 94px;
  height: 62px;
  border-radius: 18px;
  background: #48a1ff;
}

.plant-shape {
  position: absolute;
  left: 44px;
  bottom: 30px;
  width: 84px;
  height: 142px;
  border-bottom: 8px solid #d7e6fb;
}

.plant-shape::before,
.plant-shape::after {
  position: absolute;
  content: '';
  width: 44px;
  height: 70px;
  border-radius: 44px 44px 0 44px;
  background: #dfe9f7;
}

.plant-shape::before {
  left: 8px;
  top: 18px;
  transform: rotate(-34deg);
}

.plant-shape::after {
  right: 2px;
  top: 50px;
  transform: rotate(24deg);
}

.success-card h1 {
  margin: 0 0 26px;
  color: #111827;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0;
}

.start-button {
  min-width: 148px;
  height: 48px;
  border-radius: 14px;
  font-weight: 800;
  box-shadow: 0 14px 28px rgba(47, 124, 255, 0.24);
}

@media (max-width: 700px) {
  .success-page {
    padding: 20px;
  }

  .success-card {
    min-height: calc(100vh - 40px);
  }

  .success-brand {
    left: 28px;
    top: 28px;
  }

  .success-illustration {
    height: 260px;
  }

  .board-panel {
    left: 24px;
    right: 58px;
  }

  .plant-shape {
    display: none;
  }

  .person-shape {
    right: 42px;
    transform: scale(0.86);
    transform-origin: bottom right;
  }
}
</style>
