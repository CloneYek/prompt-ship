<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">PromptShip &copy; {{ currentYear }} - 提示词工程平台</p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.footer {
  background: #f8fbff;
  text-align: center;
  padding: 24px;
}

.copyright {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}
</style>
