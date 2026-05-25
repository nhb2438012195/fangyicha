import api from './index'
import type { ApiResult, Order, OrderLog } from '../types'

/** 订单相关 API */
export const orderApi = {
  /** 创建订单（客户） */
  create(propertyId: number): Promise<ApiResult<Order>> {
    return api.post('/orders', { propertyId }).then(res => res.data)
  },

  /** 获取我的订单列表（客户） */
  getMyOrders(params: { status?: string; page?: number; pageSize?: number }): Promise<ApiResult<any>> {
    return api.get('/orders/my', { params }).then(res => res.data)
  },

  /** 获取收到的订单列表（开发商） */
  getReceivedOrders(params: { status?: string; page?: number; pageSize?: number }): Promise<ApiResult<any>> {
    return api.get('/orders/received', { params }).then(res => res.data)
  },

  /** 获取订单详情 */
  getById(id: number): Promise<ApiResult<Order>> {
    return api.get(`/orders/${id}`).then(res => res.data)
  },

  /** 支付订单（客户） */
  pay(id: number): Promise<ApiResult<Order>> {
    return api.put(`/orders/${id}/pay`).then(res => res.data)
  },

  /** 取消订单（客户） */
  cancel(id: number, reason?: string): Promise<ApiResult<Order>> {
    return api.put(`/orders/${id}/cancel`, { reason }).then(res => res.data)
  },

  /** 确认完成（开发商） */
  complete(id: number): Promise<ApiResult<Order>> {
    return api.put(`/orders/${id}/complete`).then(res => res.data)
  },

  /** 获取订单日志 */
  getLogs(id: number): Promise<ApiResult<OrderLog[]>> {
    return api.get(`/orders/${id}/logs`).then(res => res.data)
  },

  /** 获取我的订单统计（客户） */
  getMyStats(): Promise<ApiResult<any>> {
    return api.get('/orders/stats/my').then(res => res.data)
  },

  /** 获取收到的订单统计（开发商） */
  getReceivedStats(): Promise<ApiResult<any>> {
    return api.get('/orders/stats/received').then(res => res.data)
  }
}
