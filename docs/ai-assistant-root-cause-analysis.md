# AI 助手需求与实现偏差的根因分析

**分析日期**: 2026-05-28  
**依据文档**: 
- `docs/superpowers/specs/2026-05-26-ai-assistant-requirements.md` (需求记录)
- `gan-harness/spec.md` (GAN Harness 规格说明书)
- `docs/superpowers/specs/2026-05-27-favorites-requirements.md` (收藏需求)
- `docs/plans/v1.4-plan.md` (v1.4 实施计划)

---

## 一、需求回顾：规格说明书的要求

GAN Harness 规格说明书 (`gan-harness/spec.md`) 对 Function Calling 和 RAG 检索做出了明确的技术要求：

### 1.1 Function Calling 要求 (Feature #7, #10, #11)

规格说明书明确规定使用 **DeepSeek 的原生 Function Calling (tool_calls)**：

> **Feature #7**: Parse response — if `finish_reason` is `tool_calls`, enter Function Calling loop. Extract function name and arguments from DeepSeek response. Execute the function locally via `FunctionCallService`.

> **Feature #10**: DeepSeek function definitions registered with the chat completion. Two tools defined using DeepSeek's function calling format (OpenAI-compatible).

> **需求记录 2.4**: 收藏房产 — 通过 Function Calling 执行收藏操作。查看收藏 — 通过 Function Calling 查询用户收藏列表。创建订单 — 通过 Function Calling 创建购房订单。

### 1.2 向量检索要求 (Feature #14)

规格说明书明确要求使用 Lucene 的 `KnnFloatVectorQuery` 进行向量检索：

> On search, generate embedding for the query and perform `KnnFloatVectorQuery` with 10 candidates. Combine with BM25 results using weighted scoring (BM25 0.5, vector 0.5).

---

## 二、实际实现：偏差对照

### 2.1 Function Calling → 关键字匹配

| 维度 | 规格要求 | 实际实现 |
|------|---------|---------|
| 意图识别 | DeepSeek `tool_calls` finish_reason | Java `String.contains()` 关键字匹配 |
| 工具注册 | DeepSeek function definitions JSON | 无 — 硬编码在 `detectAndExecuteFunction()` 中 |
| 参数提取 | DeepSeek 从对话中自动提取参数 | 手动 `replace()` 关键词 + 停止词过滤 |
| 对话循环 | LLM 驱动的多轮 function calling loop | 单次关键字检测，无循环 |

### 2.2 向量检索 → 空实现

| 维度 | 规格要求 | 实际实现 |
|------|---------|---------|
| 向量查询 | `KnnFloatVectorQuery` | 未使用 — 改为遍历 BM25 候选手动计算 |
| 向量提取 | Lucene `FloatVectorValues.vectorValue()` | 方法体为空，永远返回 null |
| 混合评分 | BM25(0.5) + Vector(0.5) | BM25(0.5) + 0(0.5) = 纯 BM25 |

### 2.3 Spring AI ChatClient → OpenAiChatModel.call()

| 维度 | 规格要求 | 实际实现 |
|------|---------|---------|
| 对话客户端 | `ChatClient` 流式 API | `OpenAiChatModel.call()` 底层 API |
| Advisors | `PromptChatMemoryAdvisor` | 手动构建 Message 列表 |
| Structured Output | Bean Output Converter | 手动 Gson 解析 |

---

## 三、根因分析

### 根因 #1: Spring AI 1.0.0-M6 + DeepSeek 的 Function Calling 不兼容（核心原因）

**证据链** — 代码注释直接承认了这个问题 (`AiChatServiceImpl.java:153-154`)：

```java
// First, check if the user message contains function-calling intent
// This avoids relying on DeepSeek's native function calling which may have API compatibility issues
```

**技术推断**:

Spring AI 1.0.0-M6（2024 年早期里程碑版本）的 OpenAI 适配器在发送 function calling 请求时，使用了与 DeepSeek 不完全兼容的 API 格式。具体可能的不兼容点：

1. **`tool_choice` 参数格式差异** — Spring AI M6 的 OpenAI adapter 可能使用了 DeepSeek 当时不支持的参数
2. **tool call 响应解析失败** — DeepSeek 返回的 tool_calls 对象结构与 OpenAI 有微妙差异，导致 Spring AI 解析抛异常
3. **function role message 处理** — Function Calling 循环需要插入 `role: "tool"` 的消息，Spring AI M6 对此的支持可能不完善

**为什么 keyword 方案被选中**：

面对 Function Calling 不工作的困境，开发者在限期压力下选择了"让它先跑起来"的策略 — 用 `contains()` 关键字检测快速实现了相同的用户可见效果。这是一种务实的工程决策，但留下了技术债。

### 根因 #2: Lucene FloatVectorValues API 使用不当（代码级 bug）

**证据链** — `RagServiceImpl.java:327-345` 的方法有明显的"未完成"痕迹：

```java
// We need to advance to the correct doc
// FloatVectorValues iterates over docs that have this vector field
// Since many docs may not have the field, we try a simpler approach
```
（方法在此返回 null，没有任何实际提取逻辑）

**技术细节**:

Lucene 9.x 中从 `FloatVectorValues` 提取特定文档向量的正确方式是 `fvv.advance(docIndex)` 配合 `fvv.vectorValue()`。开发者显然查了 API 文档、理解了概念，但 `advance()` 的返回值和边界条件让代码没有完成。

所幸 BM25 检索对于楼盘名称、户型、区域这种结构化关键词场景效果尚可，所以这个 bug 在生产环境中不容易被察觉。

### 根因 #3: GAN Harness 的评估标准偏向 UI/UX

查看 `gan-harness/spec.md` 的评估标准 (Evaluation Criteria)，权重分配为：
- Design Quality: 0.30（视觉、动画、配色）
- Craft: 0.30（交互、加载态、空状态）
- Originality: 0.20（人格化、卡片内嵌范式）
- Functionality: 0.20（端到端流程）

评估检查项聚焦在**可见行为**：气泡样式、动画、空状态、卡片渲染。以下后端问题在评估中不会被检测到：
- 向量检索是否真的在计算相似度
- Function Calling 是 LLM 驱动还是关键字匹配
- Embedding 缓存是否正确加载
- 检索结果是混合计算还是纯 BM25

**换句话说**：Evaluator 审查的是"看得到的"，Generator 在"看不到的"上面走了捷径。

### 根因 #4: 4 个 Sprint 的工期压力

整个 AI 助手（Foundation + RAG + Function Calling + Tika + 知识库 + 聊天窗口 + 会话管理）要在 4 个 Sprint 内完成。当一个核心依赖（Spring AI function calling with DeepSeek）不工作时，没有时间深入调试 — 必须快速找到替代方案。

### 根因 #5: 需求文档措辞存在轻微歧义

需求记录中有这样的表述：

> 收藏房产: 通过 Function Calling 执行收藏操作（用户说"帮我收藏"即可触发）

这里的括号说明"用户说 XX → 触发操作"，对于赶进度的实现者，如果遇到原生 Function Calling 不工作，"用户说 XX → 触发操作"用 `contains()` 也能满足字面描述。但这只是助推因素，GAN Harness 规格说明书已经用技术细节（`finish_reason`, `tool_calls`, function definitions JSON）纠正了这种歧义。

---

## 四、事件的因果链

```
需求明确要求 Function Calling
          │
          ▼
规格说明书设计了 LLM tool_calls + 循环
          │
          ▼
Spring AI 1.0.0-M6 + DeepSeek function calling 不兼容
（tool_call 参数或返回格式不匹配）
          │
          ▼
4 Sprint 紧排期，没有时间深入调试
          │
          ▼
开发者选择关键字匹配替代方案 → "先跑起来"
（代码注释留下了技术决策记录）
          │
          ▼
同时，Lucene FloatVectorValues API 使用遇到障碍
向量提取方法写成空实现 → "先跳过"
          │
          ▼
GAN Evaluator 关注 UI/UX 可见行为
未检测到后端逻辑级别的捷径
          │
          ▼
结果：看起来能用的 AI 助手，实际上功能降级了
（纯 BM25 检索 + 关键字匹配 function calling）
```

---

## 五、关键教训

1. **Function Calling 作为核心架构依赖需要提前验证** — Sprint 1 初期就应该写一个独立的 DeepSeek function calling 集成测试，验证 Spring AI → DeepSeek 的 tool_calls 能否端到端工作。如果早发现不兼容，可以在 Sprint 1 就换方案。

2. **GAN Harness 评估标准需要覆盖后端逻辑** — 当前评估聚焦视觉和交互，未来应增加对检索精度、function calling 正确性的定量评估。

3. **"先跳过"的技术债需要追踪机制** — `extractEmbeddingFromDoc()` 的未完成实现和 function calling 的关键字替代方案都没有 TODO 或 issue 跟踪，导致了"写完就忘了"的情况。

4. **依赖版本选择有实际影响** — Spring AI 1.0.0-M6 是 2024 年早期的里程碑版本。如果用的是更成熟的版本，DeepSeek function calling 的兼容性可能更好，整套实现路径可能完全不同。
