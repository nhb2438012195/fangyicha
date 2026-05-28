# Spring AI 1.0.0-M6 + DeepSeek Function Calling 不兼容技术分析

**分析日期**: 2026-05-28  
**分析对象**: Spring AI `spring-ai-openai` 1.0.0-M6 vs DeepSeek Chat Completions API  
**验证方式**: 反编译 jar + Web 搜索已知 issue + 代码注释分析

---

## 代码证据

`AiChatServiceImpl.java:153-154` 是直接的证据：

```java
// First, check if the user message contains function-calling intent
// This avoids relying on DeepSeek's native function calling which may have API compatibility issues
```

这段注释确认：开发者**尝试过**让 Spring AI M6 调用 DeepSeek 的原生 Function Calling，但遇到了兼容性问题，最终选择了绕过。

以下从技术层面具体分析哪里不兼容。

---

## 不兼容点 #1: `DefaultChatOptions` 静默丢弃 Tool 配置

**这是最可能的根因。**

从 jar 反编译确认，Spring AI M6 的 `OpenAiChatOptions` 实现了 `ToolCallingChatOptions` 接口：

```java
// 反编译自 spring-ai-openai-1.0.0-M6.jar
public class OpenAiChatOptions implements ToolCallingChatOptions {
    private List<FunctionTool> tools;
    private Object toolChoice;
    private Boolean parallelToolCalls;
    // ...
}
```

但如果开发者使用 Spring AI 推荐的 `ChatClient` 流式 API 配置默认选项：

```java
// 这样写，tool calling 会静默失效：
ChatClient.builder()
    .defaultOptions(ChatOptions.builder()  // ← 返回 DefaultChatOptions，不是 ToolCallingChatOptions
        .model("deepseek-chat")
        .temperature(0.7)
        .build())
    .build();
```

`ChatOptions.builder()` 返回的是 `DefaultChatOptions` 实例 — 它**不支持工具调用**。Function definitions 和 tool_choice 在请求构建时被**静默丢弃**，不会报错，只是不生效。

**实际影响**: 开发者配置了 tools，LLM 也支持 function calling，但 Spring AI 的请求体里根本没有 `tools` 字段。没有报错信息，极难排查。这是最典型的"明明配了为什么不工作"场景。

---

## 不兼容点 #2: DeepSeek `tool_choice: "auto"` 不可靠

即便绕过了 #1，DeepSeek 自身也有问题。官方 GitHub Issue [#826](https://github.com/deepseek-ai/DeepSeek-V3/issues/826)：

> 当 `tool_choice: "auto"` 时，模型经常**返回纯文本而不是调用工具**，即使用户消息提供了触发工具调用所需的所有信息。

这导致 Spring AI 的 Function Calling 循环无法启动 — LLM 返回 `finish_reason: "stop"` 而不是 `finish_reason: "tool_calls"`。

**解决方案**是将 `tool_choice` 设为 `"required"`，但代价是：
- 即使用户只是闲聊，LLM 也被迫调用某个工具
- 如果缺少工具参数所需信息，可能返回空响应

---

## 不兼容点 #3: Spring AI 官方文档承认 DeepSeek Function Calling "不稳定"

Spring AI M6 中文文档明确写道：

> **"deepseek-chat 模型的 Function Calling 功能的当前版本不稳定，这可能会导致循环调用或空响应。"**

当官方文档都告诉你"不稳定"时，开发者选择绕过的理由非常充分。

---

## 不兼容点 #4: Tool Choice 的序列化风险

从 jar 反编译确认，`ChatCompletionRequest` 是 Java Record，`toolChoice` 字段用 `Object` 类型承载两种值：

```java
// 场景 A: String
toolChoice = "auto"   → Jackson → "tool_choice": "auto" ✓

// 场景 B: 指定特定函数
toolChoice = ToolChoiceBuilder.FUNCTION("add_favorite")  
    → Jackson → 需要产出 {"type": "function", "function": {"name": "add_favorite"}}
```

场景 B 中，如果 `ToolChoiceBuilder` 返回对象的 Jackson 序列化结果与 DeepSeek 预期格式存在任何差异（多余字段、null 值、命名不匹配），DeepSeek 返回 400。

---

## 不兼容点 #5: Tool Call 响应解析的字段差异

Function Calling 循环中，LLM 返回 `tool_calls`，Spring AI 需要解析 `choices[0].message.tool_calls[]` 中的每个元素。DeepSeek 的结构虽然声称兼容 OpenAI，但在具体字段上可能有差异：

| 字段 | OpenAI 标准 | DeepSeek 可能的差异 |
|------|------------|-------------------|
| `tool_calls[].id` | `call_xxx` | 格式可能不同 |
| `tool_calls[].type` | `"function"` | 可能缺失此字段 |
| `tool_calls[].function.arguments` | JSON 字符串 | 可能是已解析对象 |
| `finish_reason` | `"tool_calls"` | 可能使用旧格式 `"function_call"` |

此外，Function Calling 循环中需要在对话历史插入 `role: "tool"` 的消息。某些 OpenAI 兼容层可能将其错误映射为旧版 `role: "function"`。

---

## 综合判断: 三重不兼容叠加

按发生概率从高到低：

| 优先级 | 不兼容点 | 症状 | 排查难度 |
|--------|---------|------|---------|
| 几乎确定 | `DefaultChatOptions` 静默丢弃 tool 配置 | tool calling 完全不工作，无报错 | 极高 |
| 很可能 | DeepSeek `tool_choice: "auto"` 不触发 | LLM 返回文本而非 tool_calls | 高 |
| 可能 | Tool calls 字段解析差异 | 解析抛异常或结果为空 | 中 |
| 可能 | ToolChoice 序列化不匹配 | 400 错误 | 低（有错误信息） |

**三种不兼容叠加在一起**，使得在 4 个 Sprint 的工期压力下，调试这个问题的投入产出比极差。开发者做了一个务实的工程判断：关键字匹配能实现相同的用户可见效果，且 DeepSeek 的 Function Calling 在当前版本下官方都承认不稳定，绕过是合理选择。

---

## 为什么不直接调 DeepSeek REST API？

一个合理的追问：既然 Spring AI 集成不顺利，为什么不绕过 Spring AI，直接用 `RestTemplate` 手写 HTTP 调用 DeepSeek 的 function calling API？

推测原因：
1. **Spring AI 已经在正常工作** — 正常对话功能通过 `OpenAiChatModel.call()` 跑通了，额外维护一套 HTTP 调用代码增加复杂度
2. **DeepSeek 自身的 function calling 也不稳定** — 即使裸调 REST API，`tool_choice: "auto"` 的可靠性问题（#2）依然存在
3. **关键字匹配在当前需求范围内够用** — 5 个操作（问答、推荐、收藏、查看收藏、下单），每个都有明确的关键字触发模式，`contains()` 能覆盖 90% 的用例

---

## 如果现在（2026年5月）重新评估

随着版本演进，情况已经改善：

1. **Spring AI 1.0.0** 正式版已发布，有了原生 `DeepSeekChatModel`，不再需要 OpenAI 适配器
2. **DeepSeek API** 的 function calling 稳定性经过多个版本迭代已大幅提升
3. **Spring AI 1.1.0-M2** 有专门的 `DeepSeekApi` 包（`org.springframework.ai.deepseek.api`），包含了 `ToolChoiceBuilder`

现在用 Spring AI 1.0.0+ + DeepSeek V3 做 function calling，成功率会比 M6 时代高很多。

---

## Sources

- [Spring AI DeepSeek Chat 官方文档 (M6) - 注明 Function Calling 不稳定](https://www.spring-doc.cn/spring-ai/1.0.0-M6/api_chat_deepseek-chat.html)
- [DeepSeek-V3 Issue #826 - tool_choice: "auto" 不触发函数调用](https://github.com/deepseek-ai/DeepSeek-V3/issues/826)
- [Spring AI Issue #1899 - OpenAiChatOptions toolChoice 类型问题](https://github.com/spring-projects/spring-ai/issues/1899)
- [Spring AI ChatClient 工具调用功能失效问题解析](https://blog.gitcode.com/5b7591042018bb823426bc7b7ea35158.html)
- [DeepSeek API Function Calling 官方文档](https://api-docs.deepseek.com/guides/function_calling)
- [Spring AI与DeepSeek实战四: 系统API调用 (custom options workaround)](https://www.cnblogs.com/zlt2000/p/18824279)
