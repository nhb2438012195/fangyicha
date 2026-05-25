import api from './index'
import type { ApiResult, LoginRequest, LoginResponse, RegisterRequest } from '../types'

/** 认证相关 API */
export const authApi = {
  /** 登录 */
  login(data: LoginRequest): Promise<ApiResult<LoginResponse>> {
    return api.post('/auth/login', data).then(res => res.data)
  },

  /** 注册 */
  register(data: RegisterRequest): Promise<ApiResult<any>> {
    return api.post('/auth/register', data).then(res => res.data)
  },

  /** 检查用户名是否存在 */
  checkUsername(username: string): Promise<ApiResult<{ exists: boolean }>> {
    return api.get('/auth/check-username', { params: { username } }).then(res => res.data)
  },

  /** 获取当前用户信息 */
  getCurrentUser(): Promise<ApiResult<any>> {
    return api.get('/auth/me').then(res => res.data)
  }
}
