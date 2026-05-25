import api from './index'
import type { ApiResult, Customer, CustomerDashboard } from '../types'

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
  }
}
