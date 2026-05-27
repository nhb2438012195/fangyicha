<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeBaseApi } from '../../api/ai'
import type { KnowledgeDocument } from '../../types'

const loading = ref(false)
const uploadLoading = ref(false)
const documents = ref<KnowledgeDocument[]>([])

/** 预览相关 */
const previewVisible = ref(false)
const previewContent = ref('')
const previewLoading = ref(false)
const previewTitle = ref('')

/** 拖拽上传状态 */
const isDragOver = ref(false)

/** 加载文档列表 */
async function fetchDocuments() {
  loading.value = true
  try {
    const res = await knowledgeBaseApi.getDocuments()
    documents.value = res.data || []
  } catch {
    ElMessage.error('加载文档列表失败')
    documents.value = []
  } finally {
    loading.value = false
  }
}

/** 上传文件 */
async function handleUpload(file?: File) {
  const target = file
  if (!target) return

  const ext = target.name.split('.').pop()?.toLowerCase()
  if (!ext || !['pdf', 'doc', 'docx'].includes(ext)) {
    ElMessage.warning('仅支持 PDF、DOC、DOCX 格式文件')
    return
  }

  if (target.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 10MB')
    return
  }

  uploadLoading.value = true
  try {
    await knowledgeBaseApi.upload(target)
    ElMessage.success('上传成功')
    await fetchDocuments()
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploadLoading.value = false
  }
}

/** 选择文件上传 */
function handleFileSelect() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.pdf,.doc,.docx'
  input.onchange = () => {
    if (input.files?.length) {
      handleUpload(input.files[0])
    }
  }
  input.click()
}

/** 拖拽上传 */
function onDragOver(e: DragEvent) {
  e.preventDefault()
  isDragOver.value = true
}

function onDragLeave() {
  isDragOver.value = false
}

function onDrop(e: DragEvent) {
  e.preventDefault()
  isDragOver.value = false
  if (e.dataTransfer?.files.length) {
    handleUpload(e.dataTransfer.files[0])
  }
}

/** 纳入知识库 */
async function handleIndex(doc: KnowledgeDocument) {
  try {
    await knowledgeBaseApi.indexDocument(doc.id)
    ElMessage.success('已纳入知识库')
    doc.status = 'indexed'
  } catch {
    ElMessage.error('索引失败')
  }
}

/** 删除文档 */
async function handleDelete(doc: KnowledgeDocument) {
  try {
    await ElMessageBox.confirm(`确认删除「${doc.filename}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await knowledgeBaseApi.deleteDocument(doc.id)
    ElMessage.success('已删除')
    documents.value = documents.value.filter(d => d.id !== doc.id)
  } catch {
    // cancelled
  }
}

/** 预览文档 */
async function handlePreview(doc: KnowledgeDocument) {
  previewTitle.value = doc.filename
  previewContent.value = ''
  previewVisible.value = true
  previewLoading.value = true
  try {
    const res = await knowledgeBaseApi.previewDocument(doc.id)
    previewContent.value = res.data?.content || '（无内容）'
  } catch {
    previewContent.value = '加载预览失败'
  } finally {
    previewLoading.value = false
  }
}

/** 格式化文件大小 */
function formatSize(bytes: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/** 格式化时间 */
function formatTime(dateStr: string | undefined): string {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

/** 状态标签类型 */
function statusType(status: string): string {
  switch (status) {
    case 'indexed': return 'success'
    case 'error': return 'danger'
    default: return 'info'
  }
}

/** 状态文本 */
function statusText(status: string): string {
  switch (status) {
    case 'uploaded': return '待索引'
    case 'indexed': return '已索引'
    case 'error': return '失败'
    default: return status
  }
}

onMounted(() => {
  fetchDocuments()
})
</script>

<template>
  <div class="knowledge-base-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">知识库管理</h2>
      <p class="page-desc">上传楼盘资料、户型图册等文档，AI 助手将自动学习并用于回答客户咨询</p>
    </div>

    <!-- 上传区域 -->
    <div
      class="upload-zone"
      :class="{ 'drag-over': isDragOver, 'uploading': uploadLoading }"
      @dragover="onDragOver"
      @dragleave="onDragLeave"
      @drop="onDrop"
      @click="handleFileSelect"
    >
      <div class="upload-icon">
        <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#f5a623" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
          <polyline points="17 8 12 3 7 8" />
          <line x1="12" y1="3" x2="12" y2="15" />
        </svg>
      </div>
      <div class="upload-text">
        <span class="upload-main">点击或拖拽文件到此处上传</span>
        <span class="upload-hint">支持 PDF、DOC、DOCX 格式，单个文件不超过 10MB</span>
      </div>
      <div v-if="uploadLoading" class="upload-progress">
        <span class="upload-spinner" />
        上传中...
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">已上传文档</h3>
        <span class="section-count">共 {{ documents.length }} 个文件</span>
      </div>

      <el-table
        v-loading="loading"
        :data="documents"
        stripe
        style="width: 100%"
        empty-text="暂无文档，请上传"
      >
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="filename-cell">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#f5a623" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
              </svg>
              <span class="filename-text">{{ row.filename }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="100" align="center">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatTime(row.uploadedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="索引时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatTime(row.indexedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button text size="small" type="primary" @click="handlePreview(row)">
                预览
              </el-button>
              <el-button
                v-if="row.status !== 'indexed'"
                text
                size="small"
                type="warning"
                :loading="row.status === 'uploaded' && false"
                @click="handleIndex(row)"
              >
                索引
              </el-button>
              <el-button text size="small" type="danger" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewTitle"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="previewLoading" class="preview-content">
        <pre class="preview-text">{{ previewContent }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.knowledge-base-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #4a3728;
  margin-bottom: 6px;
}

.page-desc {
  font-size: 14px;
  color: #8a7a6a;
  margin: 0;
}

/* Upload Zone */
.upload-zone {
  border: 2px dashed #e8ddd0;
  border-radius: 12px;
  padding: 40px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fdf8f3;
  margin-bottom: 28px;
}

.upload-zone:hover {
  border-color: #f5a623;
  background: #fef7ed;
}

.upload-zone.drag-over {
  border-color: #f5a623;
  background: #fef7ed;
  transform: scale(1.01);
}

.upload-zone.uploading {
  pointer-events: none;
  opacity: 0.7;
}

.upload-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #fef7ed;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.upload-main {
  font-size: 15px;
  font-weight: 600;
  color: #4a3728;
}

.upload-hint {
  font-size: 13px;
  color: #c4b5a5;
}

.upload-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #f5a623;
  font-weight: 500;
}

.upload-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #f5a623;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Section */
.section {
  background: #fdf8f3;
  border-radius: 12px;
  padding: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #4a3728;
  margin: 0;
}

.section-count {
  font-size: 13px;
  color: #c4b5a5;
}

/* Filename cell */
.filename-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filename-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Action buttons */
.action-btns {
  display: flex;
  gap: 4px;
  justify-content: center;
}

/* Preview */
.preview-content {
  max-height: 400px;
  overflow-y: auto;
  min-height: 100px;
}

.preview-text {
  font-size: 14px;
  color: #4a3728;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-family: inherit;
}
</style>
