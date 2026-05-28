# 房易小助手 AI 助手实现审查报告

**审查日期**: 2026-05-28  
**审查范围**: 仅限 AI 助手相关的后端、前端代码及数据库 schema  
**审查人**: Claude Code (deepseek-v4-pro)

---

## 一、架构总览

### 技术栈

| 层级 | 技术 | 用途 | 版本 |
|------|------|------|------|
| AI 框架 | Spring AI (OpenAI starter) | DeepSeek API 调用代理 | 1.0.0-M6 |
| LLM | DeepSeek (deepseek-chat) | 对话生成 | - |
| Embedding | DeepSeek (deepseek-embedding) | 文本向量化 | - |
| RAG 检索引擎 | Apache Lucene | BM25 + 向量混合检索 | 9.12.0 |
| 文档解析 | Apache Tika | PDF/DOCX/DOC → 纯文本 | 2.9.2 |
| 数据库 | MySQL + MyBatis-Plus | 会话/消息/订单持久化 | - |
| 前端 | Vue 3 + Pinia + TypeScript | 聊天窗口 UI | - |
| JSON | Gson | 元数据序列化 | - |

### 文件清单（共 20 个核心文件）

**后端 (14 文件)**:
- `controller/AiController.java` — 聊天、会话、消息 API
- `controller/KnowledgeBaseController.java` — 知识库文档管理 API
- `service/AiChatService.java` — 聊天服务接口
- `service/impl/AiChatServiceImpl.java` — **核心对话逻辑** (554行)
- `service/FunctionCallService.java` — 函数调用服务接口
- `service/impl/FunctionCallServiceImpl.java` — 函数调用实现 (360行)
- `service/RagService.java` — RAG 检索接口
- `service/impl/RagServiceImpl.java` — **Lucene 混合检索实现** (361行)
- `service/KnowledgeBaseService.java` — 知识库管理接口
- `service/impl/KnowledgeBaseServiceImpl.java` — 知识库管理实现 (237行)
- `service/AiSessionService.java` / `impl/AiSessionServiceImpl.java` — 会话管理
- `service/DocumentParserService.java` / `impl/DocumentParserServiceImpl.java` — Tika 解析
- `config/KnowledgeBaseSeeder.java` — 启动时播种平台数据

**前端 (6 文件)**:
- `api/ai.ts` — AI API 封装
- `stores/chat.ts` — Pinia 聊天状态管理 (252行)
- `types/ai.ts` — TypeScript 类型定义
- `views/customer/components/chat/ChatWindow.vue` — 主聊天窗口 (653行)
- `views/customer/components/chat/FloatingBubble.vue` — 悬浮气泡 (77行)
- `views/customer/components/chat/SessionPanel.vue` — 会话面板 (295行)

---

## 二、功能清单

| 功能 | 状态 | 说明 |
|------|------|------|
| 对话管理 | 已实现 | 多会话、CRUD、分页消息 |
| RAG 知识检索 | 部分实现 | BM25 可用，向量检索存在缺陷 |
| 楼盘推荐卡片 | 已实现 | 基于 RAG 结果生成推荐卡片 |
| 添加收藏 | 已实现 | 关键字匹配 + 楼盘名提取 |
| 查看收藏 | 已实现 | 关键字匹配 |
| 创建订单预览 | 已实现 | 关键字匹配 + 两阶段确认 |
| 确认订单 | 已实现 | 关键字匹配 |
| 知识库文档上传 | 已实现 | PDF/DOCX/DOC，Tika 解析 → Lucene 索引 |
| 知识库文档管理 | 已实现 | 上传/索引/删除/预览 |
| 平台数据自动播种 | 已实现 | 启动时将 DB 数据导出为文本 → 索引 |

---

## 三、发现的问题

### 严重 (CRITICAL) — 必须修复

#### C1. 向量检索完全失效

**位置**: `RagServiceImpl.java:327-345`, `extractEmbeddingFromDoc()` 方法

```java
private float[] extractEmbeddingFromDoc(Document doc, int docId, IndexSearcher searcher) {
    try {
        for (var leaf : searcher.getIndexReader().leaves()) {
            FloatVectorValues vectorValues = leaf.reader().getFloatVectorValues("embedding");
            if (vectorValues != null) {
                int docIndex = docId - leaf.docBase;
                if (docIndex >= 0 && docIndex < leaf.reader().maxDoc()) {
                    // 这里只有注释，没有任何实际提取逻辑
                }
            }
        }
    } catch (Exception e) {
        log.warn("提取文档嵌入向量失败, docId={}", docId);
    }
    return null;  // 永远返回 null
}
```

**影响**: 混合检索退化为纯 BM25 关键词匹配。所有文档的 `vectorScore` 始终为 0，`cosineSimilarity()` 计算出的向量分数从未被使用。这意味着花了成本调用了 DeepSeek Embedding API 生成了向量，但向量检索部分完全没有生效。

**修复方向**: 使用 `FloatVectorValues.advance(docIndex)` 定位到对应文档，通过 `vectorValues.vectorValue()` 获取向量。

#### C2. API Key 硬编码

**位置**: `application.yml:18`

```yaml
api-key: ${DEEPSEEK_API_KEY:sk-f80eccbf210e43ad9c925c4446d6cee2}
```

**影响**: 虽然有环境变量覆盖机制，但 fallback 值是一个完整有效的 API Key。如果不设置环境变量，Key 就会被直接使用。这违反了项目安全规则。如果代码被提交到公开仓库，该 Key 已经泄露。

**修复方向**: 移除 fallback 默认值，改为启动时检测并报错。

---

### 高优先级 (HIGH) — 应该修复

#### H1. 意图检测为手工关键字匹配，而非真正的 Function Calling

**位置**: `AiChatServiceImpl.java:253-323`, `detectAndExecuteFunction()` 方法

当前实现通过 Java 字符串匹配（`msg.contains("收藏")`, `msg.contains("买")` 等）来检测用户意图。需求文档明确要求使用 Function Calling，但 Spring AI 的工具调用能力（`@Tool` 注解 / `ToolCallback`）完全未被使用。

**具体问题**:
- "确认" 关键字会触发订单确认，即使用户只是在普通对话中说"我确认这是对的"
- "买" 关键字会触发下单预览，即使用户说"我不想买"
- 关键字列表硬编码，无法扩展
- 无法处理复杂语义（如"帮我把刚才看的那套房子收藏了" → 需要上下文推理）

#### H2. 函数调用路径中 RAG 上下文被浪费

**位置**: `AiChatServiceImpl.java:151-213`, `executeChat()` 方法

当检测到函数调用意图时（`functionHandled = true`），走的是函数处理分支，该分支构建了 `functionContext` 但没有注入 RAG 结果。这意味着在对话上下文中缺少知识库内容。

#### H3. 零测试覆盖

**位置**: 整个 `src/test/` 目录

唯一的测试文件检查开发商表不为空。以下功能完全没有测试:
- 聊天对话逻辑
- Function Calling 意图识别
- RAG 检索准确性
- 会话管理 CRUD
- 知识库文档索引

根据项目测试规则要求 80% 覆盖率，AI 助手模块的测试覆盖率为 0%。

#### H4. Spring AI 使用流于表面

**位置**: `AiChatServiceImpl.java` 整体

Spring AI 1.0.0-M6 的能力远不止 `OpenAiChatModel.call()`：
- `ChatClient` (流式 API) — 未使用
- `@Tool` 注解实现 Function Calling — 未使用
- Structured Output / Bean Output Converter — 未使用
- `Advisor` 模式（如 `PromptChatMemoryAdvisor`）— 未使用
- 内置的 Embedding 客户端 — 未使用（手动用 RestTemplate 调 DeepSeek API）

本质上，Spring AI 在这里只被当作了 HTTP 代理。

---

### 中优先级 (MEDIUM) — 建议修复

#### M1. Lucene 索引启动时数据丢失风险

**位置**: `RagServiceImpl.java:66`

```java
config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
```

每次应用重启，Lucene 索引文件被清空重建。虽然 `KnowledgeBaseSeeder` 会在 `ApplicationReadyEvent` 时重建索引，但如果有人在重建前发起搜索，将返回空结果。应使用 `CREATE_OR_APPEND`。

#### M2. Embedding 失败静默降级

**位置**: `RagServiceImpl.java:245-281`, `getOrGenerateEmbedding()`

当 DeepSeek Embedding API 调用失败时，方法返回 `null`，调用方没有任何告警。多次连续失败也不会触发任何通知。

#### M3. Conversation 历史仅保留最近 3 轮

**位置**: `AiChatServiceImpl.java:44`

3 轮对话对于涉及收藏、下单的多步骤操作可能不够用。用户说"收藏那个楼盘"时需要依赖前文推断"那个"是哪个。

#### M4. 依赖版本偏旧

| 依赖 | 当前版本 | 建议版本 | 说明 |
|------|---------|---------|------|
| Spring AI | 1.0.0-M6 | 1.0.0 | M6 是 2024 年的里程碑版本，正式版已发布 |
| Apache Lucene | 9.12.0 | 10.x | 大版本更新，性能改进 |
| Apache Tika | 2.9.2 | 3.x | 大版本更新 |

---

### 低优先级 (LOW) — 可选改进

#### L1. Gson 与 Jackson 共存

Spring Boot 已内置 Jackson（`spring-boot-starter-json`），但 `AiController` 和 `AiChatServiceImpl` 额外引入 Gson 进行 JSON 操作。可统一使用 Jackson 减少依赖。

#### L2. 无 LLM 调用的重试机制

如果 DeepSeek API 临时不可用（限流、网络波动等），没有重试逻辑，直接抛出异常。

#### L3. 系统提示词过长且不可配置

600+ 字符的 system prompt 是硬编码常量。可考虑外部化配置。

---

## 四、正面评价

1. **前后端分层清晰**: Controller → Service → Mapper 三层架构、Pinia 状态管理、TypeScript 类型定义，结构规范。
2. **两阶段下单设计**: 预览 → 确认的两阶段模式防止误操作，`pending_order` 表设计合理。
3. **Embedding 本地缓存**: SHA-256 哈希 + 文件缓存避免重复调用 Embedding API，节省成本。
4. **会话管理完善**: CRUD 完整、消息分页、级联删除、500 条上限保护。
5. **文档处理流程完整**: 上传 → 解析 → 分块 → 嵌入 → 索引的 pipeline 设计清晰。
6. **前端 UX 细节到位**: 打字动画、重试按钮、展开/收起长消息、自动滚动、快捷问题芯片。
7. **用户上下文自动注入**: 自动将用户姓名、手机号注入 system prompt，避免 LLM 反复询问。
8. **域名限制**: system prompt 明确限制只能回答购房相关问题。

---

## 五、问题优先级汇总

| 编号 | 级别 | 问题 | 位置 |
|------|------|------|------|
| C1 | CRITICAL | 向量检索完全失效，方法永远返回 null | `RagServiceImpl.extractEmbeddingFromDoc()` |
| C2 | CRITICAL | API Key 硬编码在 application.yml 中 | `application.yml:18` |
| H1 | HIGH | 意图检测是手工关键字匹配，非真正的 Function Calling | `AiChatServiceImpl.detectAndExecuteFunction()` |
| H2 | HIGH | 函数调用路径中 RAG 上下文被浪费 | `AiChatServiceImpl.executeChat()` |
| H3 | HIGH | AI 模块测试覆盖率为 0% | `src/test/` |
| H4 | HIGH | Spring AI 仅作为 HTTP 代理使用 | `AiChatServiceImpl` 整体 |
| M1 | MEDIUM | Lucene 索引启动时 CREATE 模式清空数据 | `RagServiceImpl.init()` |
| M2 | MEDIUM | Embedding 失败静默降级无告警 | `RagServiceImpl.getOrGenerateEmbedding()` |
| M3 | MEDIUM | 对话历史仅 3 轮，多步骤操作可能不够 | `AiChatServiceImpl.MAX_CONVERSATION_ROUNDS` |
| M4 | MEDIUM | 依赖版本偏旧 | `pom.xml` |
| L1 | LOW | Gson 与 Jackson 共存 | 多处 |
| L2 | LOW | LLM 调用无重试机制 | `AiChatServiceImpl.executeChat()` |
| L3 | LOW | System prompt 硬编码 | `AiChatServiceImpl.SYSTEM_PROMPT` |

---

## 六、总体评价

当前 AI 助手的实现是一个**可用的 MVP**，但存在几处需要尽快修复的缺陷。

最核心的问题是 **C1（向量检索失效）**：花成本调用了 DeepSeek Embedding API、存储了向量、构建了混合检索框架，但向量检索部分实际上没有产生任何效果。所幸 BM25 关键词检索仍在正常工作，所以 RAG 功能并非完全不可用，只是检索精度低于预期。

**H1（手工意图匹配）** 是架构层面的技术债。虽然关键字匹配在当前有限的几个操作（收藏、下单）上勉强可用，但随着功能增加，这种方式的脆弱性和维护成本会线性增长。

建议的修复顺序：**C1 → C2 → H1 → H3 → M1 → H4 → M2 → M3 → 其他**。
