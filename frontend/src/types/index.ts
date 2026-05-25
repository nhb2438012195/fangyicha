/** 统一API响应类型 */
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
  total?: number
  page?: number
  pageSize?: number
}

/** 分页参数 */
export interface PaginationParams {
  page: number
  pageSize: number
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
  role: 'developer' | 'customer'
}

/** 登录响应 */
export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: string
  displayName: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  realName: string
  phone: string
  email: string
  idCard?: string
  agreement: boolean
}

/** 开发商 */
export interface Developer {
  id: number
  companyName: string
  contactPerson: string
  phone: string
  email: string
  address: string
  businessLicense: string
  description: string
  username: string
  status: number
  createdTime: string
  updatedTime: string
}

/** 客户 */
export interface Customer {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  idCard: string
  intention: string
  preferredLocations: string
  budgetMin: number
  budgetMax: number
  urgency: string
  status: number
  createdTime: string
  updatedTime: string
}

/** 房产 */
export interface Property {
  id: number
  developerId: number
  propertyName: string
  location: string
  longitude: number
  latitude: number
  floorMin: number
  floorMax: number
  floorPlanType: string
  totalUnits: number
  vacantUnits: number
  vacancyRate: number
  pricePerSqm: number
  totalPrice: number
  areaSqm: number
  decoration: string
  status: string
  description: string
  imageUrls: string
  createdTime: string
  updatedTime: string
}

/** 房产查询参数 */
export interface PropertyQuery {
  page?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: string
  keyword?: string
  location?: string
  floorMin?: number
  floorMax?: number
  floorPlanType?: string
  priceMin?: number
  priceMax?: number
  totalPriceMin?: number
  totalPriceMax?: number
  vacancyRateMin?: number
  vacancyRateMax?: number
  status?: string
  decoration?: string
  developerId?: number
}

/** 建议/购房意向 */
export interface Suggestion {
  id: number
  customerId: number
  developerId: number
  preferredType: string
  priceMin: number
  priceMax: number
  notes: string
  status: string
  replyContent: string
  createdTime: string
  updatedTime: string
  customerName?: string
  developerName?: string
}

/** 图表数据项 */
export interface ChartDataItem {
  name: string
  vacancyRate: number
  totalUnits?: number
  propertyCount?: number
  avgFloor?: number
  propertyName?: string
  floorPlanType?: string
}

/** 开发商仪表盘数据 */
export interface DeveloperDashboard {
  propertyCount: number
  totalUnits: number
  vacantUnits: number
  avgVacancyRate: number
  onSaleCount: number
  pendingCount: number
  pendingSuggestions: number
  orderCount: number
  pendingOrderCount: number
  paidOrderCount: number
}

/** 客户仪表盘数据 */
export interface CustomerDashboard {
  suggestionCount: number
  pendingCount: number
  orderCount: number
  pendingOrderCount: number
}

/** 订单 */
export interface Order {
  id: number
  orderNo: string
  customerId: number
  developerId: number
  propertyId: number
  propertyName: string
  propertyLocation: string
  floorPlanType: string
  areaSqm: number
  totalPrice: number
  pricePerSqm: number
  status: string
  customerName: string
  customerPhone: string
  developerName: string
  paidTime: string
  completedTime: string
  cancelledTime: string
  cancelReason: string
  createdTime: string
  updatedTime: string
}

/** 订单操作日志 */
export interface OrderLog {
  id: number
  orderId: number
  actorId: number
  actorRole: string
  action: string
  fromStatus: string
  toStatus: string
  detail: string
  createdTime: string
}

/** 订单状态常量 */
export const ORDER_STATUSES = ['待支付', '已支付', '已取消', '已完成'] as const

/** 房产常量选项 */
export const FLOOR_PLAN_TYPES = ['一室一厅', '两室一厅', '三室两厅', '四室两厅', '复式', '别墅'] as const
export const PROPERTY_STATUSES = ['在售', '已售', '待开盘'] as const
export const DECORATION_TYPES = ['毛坯', '简装', '精装', '豪装'] as const
export const URGENCY_OPTIONS = ['一个月内', '三个月内', '半年内', '一年内', '不限'] as const
export const INTENTION_OPTIONS = ['自住', '投资', '改善', '学区', '养老'] as const
export const SUGGESTION_STATUSES = ['待回复', '已回复', '已关闭'] as const
