# Generator State — Iteration 001

## What Was Built
- 购房订单模块（后端+前端）
- 房产详情页（含"立即购买"功能）

## Backend Changes
### 新增文件
- **Entity**: `Order.java`, `OrderLog.java` (MyBatis-Plus entities)
- **Mapper**: `OrderMapper.java`, `OrderLogMapper.java`
- **Service**: `OrderService.java` (接口), `OrderServiceImpl.java` (实现)
- **Controller**: `OrderController.java` (8个API端点)
- **DB Tables**: `purchase_order`, `order_log` (MySQL, 含种子数据)

### 修改文件
- `SecurityConfig.java`: 添加 `/api/orders/**` 认证规则
- `CustomerServiceImpl.java`: 仪表盘增加 orderCount, pendingOrderCount
- `DeveloperServiceImpl.java`: 仪表盘增加 orderCount, pendingOrderCount, paidOrderCount
- `Constants.java`: 添加订单状态常量和实体类型常量
- `data.sql`: 添加建表DDL和种子数据

## Frontend Changes
### 新增文件
- `api/order.ts` — 订单API模块
- `views/customer/PropertyDetailView.vue` — 房产详情页
- `views/customer/OrderListView.vue` — 客户订单列表
- `views/customer/OrderDetailView.vue` — 客户订单详情
- `views/developer/DeveloperOrderListView.vue` — 开发商订单列表
- `views/developer/DeveloperOrderDetailView.vue` — 开发商订单详情

### 修改文件
- `types/index.ts`: 添加 Order, OrderLog, ORDER_STATUSES, 扩展 Dashboard 类型
- `router/index.ts`: 添加5个新路由
- `Sidebar.vue`: 添加订单菜单项（客户和开发商），导入 Tickets 图标
- `PropertySearchView.vue`: 表格添加 @row-click="handleRowClick" 跳转详情
- `WizardView.vue`: 结果卡片添加 @click="router.push(...)" 跳转详情
- `DashboardView.vue` (客户): 增加订单统计卡片和"我的订单"快捷入口，网格改为4列
- `DashboardView.vue` (开发商): 增加3个订单统计卡片，快捷操作增加"订单管理"

## API Endpoints
| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/orders | CUSTOMER | 创建订单 |
| GET | /api/orders/my | CUSTOMER | 我的订单列表 |
| GET | /api/orders/{id} | - | 订单详情 |
| PUT | /api/orders/{id}/pay | CUSTOMER | 支付订单 |
| PUT | /api/orders/{id}/cancel | CUSTOMER | 取消订单 |
| GET | /api/orders/received | DEVELOPER | 收到的订单 |
| PUT | /api/orders/{id}/complete | DEVELOPER | 确认完成 |
| GET | /api/orders/{id}/logs | - | 订单日志 |
| GET | /api/orders/stats/my | CUSTOMER | 订单统计 |
| GET | /api/orders/stats/received | DEVELOPER | 订单统计 |

## State Machine
- null -> 待支付（创建）
- 待支付 -> 已支付（客户支付）
- 待支付 -> 已取消（客户取消）
- 已支付 -> 已完成（开发商确认）

## Known Issues
- 无

## Dev Server
- Frontend: http://localhost:3001 (Vite, port 3000 was in use)
- Backend: http://localhost:8088 (Spring Boot)
- Status: running
- Backend command: `java -jar backend/target/fangyicha-backend-1.0.0.jar`
- Frontend command: `npx vite --host 0.0.0.0 --port 3000` (from frontend/)
