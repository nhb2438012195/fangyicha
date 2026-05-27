import api from './index'
import type { ApiResult, FavoriteItem } from '../types'

/** 收藏相关 API */
export const favoriteApi = {
  /** 获取收藏列表 */
  getList(): Promise<ApiResult<FavoriteItem[]>> {
    return api.get('/favorites').then(res => res.data)
  },

  /** 添加收藏 */
  add(propertyId: number): Promise<ApiResult<void>> {
    return api.post('/favorites', { propertyId }).then(res => res.data)
  },

  /** 取消收藏 */
  remove(propertyId: number): Promise<ApiResult<void>> {
    return api.delete(`/favorites/${propertyId}`).then(res => res.data)
  },

  /** 查询单个收藏状态 */
  getStatus(propertyId: number): Promise<ApiResult<{ favorited: boolean }>> {
    return api.get(`/favorites/status/${propertyId}`).then(res => res.data)
  },

  /** 批量查询收藏状态 */
  getStatusBatch(propertyIds: number[]): Promise<ApiResult<Record<number, boolean>>> {
    return api.post('/favorites/status/batch', { propertyIds }).then(res => res.data)
  }
}
