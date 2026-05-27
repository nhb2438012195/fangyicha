<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomComponent
} from 'echarts/components'
import type { PriceHistoryItem } from '../../../types'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent])

const props = defineProps<{
  data: PriceHistoryItem[]
  loading: boolean
}>()

const chartHeight = ref(400)

function updateHeight() {
  chartHeight.value = window.innerWidth < 768 ? 300 : 400
}

onMounted(() => {
  window.addEventListener('resize', updateHeight)
  updateHeight()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateHeight)
})

const sortedData = computed(() => {
  return [...props.data].sort((a, b) =>
    new Date(a.recordDate).getTime() - new Date(b.recordDate).getTime()
  )
})

const dates = computed(() =>
  sortedData.value.map(d => d.recordDate.substring(0, 7))
)

const option = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255,255,255,0.95)',
    borderColor: '#e8ddd0',
    borderWidth: 1,
    textStyle: { color: '#4a3728', fontSize: 12 },
    formatter: function (params: any[]) {
      if (!params || params.length === 0) return ''
      let html = `<div style="font-weight:600;margin-bottom:6px;color:#4a3728">${params[0].axisValue}</div>`
      params.forEach((p: any) => {
        html += `<div style="display:flex;justify-content:space-between;gap:16px;font-size:13px">
          <span style="color:#8a7a6a">${p.marker} ${p.seriesName}</span>
          <span style="font-weight:500;color:#4a3728">¥${Number(p.value).toLocaleString()}</span>
        </div>`
      })
      return html
    }
  },
  legend: {
    data: ['单价 (元/㎡)', '总价 (万元)'],
    bottom: 0,
    textStyle: { color: '#8a7a6a', fontSize: 12 },
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8
  },
  grid: {
    left: 60,
    right: 20,
    top: 20,
    bottom: 40
  },
  dataZoom: [
    {
      type: 'inside',
      start: 0,
      end: 100,
      minValueSpan: 6
    },
    {
      type: 'slider',
      height: 20,
      bottom: 20,
      borderColor: '#e8ddd0',
      fillerColor: 'rgba(245, 166, 35, 0.15)',
      handleStyle: { color: '#f5a623' },
      textStyle: { color: '#8a7a6a', fontSize: 10 }
    }
  ],
  xAxis: {
    type: 'category',
    data: dates.value,
    boundaryGap: false,
    axisLine: { lineStyle: { color: '#e8ddd0' } },
    axisLabel: {
      color: '#8a7a6a',
      fontSize: 11,
      rotate: dates.value.length > 12 ? 45 : 0
    },
    splitLine: { show: false }
  },
  yAxis: [
    {
      type: 'value',
      name: '单价 (元/㎡)',
      nameTextStyle: { color: '#8a7a6a', fontSize: 11 },
      axisLabel: {
        color: '#8a7a6a',
        fontSize: 11,
        formatter: (v: number) => v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toString()
      },
      splitLine: { lineStyle: { color: '#f0ebe5', type: 'dashed' } },
      axisLine: { show: false }
    },
    {
      type: 'value',
      name: '总价 (万元)',
      nameTextStyle: { color: '#8a7a6a', fontSize: 11 },
      axisLabel: {
        color: '#8a7a6a',
        fontSize: 11,
        formatter: (v: number) => (v / 10000).toFixed(0) + '万'
      },
      splitLine: { show: false },
      axisLine: { show: false }
    }
  ],
  series: [
    {
      name: '单价 (元/㎡)',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#f5a623' },
      itemStyle: { color: '#f5a623' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(245, 166, 35, 0.25)' },
            { offset: 1, color: 'rgba(245, 166, 35, 0.02)' }
          ]
        }
      },
      data: sortedData.value.map(d => d.pricePerSqm)
    },
    {
      name: '总价 (万元)',
      type: 'line',
      smooth: true,
      symbol: 'diamond',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#34a853' },
      itemStyle: { color: '#34a853' },
      yAxisIndex: 1,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(52, 168, 83, 0.2)' },
            { offset: 1, color: 'rgba(52, 168, 83, 0.02)' }
          ]
        }
      },
      data: sortedData.value.map(d => d.totalPrice)
    }
  ]
}))
</script>

<template>
  <div class="price-trend-chart">
    <el-skeleton :loading="loading" animated>
      <template #template>
        <div style="height: 400px; display: flex; align-items: center; justify-content: center;">
          <el-skeleton-item variant="rect" style="width: 90%; height: 90%; border-radius: 8px;" />
        </div>
      </template>
    </el-skeleton>

    <div v-if="!loading && data.length === 0" class="empty-chart">
      <el-empty description="暂无价格历史数据" :image-size="80" />
    </div>

    <div v-if="!loading && data.length > 0" class="chart-wrapper">
      <v-chart :option="option" autoresize :style="{ height: chartHeight + 'px' }" />
    </div>
  </div>
</template>

<style scoped>
.price-trend-chart {
  padding: 8px 0;
}

.chart-wrapper {
  transition: opacity 0.3s;
}

.empty-chart {
  padding: 40px 0;
}
</style>
