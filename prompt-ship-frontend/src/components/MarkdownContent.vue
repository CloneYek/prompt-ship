<template>
  <div class="markdown-content" v-html="safeHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

const props = defineProps<{
  content: string
}>()

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const safeHtml = computed(() => DOMPurify.sanitize(markdown.render(props.content || '')))
</script>

<style scoped>
.markdown-content {
  color: #111827;
  font-size: 14px;
  line-height: 1.8;
  overflow-wrap: anywhere;
}
.markdown-content :deep(*) {
  box-sizing: border-box;
}
.markdown-content :deep(p),
.markdown-content :deep(ul),
.markdown-content :deep(ol),
.markdown-content :deep(blockquote),
.markdown-content :deep(pre),
.markdown-content :deep(table) {
  margin: 0 0 10px;
}
.markdown-content :deep(:last-child) {
  margin-bottom: 0;
}
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  margin: 12px 0 8px;
  color: #0f172a;
  font-weight: 900;
  line-height: 1.35;
}
.markdown-content :deep(h1) {
  font-size: 20px;
}
.markdown-content :deep(h2) {
  font-size: 18px;
}
.markdown-content :deep(h3) {
  font-size: 16px;
}
.markdown-content :deep(h4) {
  font-size: 15px;
}
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 22px;
}
.markdown-content :deep(li + li) {
  margin-top: 4px;
}
.markdown-content :deep(blockquote) {
  padding: 8px 12px;
  border-left: 3px solid #9fc3ff;
  color: #475467;
  background: #f5f8ff;
}
.markdown-content :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  color: #0f3b72;
  background: #eef4ff;
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
    monospace;
  font-size: 13px;
}
.markdown-content :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  padding: 12px;
  border: 1px solid #dce5f2;
  border-radius: 8px;
  background: #0f172a;
}
.markdown-content :deep(pre code) {
  display: block;
  padding: 0;
  color: #e5eefc;
  background: transparent;
  white-space: pre;
}
.markdown-content :deep(a) {
  color: #1769e0;
  font-weight: 700;
  text-decoration: none;
}
.markdown-content :deep(a:hover) {
  text-decoration: underline;
}
.markdown-content :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
}
.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 6px 10px;
  border: 1px solid #dce5f2;
  text-align: left;
}
.markdown-content :deep(th) {
  background: #f5f8ff;
  font-weight: 900;
}
</style>
