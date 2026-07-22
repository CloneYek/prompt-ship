# AGENTS.md

本文件只规定 AI 编码 Agent 在本仓库中的行为边界和文档读取规则。产品需求、技术方案、接口细节、缺陷复盘和学习笔记由 `docs/` 维护，本文件不重复抄写。

## 1. 基本行为

- 始终使用中文回复；代码、标识符、API、数据库字段保持项目既有英文命名风格。
- 先查项目文档、现有代码和测试，再决定如何实现。文档已有答案时，不重复询问用户。
- 只修改当前任务涉及的内容，不顺手做无关重构，不为尚未发生的需求提前建设复杂抽象。
- 不能在当前任务中完成的部分要明确说明，不承诺后台交付。
- 用户要求先讲设计或先制定计划时，先停止在方案阶段，等待用户确认后再编码。

## 2. 文档读取顺序

- 开始任务前优先读取 `docs/文档治理规范.md`，确认应创建或更新哪类文档。
- 涉及项目背景、运行方式或架构边界时，读取：
  - `docs/项目总览.md`
  - `docs/本地运行与验证.md`
  - `docs/后端架构与开发规范.md`
  - `docs/前端架构与开发规范.md`
  - `docs/AI生成链路与SSE协议.md`
- 涉及接口时，读取 `docs/接口文档.md` 和相关对接说明；生成代码以 `prompt-ship-frontend/src/api/generated/` 为机器生成边界。
- 涉及 Vue 工程化生成时，读取 `docs/前端对接说明-Vue工程化生成.md` 和 `docs/AI生成链路与SSE协议.md`。
- 冲突优先级：`AGENTS.md` -> 用户本次明确要求 -> `REQ` -> `DESIGN` -> `BUG` -> `PROG` -> `NOTE` -> 现有代码。发现冲突时说明差异，不静默猜测。

## 3. 文档治理规则

- 新功能或大范围改造前，若没有对应需求文档，先协助用户沉淀 `docs/requirements/REQ-YYYYMMDD-XX-*.md`。
- 复杂改造、跨前后端联调、AI 生成链路变化、数据库变化，编码前先创建或更新 `docs/designs/DESIGN-YYYYMMDD-XX-*.md`。
- 复杂 bug 定位必须记录到 `docs/bugs/BUG-YYYYMMDD-XX-*.md`，包含现象、根因、修复和验证。
- 每完成一个可验证闭环，更新当天 `docs/progress/PROG-YYYYMMDD.md`。
- 用户要求总结学习收获、链路解释或复盘时，写入 `docs/notes/NOTE-YYYYMMDD-XX-*.md`。
- `AGENTS.md` 只维护 Agent 行为和代码边界，不复制项目文档细节。

## 4. 开始任务前必须自主检查

- 查看相关 `REQ / DESIGN / BUG / PROG / NOTE`，确认当前阶段、下一任务、前置依赖和阻塞。
- 在 `docs/` 中搜索相关页面、功能名、接口、表、SSE 事件和验收标准。
- 检查受影响代码、测试和已有实现模式，优先沿用本仓库现有风格。
- 先看 `git diff` 和 `git status`，识别用户或其他工具已修改的文件，不覆盖无关改动。

## 5. 后端开发边界

- Controller 只做参数接收、鉴权入口和响应桥接；业务逻辑放在 Service。
- API 不直接写 SQL，也不直接调用第三方数据源；数据访问走 Mapper/Service 既有模式。
- 参数校验使用项目已有 `@Valid`、`ThrowUtils.throwIf`、`ErrorCode`、`BusinessException` 模式。
- 新增或变更数据库结构时，先写迁移/SQL 和约束，再改实体、Mapper、Service 和 API。
- AI 生成、文件写入、部署、下载、SSE 相关逻辑必须保持路径边界和权限校验，不允许越权访问其他应用目录。

## 6. 前端开发边界

- Vue 页面、store、组件从 `src/api/services/` 导入手写服务封装，不直接依赖 `src/api/generated/`。
- `src/api/generated/` 是 OpenAPI 机器生成区，除用户明确说明已刷新外，不手写修改。
- 前端新增接口时，优先在 `src/api/services/` 做稳定业务封装，统一处理 id 字符串/数字转换、Blob、SSE、错误提示等页面不该关心的细节。
- UI 改动遵循现有 Vue 3 + Ant Design Vue + scoped CSS 风格，不引入新的设计体系。
- 对话页、首页、我的应用等核心页面改动后，必须验证类型检查和构建，不能只看浏览器效果。

## 7. AI 生成链路与 SSE

- 首轮生成走 `/app/chat`，由后端智能路由选择 `html / multi_file / vue_app`。
- 续聊走 `/app/chat/continue`，沿用已有应用的 `codeGenType`，前端不重新选择生成类型。
- SSE 解析要兼容 `i`、`r`、`d`、`tool_executed`、`b`、`done` 等事件；不能因为新增事件破坏文本流式输出。
- Vue 工程化生成涉及 Tool Calling、依赖声明、npm 构建、预览刷新和构建结果卡片，修改前必须读取链路文档。

## 8. 验证与交付标准

- 后端改动优先运行相关 Maven 编译或测试；必须使用 JDK 21。
- 前端改动优先运行 `npm run type-check`，再运行 `npm run build`。若当前 shell 找不到 `npm`，可使用本地 `node_modules` 和 Codex bundled Node 验证。
- Vite 大 chunk 提示视为构建警告，除非任务专门要求包体积优化。
- 完成后说明：设计决策、调用链路、参数校验、验证结果、未完成项或风险。
- “代码能运行”不等于完成；测试、文档和进度没有同步时，任务仍未完成。

## 9. Git 与文件安全

- 未经用户明确授权，不推送远程、不发布版本。
- 提交只包含当前任务相关修改，不混入无关格式化、重构、依赖目录或构建产物。
- 不提交 `.env*`、密钥、数据库、备份、日志、`node_modules/`、`target/`、`dist/`、`tmp/`。
- 工作区已有用户改动时，必须保护这些改动；除非用户明确要求，不执行 `git reset --hard`、`git checkout --` 等破坏性操作。

## 10. Windows 与编码

- Vue/TS/CSS/Markdown 源文件必须使用 UTF-8。
- PowerShell/终端输出中文乱码不一定代表文件损坏；必要时用 UTF-8 读取并检查 `\uFFFD` 替换字符。
- Windows 上测试中文 JSON API 时，优先使用 UTF-8 文件配合 `--data-binary @file`，避免内联 `curl -d` 编码不确定。
