<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409EFF;">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
          <div class="stat-footer">今日新增: {{ stats.todayNewUsers || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67C23A;">
              <el-icon :size="24"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
              <div class="stat-label">总订单数</div>
            </div>
          </div>
          <div class="stat-footer">今日: {{ stats.todayOrders || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #E6A23C;">
              <el-icon :size="24"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatMoney(stats.totalAssets) }}</div>
              <div class="stat-label">平台总资产</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #F56C6C;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatMoney(stats.totalProfit) }}</div>
              <div class="stat-label">累计收益</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <div class="action-item" @click="$router.push('/admin/users')">
              <el-icon :size="32" color="#409EFF"><User /></el-icon>
              <span>用户管理</span>
            </div>
            <div class="action-item" @click="$router.push('/admin/products')">
              <el-icon :size="32" color="#67C23A"><Goods /></el-icon>
              <span>产品管理</span>
            </div>
            <div class="action-item" @click="$router.push('/admin/orders')">
              <el-icon :size="32" color="#E6A23C"><Document /></el-icon>
              <span>订单管理</span>
            </div>
            <div class="action-item" @click="$router.push('/admin/assets')">
              <el-icon :size="32" color="#F56C6C"><Wallet /></el-icon>
              <span>资产管理</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>交易统计</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="总申购金额">{{ formatMoney(stats.totalOrderAmount) }}</el-descriptions-item>
            <el-descriptions-item label="今日申购金额">{{ formatMoney(stats.todayOrderAmount) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStatistics } from '@/api/admin'

const stats = ref({})

const formatMoney = (value) => {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const fetchStatistics = async () => {
  try {
    const res = await getStatistics()
    stats.value = res.data
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchStatistics()
})
</script>

<style scoped>
.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
}

.stat-info {
  margin-left: 16px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-footer {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}

.quick-actions {
  display: flex;
  justify-content: space-around;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: 20px;
  border-radius: 8px;
  transition: background 0.3s;
}

.action-item:hover {
  background: #f5f7fa;
}

.action-item span {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}
</style>
