# 前端对接说明 — Vue 工程化项目生成

## 概述

后端新增了第三种代码生成模式 `VUE_APP`（前两种是 `HTML` 和 `MULTI_FILE`）。核心变化：AI 通过工具调用（Tool Calling）逐步创建 Vue 项目文件，生成完成后自动执行 `npm install && npm build`。前端需要适配新的 SSE 事件类型和 API 端点。

---

## 新增/变更的 API

### 1. 创建并生成 Vue 项目（SSE）

```
POST /api/app/chat/vue
Body: { "initPrompt": "创建一个博客网站" }
返回: text/event-stream
```

### 2. 续聊（自动分流，无需前端判断类型）

```
POST /api/app/chat/continue   ← 端点不变
Body: { "appId": 1, "message": "加个轮播图" }
返回: text/event-stream
```

后端根据 app 的 `codeGenType` 自动选择 Vue/旧模式流程。前端无需改动续聊请求。

### 3. 部署（不变）

```
POST /api/app/deploy
Body: { "appId": 1 }
```

后端内部处理 VUE_APP 的 dist/ 目录。

---

## SSE 协议扩展

Vue 模式的 SSE 事件比旧模式多了 `tool_executed` 和 `b` 事件：

| 事件 | data 格式 | 含义 |
|------|----------|------|
| 初始化 | `{"i":"appId"}` | 同旧模式 |
| 文本 chunk | `{"d":"token"}` | AI 文本，同旧模式 |
| **工具执行** | `{"t":"tool_executed","id":"call_xxx","name":"writeFile","input":{"path":"src/App.vue"}}` | **新增**，每完成一次工具调用触发 |
| **构建结果** | `{"b":"ok"}` 或 `{"b":"fail","msg":"错误日志"}` | **新增**，AI 完成后触发 |
| 完成 | `event: done` | 同旧模式 |

### 前端处理要点

1. **旧模式的 `{"d":"token"}` 解析逻辑不变**，新模式下 AI 仍会输出文本说明
2. **新增 `t` 字段处理**：当收到 `{"t":"tool_executed",...}` 时，在对话区渲染"工具执行步骤"卡片（如"已创建 src/App.vue"），其中 `input.path` 是文件路径
3. **新增 `b` 字段处理**：`"ok"` 时展示构建成功，`"fail"` 时展示错误信息，建议用 `msg` 展示详情
4. **`event: done` 处理不变**

---

## 预览与部署

- 预览 URL 格式不变：`/api/static/vue_app_{appId}/`
- 后端已自动将请求路由到 `dist/` 子目录
- 部署仍返回 `http://localhost/{deployKey}/`
- Vue 项目使用 **hash 路由**，Nginx 无需修改
- 预览 iframe 刷新逻辑不变（increment key 触发重载）

---

## 前端需要改动的页面

### AppChatPage.vue

1. **解析新 SSE 事件**：在 SSE reader 中增加对 `t` 和 `b` 字段的判断
2. **渲染工具步骤**：收到 `tool_executed` 时，在 AI 消息中插入步骤卡片（类似现有的 `parseAssistantSteps` 逻辑，但事件来源从文本解析改为 SSE 事件）
3. **展示构建状态**：收到 `b` 事件后，在消息底部展示构建成功/失败标识
4. **模式选择器**：首页 prompt 输入框旁增加模式选择（可选，暂可硬编码默认 VUE_APP）

### appService.ts

1. 新增 `chatToGenVueApp()` 函数（参考现有 `chatToGenerateApp`，调 `/app/chat/vue`）
2. `readChatStream()` 中扩展 SSE 事件解析，增加 `t`/`b` 字段的 handler 回调
3. `getGeneratedPreviewUrl()` 中 VUE_APP 的拼接可能需调整（目前后端已兼容 `/static/vue_app_{id}/`）
