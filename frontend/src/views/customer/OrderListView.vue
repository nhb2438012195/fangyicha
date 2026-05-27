<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '../../api/order'
import type { Order } from '../../types'
import { ORDER_STATUSES } from '../../types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(true)
const orderList = ref<Order[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  ...ORDER_STATUSES.map(s => ({ value: s, label: s }))
]

async function fetchOrders() {
  loading.value = true
  try {
    const res = await orderApi.getMyOrders({
      status: statusFilter.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    orderList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleStatusFilter(val: string) {
  statusFilter.value = val
  currentPage.value = 1
  fetchOrders()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchOrders()
}

function formatPrice(price: number | null): string {
  if (!price) return '-'
  if (price >= 10000) {
    return '¥' + (price / 10000).toFixed(0) + '万'
  }
  return '¥' + price.toLocaleString()
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    '待支付': 'warning',
    '已支付': 'primary',
    '已完成': 'success',
    '已取消': 'info'
  }
  return map[status] || 'info'
}

async function handlePay(order: Order) {
  try {
    await ElMessageBox.confirm(
      `确认支付订单 ${order.orderNo}？`,
      '支付确认',
      { confirmButtonText: '去支付', cancelButtonText: '取消', type: 'info' }
    )
    await orderApi.pay(order.id)
    ElMessage.success('支付成功！')
    fetchOrders()
  } catch (error: any) {
    if (error === 'cancel' || error?.toString().includes('cancel')) return
    ElMessage.error(error?.message || '支付失败')
  }
}

async function handleCancel(order: Order) {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入取消原因（选填）',
      '取消订单',
      { confirmButtonText: '确认取消', cancelButtonText: '再想想', inputPlaceholder: '取消原因...' }
    )
    await orderApi.cancel(order.id, reason || '客户主动取消')
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error: any) {
    if (error === 'cancel' || error?.toString().includes('cancel')) return
    ElMessage.error(error?.message || '取消失败')
  }
}

function handleAction(cmd: string, order: Order) {
  if (cmd === 'detail') {
    router.push(`/customer/orders/${order.id}`)
  } else if (cmd === 'cancel') {
    handleCancel(order)
  }
}

onMounted(fetchOrders)
</script>

<template>
  <div class="order-list-page">
    <div class="page-header">
      <h2 class="page-title">我的订单</h2>
      <p class="page-desc">查看和管理您的购房订单</p>
    </div>

    <!-- 状态筛选 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <span class="filter-label">订单状态：</span>
        <el-radio-group :model-value="statusFilter" @change="handleStatusFilter">
          <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 订单列表 -->
    <el-card shadow="never" class="list-card">
      <el-table
        :data="orderList"
        v-loading="loading"
        stripe
        style="width: 100%"
        @row-click="(row: Order) => router.push(`/customer/orders/${row.id}`)"
        empty-text="暂无订单记录"
      >
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }">
            <span style="font-family: monospace; font-size: 13px;">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="propertyName" label="房产名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="propertyLocation" label="位置" min-width="180" show-overflow-tooltip />
        <el-table-column prop="totalPrice" label="总价" width="130" align="right">
          <template #default="{ row }">{{ formatPrice(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <!-- 待支付: 突出"去支付"，次要操作放下拉 -->
              <template v-if="row.status === '待支付'">
                <el-button type="primary" size="small" @click.stop="handlePay(row)">
                  去支付
                </el-button>
                <el-dropdown trigger="click" @command="(cmd: string) => handleAction(cmd, row)">
                  <el-button size="small" @click.stop class="more-btn">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="detail">详情</el-dropdown-item>
                      <el-dropdown-item command="cancel">取消</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <!-- 其他状态: 只有详情 -->
              <el-button v-else text type="primary" size="small" @click.stop="router.push(`/customer/orders/${row.id}`)">
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="() => fetchOrders()"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #4a3728;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 14px;
  color: #8a7a6a;
  margin: 0;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  color: #8a7a6a;
  white-space: nowrap;
}

.list-card {
  border-radius: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0 0;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.more-btn {
  padding: 4px 6px;
}
</style>
