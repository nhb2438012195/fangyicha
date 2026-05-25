<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { propertyApi } from '../../api/property'
import type { ChartDataItem } from '../../types'
import * as echarts from 'echarts'

const loading = ref(true)

/** ECharts 容器引用 */
const chartByLocation = ref<HTMLDivElement | null>(null)
const chartByType = ref<HTMLDivElement | null>(null)
const chartByFloor = ref<HTMLDivElement | null>(null)

/** ECharts 实例缓存 */
const chartInstances: echarts.ECharts[] = []

onMounted(async () => {
  let locationRes, typeRes, floorRes
  try {
    [locationRes, typeRes, floorRes] = await Promise.all([
      propertyApi.getVacancyByLocation(),
      propertyApi.getVacancyByType(),
      propertyApi.getVacancyByFloor()
    ])
  } catch (error) {
    console.error('获取图表数据失败:', error)
  } finally {
    loading.value = false
  }

  await nextTick()

  renderLocationChart(locationRes?.data || [])
  renderTypeChart(typeRes?.data || [])
  renderFloorChart(floorRes?.data || [])

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.forEach(instance => instance.dispose())
})

function handleResize() {
  chartInstances.forEach(instance => instance.resize())
}

/** 渲染位置-空置率柱状图 */
function renderLocationChart(data: ChartDataItem[]) {
  if (!chartByLocation.value) return
  if (!data || data.length === 0) {
    showEmpty(chartByLocation.value)
    return
  }

  const myChart = echarts.init(chartByLocation.value)
  chartInstances.push(myChart)
  myChart.setOption({
    title: { text: '各区域空置率对比', left: 'center', textStyle: { fontSize: 16, color: '#4a3728' } },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0]
        return `${item.name}<br/>空置率: ${item.value}%<br/>总户数: ${data[item.dataIndex]?.totalUnits || '-'}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.name || '未知'),
      axisLabel: {
        rotate: 45,
        fontSize: 10,
        interval: 0,
        formatter: (val: string) => {
          if (val.length > 6) {
            const half = Math.ceil(val.length / 2)
            return val.slice(0, half) + '\n' + val.slice(half)
          }
          return val
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '空置率(%)',
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      type: 'bar',
      data: data.map(d => Number(d.vacancyRate).toFixed(1)),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#f5a623' },
          { offset: 1, color: '#f8c065' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      label: {
        show: true,
        position: 'top',
        formatter: (p: any) => p.value + '%',
        fontSize: 11
      }
    }]
  })
}

/** 渲染户型-空置率分组柱状图 */
function renderTypeChart(data: ChartDataItem[]) {
  if (!chartByType.value) return
  if (!data || data.length === 0) {
    showEmpty(chartByType.value)
    return
  }

  const myChart = echarts.init(chartByType.value)
  chartInstances.push(myChart)
  myChart.setOption({
    title: { text: '各户型空置率对比', left: 'center', textStyle: { fontSize: 16, color: '#4a3728' } },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0]
        return `${item.name}<br/>空置率: ${item.value}%<br/>楼盘数: ${data[item.dataIndex]?.propertyCount || '-'}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.name || '未知'),
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '空置率(%)',
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      type: 'bar',
      data: data.map(d => Number(d.vacancyRate).toFixed(1)),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#34a853' },
          { offset: 1, color: '#81c784' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      label: {
        show: true,
        position: 'top',
        formatter: (p: any) => p.value + '%',
        fontSize: 11
      }
    }]
  })
}

/** 渲染楼层-空置率散点图 */
function renderFloorChart(data: ChartDataItem[]) {
  if (!chartByFloor.value) return
  if (!data || data.length === 0) {
    showEmpty(chartByFloor.value)
    return
  }

  const myChart = echarts.init(chartByFloor.value)
  chartInstances.push(myChart)
  myChart.setOption({
    title: { text: '楼层与空置率关系', left: 'center', textStyle: { fontSize: 16, color: '#4a3728' } },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.data[2]}<br/>平均楼层: ${params.data[0]}<br/>空置率: ${params.data[1]}%<br/>户型: ${params.data[3] || '-'}`
      }
    },
    grid: { left: '4%', right: '4%', bottom: '12%', top: '15%', containLabel: true },
    xAxis: {
      type: 'value',
      name: '平均楼层',
      nameLocation: 'center',
      nameGap: 25
    },
    yAxis: {
      type: 'value',
      name: '空置率(%)',
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      type: 'scatter',
      symbolSize: (val: number[]) => {
        return Math.max(8, Math.min(30, val[1] * 0.8))
      },
      data: data.map(d => [d.avgFloor || 0, Number(d.vacancyRate).toFixed(1), d.propertyName || '', d.floorPlanType || '']),
      itemStyle: {
        color: new echarts.graphic.RadialGradient(0.5, 0.5, 0.5, [
          { offset: 0, color: '#f8c065' },
          { offset: 1, color: '#f5a623' }
        ])
      },
      markLine: {
        data: [{ type: 'average', name: '平均空置率' }],
        label: { formatter: '平均: {c}%' }
      }
    }]
  })
}

function showEmpty(container: HTMLDivElement) {
  container.innerHTML = '<div class="chart-empty">暂无数据</div>'
}

/** 导出图表为 PNG */
function exportChart(el: HTMLElement | null) {
  if (!el) return
  const canvas = el.querySelector('canvas')
  if (canvas) {
    const link = document.createElement('a')
    link.download = `chart_${Date.now()}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
  }
}
</script>

<template>
  <div class="analytics-page">
    <div class="page-header">
      <h2 class="page-title">空置率关联分析</h2>
      <p class="page-desc">洞察楼盘空置与区域、户型、楼层的关系</p>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(480px, 1fr)); gap: 16px;">
          <el-skeleton-item variant="card" style="aspect-ratio: 16/9; min-height: 300px;" v-for="i in 2" :key="i" />
          <el-skeleton-item variant="card" style="aspect-ratio: 16/9; min-height: 300px; grid-column: 1 / -1;" />
        </div>
      </template>
    </el-skeleton>

    <div v-if="!loading" class="chart-grid">
      <!-- 位置-空置率柱状图 -->
      <el-card shadow="never" class="chart-card">
        <div class="chart-header">
          <h3>按区域统计</h3>
          <el-button text size="small" @click="exportChart(chartByLocation)">导出PNG</el-button>
        </div>
        <div ref="chartByLocation" class="chart-container"></div>
      </el-card>

      <!-- 户型-空置率柱状图 -->
      <el-card shadow="never" class="chart-card">
        <div class="chart-header">
          <h3>按户型统计</h3>
          <el-button text size="small" @click="exportChart(chartByType)">导出PNG</el-button>
        </div>
        <div ref="chartByType" class="chart-container"></div>
      </el-card>

      <!-- 楼层-空置率散点图 -->
      <el-card shadow="never" class="chart-card full-width">
        <div class="chart-header">
          <h3>楼层与空置率散点图</h3>
          <el-button text size="small" @click="exportChart(chartByFloor)">导出PNG</el-button>
        </div>
        <div ref="chartByFloor" class="chart-container"></div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.analytics-page {
  max-width: 1200px;
}

.page-header {
  margin-bottom: 24px;
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

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 16px;
}

.full-width {
  grid-column: 1 / -1;
}

.chart-card {
  border-radius: 10px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0e8e0;
}

.chart-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #4a3728;
  margin: 0;
}

.chart-container {
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: 300px;
  max-height: 500px;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #b0a090;
  font-size: 14px;
}

@media (max-width: 520px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .chart-container {
    aspect-ratio: 4 / 3;
    min-height: 240px;
  }
}
</style>
