import api from './index'
import type { ApiResult, Suggestion } from '../types'

/** 建议相关 API */
export const suggestionApi = {
  /** 提交建议（客户） */
  submit(data: Partial<Suggestion>): Promise<ApiResult<void>> {
    return api.post('/suggestions', data).then(res => res.data)
  },

  /** 我的建议列表（客户） */
  getMyList(params: { page?: number; pageSize?: number }): Promise<ApiResult<any>> {
    return api.get('/suggestions/my', { params }).then(res => res.data)
  },

  /** 收到的建议列表（开发商） */
  getReceivedList(params: { page?: number; pageSize?: number }): Promise<ApiResult<any>> {
    return api.get('/suggestions/received', { params }).then(res => res.data)
  },

  /** 回复建议（开发商） */
  reply(id: number, replyContent: string): Promise<ApiResult<void>> {
    return api.put(`/suggestions/${id}/reply`, { replyContent }).then(res => res.data)
  }
}
