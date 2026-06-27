<template>
  <main class="home-page">
    <section class="hero-section">
      <div class="hero-inner">
        <div class="hero-kicker">PromptShip</div>
        <h1>一句提示词，驶向更稳定的 AI 工作流</h1>
        <p class="hero-subtitle">沉淀可复用的 Prompt 模板、案例和团队经验，让灵感不再散落在聊天记录里。</p>

        <div id="prompt-box" class="prompt-box">
          <a-textarea
            v-model:value="promptText"
            :auto-size="{ minRows: 4, maxRows: 4 }"
            placeholder="例如：帮我整理一个适合小红书运营的选题提示词库"
            disabled
          />
          <div class="prompt-actions">
            <div class="action-buttons">
              <a-button shape="round">上传</a-button>
              <a-button shape="round">优化</a-button>
            </div>
            <a-button class="send-button" shape="circle" disabled>↑</a-button>
          </div>
        </div>

        <div class="quick-prompts">
          <button v-for="item in quickPrompts" :key="item" type="button">{{ item }}</button>
        </div>
      </div>
    </section>

    <section id="cases" class="cases-section">
      <div class="cases-panel">
        <div class="cases-header">
          <div>
            <span class="section-label">案例广场</span>
            <h2>从真实场景开始复用</h2>
          </div>
          <a-button shape="round">全部案例</a-button>
        </div>

        <div class="case-toolbar">
          <a-select v-model:value="sortValue" class="sort-select" :bordered="false">
            <a-select-option value="default">默认排序</a-select-option>
            <a-select-option value="newest">最新发布</a-select-option>
          </a-select>
          <div class="category-tabs">
            <button
              v-for="category in categories"
              :key="category"
              :class="{ active: activeCategory === category }"
              type="button"
              @click="activeCategory = category"
            >
              {{ category }}
            </button>
          </div>
        </div>

        <div class="case-grid">
          <article v-for="item in filteredCases" :key="item.title" class="case-card">
            <div class="case-cover" :class="item.coverClass">
              <span>{{ item.tag }}</span>
            </div>
            <div class="case-body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const promptText = ref('')
const sortValue = ref('default')
const activeCategory = ref('全部')

const quickPrompts = ['爆款内容选题', '企业知识库', '客服话术', '数据分析报告']
const categories = ['全部', '写作', '营销', '运营', '产品', '数据分析', '团队管理']

const cases = [
  {
    title: '社媒选题 Prompt 库',
    description: '把账号定位、内容方向、爆款结构沉淀成可复用模板。',
    category: '营销',
    tag: '内容增长',
    coverClass: 'cover-blue',
  },
  {
    title: '产品需求拆解助手',
    description: '从一句想法扩展为需求背景、用户故事和验收标准。',
    category: '产品',
    tag: '产品设计',
    coverClass: 'cover-green',
  },
  {
    title: '周报与复盘工作流',
    description: '将零散事项整理成结构清晰、有重点的项目周报。',
    category: '团队管理',
    tag: '效率协作',
    coverClass: 'cover-ink',
  },
  {
    title: '数据洞察报告模板',
    description: '把指标变化转化为结论、原因假设和下一步动作。',
    category: '数据分析',
    tag: '分析报告',
    coverClass: 'cover-coral',
  },
]

const filteredCases = computed(() => {
  if (activeCategory.value === '全部') {
    return cases
  }
  return cases.filter((item) => item.category === activeCategory.value)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  color: #111827;
  background:
    radial-gradient(circle at 18% 34%, rgba(32, 211, 176, 0.2), transparent 32%),
    radial-gradient(circle at 82% 58%, rgba(47, 124, 255, 0.24), transparent 34%),
    linear-gradient(180deg, #fbfdfb 0%, #e9fbfb 42%, #dbeafe 100%);
}

.hero-section {
  min-height: 720px;
  display: flex;
  align-items: center;
  padding: 72px 24px 140px;
}

.hero-inner {
  width: min(980px, 100%);
  margin: 0 auto;
  text-align: center;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 22px;
  border-radius: 18px;
  background: #20d3b0;
  color: #07111f;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 18px 42px rgba(32, 211, 176, 0.22);
}

.hero-inner h1 {
  max-width: 800px;
  margin: 0 auto;
  font-size: 46px;
  line-height: 1.22;
  font-weight: 900;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 620px;
  margin: 22px auto 40px;
  color: #536174;
  font-size: 17px;
  line-height: 1.8;
}

.prompt-box {
  max-width: 880px;
  margin: 0 auto;
  padding: 18px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 26px 70px rgba(17, 58, 113, 0.13);
  text-align: left;
}

.prompt-box :deep(textarea.ant-input) {
  resize: none;
  border: none;
  box-shadow: none;
  color: #111827;
  background: transparent;
  font-size: 18px;
}

.prompt-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.send-button {
  width: 42px;
  height: 42px;
  font-size: 20px;
  font-weight: 900;
}

.quick-prompts {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.quick-prompts button,
.category-tabs button {
  border: none;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.quick-prompts button {
  padding: 9px 18px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  color: #536174;
  font-weight: 600;
}

.quick-prompts button:hover {
  color: #111827;
  transform: translateY(-1px);
}

.cases-section {
  padding: 0 24px 84px;
}

.cases-panel {
  width: min(1680px, 100%);
  margin: -64px auto 0;
  padding: 52px;
  border-radius: 26px 26px 0 0;
  background: #fff;
  box-shadow: 0 -20px 60px rgba(17, 58, 113, 0.08);
}

.cases-header,
.case-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.section-label {
  display: inline-block;
  margin-bottom: 10px;
  color: #2f7cff;
  font-size: 14px;
  font-weight: 800;
}

.cases-header h2 {
  margin: 0;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0;
}

.case-toolbar {
  margin: 32px 0 24px;
}

.sort-select {
  min-width: 132px;
  height: 42px;
  border-radius: 999px;
  background: #f6f8fb;
}

.category-tabs {
  display: flex;
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.category-tabs button {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  background: #f6f8fb;
  color: #667085;
  font-weight: 700;
}

.category-tabs button.active {
  background: #111827;
  color: #fff;
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
}

.case-card {
  overflow: hidden;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(17, 24, 39, 0.07);
}

.case-cover {
  height: 170px;
  display: flex;
  align-items: flex-end;
  padding: 18px;
  color: #fff;
  font-weight: 900;
}

.case-cover span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
}

.cover-blue {
  background: linear-gradient(135deg, #111827, #2f7cff);
}

.cover-green {
  background: linear-gradient(135deg, #0f766e, #84cc16);
}

.cover-ink {
  background: linear-gradient(135deg, #111827, #64748b);
}

.cover-coral {
  background: linear-gradient(135deg, #f97316, #db2777);
}

.case-body {
  padding: 20px;
}

.case-body h3 {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0;
}

.case-body p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .case-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-section {
    min-height: 640px;
    padding-top: 54px;
  }

  .hero-inner h1 {
    font-size: 34px;
  }

  .prompt-box {
    border-radius: 20px;
  }

  .prompt-actions,
  .cases-header,
  .case-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .category-tabs {
    justify-content: flex-start;
  }

  .cases-panel {
    padding: 32px 20px;
    border-radius: 22px 22px 0 0;
  }

  .case-grid {
    grid-template-columns: 1fr;
  }
}
</style>
