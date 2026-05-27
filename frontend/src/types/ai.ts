/** AI助手相关类型 */

/** AI会话 */
export interface AiSession {
  id: number
  customerId: number
  title: string
  createdTime: string
  updatedTime: string
}

/** AI会话列表项DTO */
export interface AiSessionDTO {
  id: number
  title: string
  createdTime: string
  updatedTime: string
  messageCount: number
}

/** AI消息 */
export interface AiMessageDTO {
  id: number
  sessionId: number
  role: 'user' | 'assistant'
  content: string
  messageType: 'text' | 'recommendation' | 'favorites' | 'order_summary'
  metadata: Record<string, any> | null
  createdTime: string
}

/** 聊天请求 */
export interface ChatRequest {
  sessionId: number | null
  message: string
}

/** 聊天回复内容 */
export interface ReplyContent {
  type: string
  text: string
  data: Record<string, any> | null
}

/** 聊天响应 */
export interface ChatResponse {
  sessionId: number
  reply: ReplyContent
}

/** 知识库文档 */
export interface KnowledgeDocument {
  id: number
  developerId: number
  filename: string
  storedFilename: string
  fileSize: number
  status: 'uploaded' | 'indexed' | 'error'
  errorMessage: string
  uploadedTime: string
  indexedTime: string
}
