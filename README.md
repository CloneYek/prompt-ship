# Prompt-Ship

> AI 驱动的零代码 Web 应用生成平台 — 用自然语言描述需求，AI 自动生成完整前端代码，支持对话式迭代修改和一键部署。

![Java](https://img.shields.io/badge/JDK-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?logo=springboot)
![Vue](https://img.shields.io/badge/Vue-3-blue?logo=vue.js)
![LangChain4j](https://img.shields.io/badge/LangChain4j-1.1-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 项目架构

### 系统架构图

![系统架构图](docs/architecture.svg)

### 核心工作流

![核心工作流](docs/workflow.svg)

**数据流**: 用户输入 Prompt → 图片素材收集 (Image Agent) → 类型路由判断 (Router Agent) → 代码生成 (HTML / MultiFile / Vue Agent) → 代码解析保存 → Vue 项目构建 (仅 Vue 模式) → 一键部署预览

---

## 功能模块

### AI 智能生成

- **智能路由**: AI 自动判断最适合的代码生成类型 (单文件 HTML / 多文件 / Vue 工程)
- **三种生成模式**:
  - `HTML` — 生成单文件 `index.html`，适合简单页面
  - `MULTI_FILE` — 生成 `index.html + style.css + script.js`，适合中等复杂度页面
  - `VUE_APP` — 通过 Tool Calling 逐步生成 Vue 工程，自动 `npm install + build`
- **图片素材自动收集**: 集成 Pexels 实拍图、undraw 插画、Icons8 图标、腾讯混元 AI Logo，AI 主动搜索并注入代码
- **对话式迭代**: 支持续聊修改已有应用，per-app ChatMemory 保证上下文隔离 (10 轮滑动窗口)

### 应用管理

- **应用 CRUD**: 创建、编辑、删除、查询应用
- **一键部署**: 生成代码一键部署到 Nginx 静态服务，支持在线预览
- **项目下载**: 支持下载应用源码压缩包
- **精选展示**: 优先级 > 0 的应用在首页精选展示

### 用户体系

- **注册/登录**: 账号密码模式
- **角色权限**: `user` / `admin` 两种角色，`@AuthCheck` 注解 + AOP 切面控制访问
- **管理后台**: 管理员可管理用户和应用

### SSE 流式交互

实时推送 AI 生成进度，前端可展示:
- 代码生成类型路由结果
- AI 输出文本流 (token 级)
- Vue 模式下的工具调用步骤 (文件创建、依赖安装)
- 构建成功/失败状态

---

## 技术选型

### 后端

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 核心框架 | Spring Boot | 3.5.15 | JDK 21 |
| ORM | MyBatis-Flex | 1.11.7 | 链式查询 + Lambda 字段引用 |
| AI 接口 | LangChain4j | 1.1.0 / 1.1.0-beta7 | OpenAI 兼容接口 + Reactor 流式输出 |
| AI 模型 | DeepSeek | deepseek-chat / deepseek-v4-flash | 代码生成 + 流式输出 |
| 本地缓存 | Caffeine | Spring Boot 管理 | per-app AI Service 实例缓存 |
| API 文档 | Knife4j + springdoc | 4.4.0 / 2.8.17 | OpenAPI 3 Swagger UI |
| 网页截图 | Selenium | 4.33.0 | 应用封面图自动截取 |
| 对象存储 | 腾讯云 COS | 5.6.54 | 封面图、Logo 存储 |
| AI 生图 | 腾讯混元 | 3.1.1448 | Logo 生成 |
| 工具库 | Hutool | 5.8.38 | 通用工具 |
| 代码简化 | Lombok | 1.18.36 | @Data 生成 getter/setter |

### 前端

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Vue 3 | ^3.5.17 | Composition API |
| 构建 | Vite | ^7.0.0 | 快速构建 |
| 路由 | vue-router | ^4.5.1 | hash 路由模式 |
| 状态管理 | Pinia | ^3.0.3 | 用户状态 + 应用状态 |
| UI 组件 | Ant Design Vue | ^4.2.6 | 企业级 UI 组件库 |
| HTTP | Axios | ^1.11.0 | 请求封装 + SSE 流读取 |
| Markdown | markdown-it | ^14.3.0 | AI 回答渲染 |
| XSS 防护 | DOMPurify | ^3.4.11 | 安全 HTML 渲染 |
| API 生成 | @umijs/openapi | ^1.13.15 | 从后端 OpenAPI 自动生成 API 调用代码 |
| 类型检查 | vue-tsc | ^2.2.10 | TypeScript 类型校验 |

### 数据库

| 类别 | 技术 | 说明 |
|------|------|------|
| 数据库 | MySQL 8 | utf8mb4_unicode_ci |
| 连接池 | HikariCP | Spring Boot 默认 |
| 数据表 | user / app / chat_history | 雪花算法 ID |

### 部署

| 类别 | 技术 | 说明 |
|------|------|------|
| 静态服务 | Nginx | 端口 80，根目录 `code_deploy/` |
| 后端服务 | Spring Boot | 端口 8123，context-path `/api` |

---

## SSE 协议

| 事件 | 格式 | 含义 |
|------|------|------|
| 初始化 | `{"i":"appId"}` | 应用 ID |
| 路由结果 | `{"r":"vue_app"}` | 代码生成类型 |
| 文本块 | `{"d":"token"}` | AI 输出文本 |
| 工具执行 | `{"t":"tool_executed","name":"writeFile","input":{...}}` | Vue 模式工具调用 |
| 构建结果 | `{"b":"ok"}` 或 `{"b":"fail","msg":"错误日志"}` | Vue 构建 |
| 完成 | `event:done` | 流结束 |

---

## 项目结构

```
prompt-ship/
├── src/main/java/com/xiaoyu/promptship/
│   ├── controller/          # REST 接口层
│   │   ├── AppController              # 应用 CRUD + 对话生成 + 部署
│   │   ├── UserController             # 用户登录/注册/管理
│   │   ├── ChatHistoryController      # 对话历史
│   │   ├── StaticResourceController   # 部署后的静态文件浏览
│   │   └── HealthController           # 健康检查
│   ├── service/             # 业务逻辑
│   │   ├── AppServiceImpl             # 核心: 创建/对话/部署
│   │   ├── UserServiceImpl            # 用户管理
│   │   ├── ChatHistoryServiceImpl     # 对话存储
│   │   ├── ScreenshotServiceImpl      # 网页截图
│   │   └── ProjectDownloadServiceImpl # 项目下载
│   ├── ai/                  # AI Agent 层
│   │   ├── AiCodeGeneratorService     # HTML/MultiFile 代码生成
│   │   ├── AiCodeGeneratorServiceFactory # Caffeine 缓存 + ChatMemory 管理
│   │   ├── VueCodeGeneratorAgent      # Vue 工程化 Tool Calling
│   │   ├── CodeGenRouterAgent         # 智能路由判断
│   │   ├── ImageCollectionAgent       # 图片素材收集
│   │   ├── model/                     # AI 输出数据模型
│   │   └── tool/                      # Tool Calling 工具集
│   │       ├── FileTools              # Vue 项目文件操作
│   │       ├── DependencyTool         # npm 依赖声明
│   │       ├── PexelsImageTool        # Pexels 图片搜索
│   │       ├── IconsApiTool           # Icons8 图标搜索
│   │       ├── UndrawIllustrationTool # undraw 插画搜索
│   │       └── LogoGeneratorTool      # 腾讯混元 Logo 生成
│   ├── core/                # 代码生成管道
│   │   ├── AiCodeGeneratorFacade      # 统一入口
│   │   ├── parser/                    # AI 输出 → 代码块解析
│   │   └── saver/                     # 模板方法模式写文件
│   │   └── vue/                       # Vue 骨架 + npm 构建
│   ├── model/               # 数据模型
│   │   ├── entity/                    # 数据库实体
│   │   ├── dto/                       # 请求 DTO
│   │   ├── vo/                        # 视图对象
│   │   └── enums/                     # 枚举定义
│   ├── mapper/              # MyBatis-Flex Mapper
│   ├── config/              # 配置类 (CORS, COS)
│   ├── manager/             # 腾讯云 COS 文件管理
│   ├── annotation/          # @AuthCheck 权限注解
│   ├── aop/                 # AuthCheckAspect 切面
│   ├── constant/            # 常量 (AppConstant, UserConstant)
│   ├── common/              # 通用响应/请求类
│   ├── exception/           # 异常处理体系
│   └── utils/               # 工具类
│
├── src/main/resources/
│   ├── template/            # AI System Prompt 模板
│   │   ├── system-prompt-html.txt
│   │   ├── system-prompt-multi.txt
│   │   ├── system-prompt-vue.txt
│   │   ├── system-prompt-router.txt
│   │   └── system-prompt-image-collection.txt
│   ├── skeleton/vue/        # Vue 项目骨架
│   ├── mapper/              # MyBatis Mapper XML
│   ├── application.yml      # 主配置
│   └── application-local.yml # 本地配置
│
├── prompt-ship-frontend/
│   ├── src/
│   │   ├── api/
│   │   │   ├── generated/              # OpenAPI 自动生成 (不手写)
│   │   │   └── services/               # 业务 API 封装层
│   │   ├── pages/                      # 页面组件
│   │   │   ├── HomeView.vue            # 首页
│   │   │   ├── AppChatPage.vue         # 对话生成页 (核心)
│   │   │   ├── MyAppsPage.vue          # 我的应用
│   │   │   ├── admin/                  # 管理后台
│   │   │   └── user/                   # 登录/注册/个人资料
│   │   ├── components/                 # 公共组件
│   │   ├── layouts/                    # 布局组件
│   │   ├── stores/                     # Pinia 状态
│   │   └── router/                     # 路由配置
│   ├── package.json
│   └── vite.config.ts
│
├── sql/
│   └── create_table.sql               # 数据库建表脚本
├── docs/
│   ├── architecture.svg               # 系统架构图
│   ├── workflow.svg                   # 核心工作流图
│   ├── 接口文档.md                     # API 接口文档
│   ├── 前端对接说明-Vue工程化生成.md     # Vue 模式对接说明
│   └── 一次完整的运行日志.md            # 运行日志参考
├── pom.xml                            # Maven 配置
└── CLAUDE.md                          # AI Agent 协作指南
```

---

## 快速开始

### 前置要求

- JDK 21
- Node.js 18+
- MySQL 8
- Nginx (可选，用于部署预览)

### 1. 数据库初始化

```bash
mysql -u root -p123456 < sql/create_table.sql
```

### 2. 后端启动

```bash
# 编译
JAVA_HOME="path/to/jdk-21" mvn clean compile

# 启动 Spring Boot
JAVA_HOME="path/to/jdk-21" mvn spring-boot:run
```

后端服务启动在 `http://localhost:8123/api`，API 文档: `http://localhost:8123/api/doc.html`

### 3. 前端启动

```bash
cd prompt-ship-frontend
npm install
npm run dev
```

### 4. Nginx 部署 (可选)

```bash
# 启动 Nginx
D:/Nginx/nginx-1.28.3/nginx.exe

# 重载配置
D:/Nginx/nginx-1.28.3/nginx.exe -s reload
```

Nginx 监听端口 80，根目录设为 `tmp/code_deploy/`。

### 5. 配置 AI 服务

在 `application-local.yml` 中配置以下 API Key:

| 配置项 | 说明 |
|--------|------|
| `langchain4j.open-ai.chat-model.api-key` | DeepSeek API Key (必填) |
| `langchain4j.open-ai.streaming-chat-model.api-key` | DeepSeek 流式 API Key (必填) |
| `cos.*` | 腾讯云 COS 配置 (对象存储) |
| `pexels.api-key` | Pexels 图片搜索 API Key |
| `icons8.app-key` | Icons8 图标搜索 Key |
| `undraw.build-id` | undraw 插画搜索 ID |

---

## 关键设计

| 设计点 | 方案 | 说明 |
|--------|------|------|
| AI 服务缓存 | Caffeine + per-app Service | 10 分钟无访问过期，最多 200 实例 |
| 上下文隔离 | MessageWindowChatMemory | 每个 App 独立 10 轮滑动窗口 |
| 权限控制 | `@AuthCheck` + AOP 切面 | 注解式声明，可选 `mustRole = "admin"` |
| SSE 桥接 | SseEmitter (Spring MVC) + Flux (Reactor) | Controller 桥接 framework-agnostic 流 |
| 代码生成管道 | Facade → Parser → Saver | 模板方法模式，可扩展新生成类型 |
| Vue 工程化 | Tool Calling + Skeleton | AI 像开发者一样逐步创建项目文件 |
| 路径安全 | FileTools 路径穿越防护 | AI 生成的文件路径校验，防止越权写入 |

---

## License

MIT
