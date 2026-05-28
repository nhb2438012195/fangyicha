# 房易查 (FangYiCha)

一站式购房服务平台，提供房产信息查询、智能推荐、AI 购房助手、在线下单等功能。

## 功能模块

### 购房客户端（CUSTOMER）
- 房产搜索与筛选（按区域、户型、价格、面积等）
- 房产详情页（图片轮播、价格走势图表、地理位置）
- 个性化推荐（基于用户偏好区域和预算）
- 购房建议向导
- 收藏管理
- 在线下单
- 意向建议提交

### 开发商后台（DEVELOPER）
- 楼盘管理（新增 / 编辑 / 上下架）
- 订单管理
- 客户意向查看与回复
- 数据分析面板
- 知识库文档管理（上传 PDF/Word → AI 学习）

### AI 购房助手（房易小助手）
- 基于 RAG（检索增强生成）的智能问答
- 楼盘推荐（卡片式展示，可点击跳转详情）
- 收藏管理（自然语言触发添加/查看收藏，DeepSeek 原生 Function Calling）
- 在线下单（两阶段确认：预览 → 确认）
- 多会话持久化管理
- 知识库：平台数据 + 房地产知识 + 开发商上传文档

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 + JDK 17 |
| AI 框架 | Spring AI 1.0.0（DeepSeek 原生模块） |
| LLM | DeepSeek API（对话 + Embedding） |
| RAG 检索引擎 | Apache Lucene 9.12（BM25 + 向量混合检索） |
| 文档解析 | Apache Tika 2.9 |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT + Spring Security |
| 前端 | Vue 3.5 + TypeScript + Pinia + Element Plus |
| 图表 | ECharts 6 + Vue-ECharts |
| API 文档 | Knife4j / Swagger |

## 项目结构

```
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/fangyicha/
│   │   ├── ai/tool/                  # AI 工具回调（收藏、下单）
│   │   ├── common/                   # 通用常量、统一响应
│   │   ├── config/                   # 安全、Swagger、种子数据
│   │   ├── controller/               # REST 控制器
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 数据库实体
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── security/                 # JWT 认证过滤器
│   │   └── service/                  # 业务逻辑服务
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置
│   │   └── db/schema.sql             # 数据库初始化
│   └── data/
│       ├── lucene-index/             # Lucene 索引文件
│       ├── emb-cache/                # Embedding 缓存
│       ├── rag-platform/             # 平台知识库数据
│       └── rag-uploads/              # 开发商上传文档
│
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── api/                      # API 封装
│       ├── stores/                   # Pinia 状态管理
│       ├── types/                    # TypeScript 类型
│       └── views/
│           ├── customer/             # 购房客户端页面
│           │   └── components/chat/  # AI 聊天组件
│           └── developer/            # 开发商后台页面
│
└── docs/                             # 文档
    ├── ai-assistant-review.md        # AI 助手审查报告
    ├── ai-assistant-root-cause-analysis.md
    └── spring-ai-m6-deepseek-incompatibility.md
```

## 快速启动

### 环境要求
- JDK 17+
- MySQL 8.0
- Node.js 18+
- DeepSeek API Key

### 1. 数据库初始化
```sql
CREATE DATABASE IF NOT EXISTS fangyicha;
-- 执行 backend/src/main/resources/db/schema.sql
-- 执行 backend/src/main/resources/db/data.sql
```

### 2. 后端启动
```bash
cd backend
export DEEPSEEK_API_KEY=your_api_key
mvn spring-boot:run
# 服务启动在 http://localhost:8088
# API 文档: http://localhost:8088/doc.html
```

### 3. 前端启动
```bash
cd frontend
npm install
npm run dev
# 开发服务器启动在 http://localhost:5173
```

### 默认账号
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 开发商 | admin | admin123456 |
| 客户 | testuser | test123456 |

## 配置说明

```yaml
# backend/src/main/resources/application.yml
spring:
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}   # 必需环境变量
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
      embedding:
        options:
          model: deepseek-embedding
```
