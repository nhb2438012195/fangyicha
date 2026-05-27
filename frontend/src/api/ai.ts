import api from './index'
import type { ApiResult, AiSessionDTO, AiMessageDTO, ChatRequest, ChatResponse, KnowledgeDocument } from '../types'

/** AI助手相关 API */
export const aiApi = {
  /** 发送聊天消息 */
  chat(data: ChatRequest): Promise<ApiResult<ChatResponse>> {
    return api.post('/ai/chat', data).then(res => res.data)
  },

  /** 创建新会话 */
  createSession(): Promise<ApiResult<{ id: number }>> {
    return api.post('/ai/sessions').then(res => res.data)
  },

  /** 获取会话列表 */
  getSessions(): Promise<ApiResult<AiSessionDTO[]>> {
    return api.get('/ai/sessions').then(res => res.data)
  },

  /** 删除会话 */
  deleteSession(id: number): Promise<ApiResult<void>> {
    return api.delete(`/ai/sessions/${id}`).then(res => res.data)
  },

  /** 获取会话消息列表 */
  getMessages(sessionId: number, page = 1): Promise<ApiResult<AiMessageDTO[]>> {
    return api.get(`/ai/sessions/${sessionId}/messages`, { params: { page } }).then(res => res.data)
  }
}

/** 知识库相关 API */
export const knowledgeBaseApi = {
  /** 上传文档 */
  upload(file: File): Promise<ApiResult<KnowledgeDocument>> {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/ai/knowledge-base/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => res.data)
  },

  /** 获取文档列表 */
  getDocuments(): Promise<ApiResult<KnowledgeDocument[]>> {
    return api.get('/ai/knowledge-base/files').then(res => res.data)
  },

  /** 纳入知识库 */
  indexDocument(fileId: number): Promise<ApiResult<KnowledgeDocument>> {
    return api.post(`/ai/knowledge-base/${fileId}/index`).then(res => res.data)
  },

  /** 删除文档 */
  deleteDocument(fileId: number): Promise<ApiResult<void>> {
    return api.delete(`/ai/knowledge-base/${fileId}`).then(res => res.data)
  },

  /** 预览文档内容 */
  previewDocument(fileId: number): Promise<ApiResult<{ content: string }>> {
    return api.get(`/ai/knowledge-base/${fileId}/preview`).then(res => res.data)
  }
}
