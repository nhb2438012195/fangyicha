import api from './index'
import type { ApiResult, Customer, CustomerDashboard, RecommendationItem } from '../types'

/** 客户相关 API */
export const customerApi = {
  /** 获取个人信息 */
  getProfile(): Promise<ApiResult<Customer>> {
    return api.get('/customers/profile').then(res => res.data)
  },

  /** 更新个人信息 */
  updateProfile(data: Partial<Customer>): Promise<ApiResult<void>> {
    return api.put('/customers/profile', data).then(res => res.data)
  },

  /** 获取仪表盘数据 */
  getDashboard(): Promise<ApiResult<CustomerDashboard>> {
    return api.get('/customers/dashboard').then(res => res.data)
  },

  /** 获取个性化房产推荐 */
  getRecommendations(): Promise<ApiResult<RecommendationItem[]>> {
    return api.get('/customers/recommendations').then(res => res.data)
  },

  /** 更新偏好区域 */
  updatePreferredLocations(preferredLocations: string): Promise<ApiResult<void>> {
    return api.put('/customers/profile', { preferredLocations } as any).then(res => res.data)
  }
}
