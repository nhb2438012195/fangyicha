<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { suggestionApi } from '../../api/suggestion'
import type { Suggestion } from '../../types'

const loading = ref(false)
const suggestionList = ref<Suggestion[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const replyDialogVisible = ref(false)
const currentSuggestion = ref<Suggestion | null>(null)
const replyContent = ref('')
const replyLoading = ref(false)

async function fetchSuggestions() {
  loading.value = true
  try {
    const res = await suggestionApi.getReceivedList({ page: page.value, pageSize: pageSize.value })
    suggestionList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取建议列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  page.value = p
  fetchSuggestions()
}

/** 打开回复对话框 */
function openReplyDialog(suggestion: Suggestion) {
  currentSuggestion.value = suggestion
  replyContent.value = suggestion.replyContent || ''
  replyDialogVisible.value = true
}

/** 回复建议 */
async function handleReply() {
  if (!currentSuggestion.value || !replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replyLoading.value = true
  try {
    await suggestionApi.reply(currentSuggestion.value.id, replyContent.value)
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    fetchSuggestions()
  } catch (error) {
    console.error('回复失败:', error)
  } finally {
    replyLoading.value = false
  }
}

function getStatusType(status: string) {
  switch (status) {
    case '待回复': return 'warning'
    case '已回复': return 'success'
    case '已关闭': return 'info'
    default: return ''
  }
}

onMounted(fetchSuggestions)
</script>

<template>
  <div class="suggestions-page">
    <div class="page-header">
      <h2 class="page-title">客户建议</h2>
      <p class="page-desc">查看和管理客户提交的购房意向，共 {{ total }} 条</p>
    </div>

    <!-- 建议列表 -->
    <el-card shadow="never" class="list-card">
      <el-table :data="suggestionList" v-loading="loading" stripe empty-text="暂无客户建议">
        <el-table-column prop="customerName" label="客户姓名" width="120" />
        <el-table-column prop="preferredType" label="偏好户型" width="120" align="center" />
        <el-table-column label="预算范围" width="180" align="center">
          <template #default="{ row }">
            <span v-if="row.priceMin || row.priceMax">
              ¥{{ (row.priceMin || 0).toLocaleString() }} - ¥{{ (row.priceMax || 0).toLocaleString() }}
            </span>
            <span v-else class="text-muted">不限</span>
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="备注说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replyContent" label="回复内容" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.replyContent">{{ row.replyContent }}</span>
            <span v-else class="text-muted">未回复</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="提交时间" width="170">
          <template #default="{ row }">{{ row.createdTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              :disabled="row.status !== '待回复'"
              @click="openReplyDialog(row)"
            >
              {{ row.status === '待回复' ? '回复' : '已回复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyDialogVisible" title="回复客户" width="500px" :close-on-click-modal="false">
      <div v-if="currentSuggestion" class="reply-context">
        <div class="context-item"><strong>客户：</strong>{{ currentSuggestion.customerName }}</div>
        <div class="context-item"><strong>户型偏好：</strong>{{ currentSuggestion.preferredType || '不限' }}</div>
        <div class="context-item"><strong>备注：</strong>{{ currentSuggestion.notes || '无' }}</div>
      </div>
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="5"
        placeholder="请输入回复内容..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replyLoading" @click="handleReply">确认回复</el-button>
      </template>
    </el-dialog>
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

.list-card {
  border-radius: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0 0;
}

.text-muted {
  color: #b0a090;
}

.reply-context {
  background: #f5f0ea;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.context-item {
  font-size: 13px;
  color: #8a7a6a;
  margin-bottom: 4px;
}

.context-item:last-child {
  margin-bottom: 0;
}
</style>
