import api from './index'
import type { ApiResult, Developer, DeveloperDashboard } from '../types'

/** 开发商相关 API */
export const developerApi = {
  /** 获取开发商列表 */
  list(params: { keyword?: string; page?: number; pageSize?: number }): Promise<ApiResult<any>> {
    return api.get('/developers', { params }).then(res => res.data)
  },

  /** 获取开发商详情 */
  getById(id: number): Promise<ApiResult<Developer>> {
    return api.get(`/developers/${id}`).then(res => res.data)
  },

  /** 更新公司信息 */
  updateProfile(data: Partial<Developer>): Promise<ApiResult<void>> {
    return api.put('/developers/profile', data).then(res => res.data)
  },

  /** 获取仪表盘数据 */
  getDashboard(): Promise<ApiResult<DeveloperDashboard>> {
    return api.get('/developers/dashboard').then(res => res.data)
  }
}
