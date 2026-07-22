# AI 生成链路与 SSE 协议

## 1. 生成类型

后端智能路由会选择三种生成类型之一：

| 类型 | 含义 | 输出目录 |
| --- | --- | --- |
| `html` | 单文件 HTML | `tmp/code_output/html_{appId}/` |
| `multi_file` | 多文件 HTML/CSS/JS | `tmp/code_output/multi_file_{appId}/` |
| `vue_app` | Vue 工程化项目 | `tmp/code_output/vue_app_{appId}/` |

静态预览 URL：

```txt
/api/static/{codeGenType}_{appId}/
```

## 2. 首轮生成

前端调用：

```txt
POST /api/app/chat
```

链路：

```txt
前端提交 initPrompt
-> 后端创建 App
-> 保存用户消息
-> Router Agent 选择 codeGenType
-> SSE 返回 appId 和 route
-> 对应生成器生成代码
-> 保存 assistant 消息
-> 如为 vue_app，执行依赖处理和 npm 构建
-> SSE done
```

前端不能在首轮创建时固定走 Vue 生成接口，应以 `r` 事件和后端保存的 `codeGenType` 为准。

## 3. 续聊

前端调用：

```txt
POST /api/app/chat/continue
```

续聊不发送 `r` 事件。后端根据已有应用的 `codeGenType` 分流，前端使用应用详情中的 `codeGenType` 展示生成类型和拼接预览地址。

## 4. SSE 事件

| 事件 | 示例 | 含义 | 前端处理 |
| --- | --- | --- | --- |
| 初始化 | `{"i":123}` | 返回 appId | 保存当前 appId |
| 路由 | `{"r":"vue_app"}` | AI 选择生成类型 | 更新生成类型标签和预览路径 |
| 文本 | `{"d":"token"}` | AI 文本片段 | 累加到 assistant 消息 |
| 工具执行 | `{"t":"tool_executed","name":"writeFile","input":{...}}` | Vue 工具调用完成 | 展示工具步骤卡片 |
| 构建结果 | `{"b":"ok"}` | Vue 构建成功 | 展示成功卡片，刷新预览 |
| 构建失败 | `{"b":"fail","msg":"..."}` | Vue 构建失败 | 展示失败卡片和必要错误信息 |
| 完成 | `event: done` | 流结束 | 关闭 streaming 状态 |

## 5. Vue 工程化生成

Vue 模式下，LLM 通过工具读写项目文件和声明依赖。npm install/build 是后端固定流程，不是 LLM 直接执行命令。

典型链路：

```txt
复制 Vue 项目骨架
-> VueCodeGeneratorAgent 通过工具修改文件
-> DependencyTool 记录依赖
-> FileTools 写入源码
-> 生成完成
-> VueProjectBuilder 写入依赖
-> npm install
-> npm run build
-> 返回构建结果事件
```

## 6. 历史记忆

- 对话历史通过 `/chatHistory/list` 游标分页加载。
- 返回字段为 `records / nextCursor / hasMore`。
- 首次不传 cursor，后续用 `nextCursor`。
- 历史按时间正序展示。
- `role=user` 渲染纯文本气泡。
- `role=assistant` 渲染 STEP 卡片和 Markdown。

## 7. 预览、部署与下载

- 预览使用 `/api/static/{codeGenType}_{appId}/`。
- 部署将生成结果复制到部署目录并返回部署 URL。
- 下载调用 `/api/app/{appId}/download`，前端用 Blob 处理 ZIP。
- 下载和部署都必须基于当前应用权限；后端负责最终鉴权。
