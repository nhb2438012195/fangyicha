<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { customerApi } from '../../../api/customer'

const emit = defineEmits<{
  change: [locations: string[]]
}>()

const locations = ref<string[]>([])
const loading = ref(true)
const editing = ref(false)
const inputValue = ref('')

const commonLocations = [
  '株洲市', '株洲市天元区', '株洲市芦淞区', '株洲市荷塘区',
  '株洲市石峰区', '株洲市渌口区', '醴陵市', '攸县',
  '广州市', '深圳市', '佛山市', '东莞市',
  '长沙市', '北京市', '上海市', '武汉市',
  '成都市', '杭州市', '南京市', '苏州市',
  '西安市', '重庆市', '珠海市'
]

async function fetchProfile() {
  loading.value = true
  try {
    const res = await customerApi.getProfile()
    const preferred = res.data.preferredLocations
    if (preferred) {
      locations.value = preferred.split(',').map(l => l.trim()).filter(Boolean)
    }
  } catch (error) {
    console.error('获取客户信息失败:', error)
  } finally {
    loading.value = false
  }
}

async function saveLocations(newLocations: string[]) {
  try {
    await customerApi.updatePreferredLocations(newLocations.join(','))
    locations.value = newLocations
    emit('change', newLocations)
    ElMessage.success('偏好区域已更新')
  } catch (error) {
    console.error('保存偏好区域失败:', error)
    ElMessage.error('保存失败，请重试')
  }
}

function addLocation(loc: string) {
  if (!loc.trim()) return
  if (locations.value.includes(loc.trim())) {
    ElMessage.warning('该区域已添加')
    return
  }
  const newLocations = [...locations.value, loc.trim()]
  saveLocations(newLocations)
  inputValue.value = ''
}

function removeLocation(index: number) {
  const newLocations = locations.value.filter((_, i) => i !== index)
  saveLocations(newLocations)
}

function toggleEdit() {
  editing.value = !editing.value
}

onMounted(fetchProfile)
</script>

<template>
  <div class="preferred-location-editor">
    <div class="editor-header">
      <h3 class="editor-title">偏好区域</h3>
      <el-button
        text
        size="small"
        @click="toggleEdit"
      >
        {{ editing ? '完成' : '编辑' }}
      </el-button>
    </div>

    <div class="tags-container" v-loading="loading">
      <!-- 已有标签 -->
      <el-tag
        v-for="(loc, index) in locations"
        :key="index"
        closable
        :disable-transitions="false"
        class="location-tag"
        @close="removeLocation(index)"
      >
        {{ loc }}
      </el-tag>

      <!-- 添加按钮 -->
      <el-popover
        v-if="editing"
        placement="bottom"
        trigger="click"
        :width="320"
        popper-class="location-popover"
      >
        <template #reference>
          <el-button size="small" circle class="add-btn">
            +
          </el-button>
        </template>
        <div class="popover-content">
          <el-input
            v-model="inputValue"
            placeholder="输入区域名称"
            size="small"
            clearable
            @keyup.enter="addLocation(inputValue)"
          />
          <div class="common-locations">
            <span
              v-for="loc in commonLocations"
              :key="loc"
              class="common-location-item"
              :class="{ selected: locations.includes(loc) }"
              @click="addLocation(loc)"
            >
              {{ loc }}
            </span>
          </div>
        </div>
      </el-popover>

      <!-- 空状态 -->
      <span v-if="!loading && locations.length === 0 && !editing" class="empty-tip">
        点击「编辑」设置偏好区域
      </span>
    </div>
  </div>
</template>

<style scoped>
.preferred-location-editor {
  margin-bottom: 20px;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.editor-title {
  font-size: 15px;
  font-weight: 600;
  color: #4a3728;
  margin: 0;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 32px;
}

.location-tag {
  --el-tag-bg-color: rgba(245, 166, 35, 0.1);
  --el-tag-text-color: #f5a623;
  --el-tag-border-color: rgba(245, 166, 35, 0.2);
  --el-tag-hover-color: #d49520;
  cursor: default;
  font-size: 13px;
  padding: 0 10px;
  height: 28px;
  line-height: 26px;
  transition: transform 0.2s;
}

.location-tag:hover {
  transform: translateY(-1px);
}

.add-btn {
  background: rgba(245, 166, 35, 0.1);
  color: #f5a623;
  border: 1px dashed rgba(245, 166, 35, 0.3);
  font-size: 16px;
  transition: all 0.2s;
}

.add-btn:hover {
  background: rgba(245, 166, 35, 0.2);
  transform: scale(1.05);
}

.empty-tip {
  font-size: 13px;
  color: #b0a090;
}
</style>

<style>
.location-popover {
  padding: 12px;
}

.popover-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.common-locations {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.common-location-item {
  display: inline-block;
  padding: 4px 10px;
  font-size: 12px;
  color: #4a3728;
  background: #f5f0ea;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
}

.common-location-item:hover {
  background: #f5a623;
  color: #fff;
}

.common-location-item.selected {
  background: rgba(245, 166, 35, 0.15);
  color: #d49520;
  cursor: not-allowed;
}
</style>
