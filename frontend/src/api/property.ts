import api from './index'
import type { ApiResult, Property, PropertyQuery, ChartDataItem, PriceHistoryItem, PropertyDetail } from '../types'

/** 房产相关 API */
export const propertyApi = {
  /** 多条件分页查询房产 */
  list(params: PropertyQuery): Promise<ApiResult<any>> {
    return api.get('/properties', { params }).then(res => res.data)
  },

  /** 获取房产详情（含开发商名称） */
  getById(id: number): Promise<ApiResult<PropertyDetail>> {
    return api.get(`/properties/${id}`).then(res => res.data)
  },

  /** 获取价格历史 */
  getPriceHistory(id: number, months: number = 24): Promise<ApiResult<PriceHistoryItem[]>> {
    return api.get(`/properties/${id}/price-history`, { params: { months } }).then(res => res.data)
  },

  /** 获取我的房产列表（开发商） */
  getMyList(params: PropertyQuery): Promise<ApiResult<any>> {
    return api.get('/properties/my', { params }).then(res => res.data)
  },

  /** 创建房产（开发商） */
  create(data: Partial<Property>): Promise<ApiResult<void>> {
    return api.post('/properties', data).then(res => res.data)
  },

  /** 更新房产（开发商） */
  update(id: number, data: Partial<Property>): Promise<ApiResult<void>> {
    return api.put(`/properties/${id}`, data).then(res => res.data)
  },

  /** 删除房产（开发商） */
  delete(id: number): Promise<ApiResult<void>> {
    return api.delete(`/properties/${id}`).then(res => res.data)
  },

  /** 空置率-位置统计 */
  getVacancyByLocation(): Promise<ApiResult<ChartDataItem[]>> {
    return api.get('/properties/statistics/vacancy-by-location').then(res => res.data)
  },

  /** 空置率-户型统计 */
  getVacancyByType(): Promise<ApiResult<ChartDataItem[]>> {
    return api.get('/properties/statistics/vacancy-by-type').then(res => res.data)
  },

  /** 楼层-空置率散点 */
  getVacancyByFloor(): Promise<ApiResult<ChartDataItem[]>> {
    return api.get('/properties/statistics/vacancy-by-floor').then(res => res.data)
  }
}
