<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409EFF;">
              <el-icon :size="24"><Wallet /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatMoney(asset.totalAsset) }}</div>
              <div class="stat-label">总资产</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67C23A;">
              <el-icon :size="24"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatMoney(asset.availableBalance) }}</div>
              <div class="stat-label">可用余额</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #E6A23C;">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatMoney(asset.totalProfit) }}</div>
              <div class="stat-label">累计收益</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #F56C6C;">
              <el-icon :size="24"><Lock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatMoney(asset.frozenBalance) }}</div>
              <div class="stat-label">冻结金额</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>快捷入口</span>
          </template>
          <div class="quick-actions">
            <div class="action-item" @click="$router.push('/products')">
              <el-icon :size="32" color="#409EFF"><Goods /></el-icon>
              <span>理财产品</span>
            </div>
            <div class="action-item" @click="$router.push('/chat')">
              <el-icon :size="32" color="#67C23A"><ChatDotRound /></el-icon>
              <span>AI顾问</span>
            </div>
            <div class="action-item" @click="$router.push('/asset')">
              <el-icon :size="32" color="#E6A23C"><Wallet /></el-icon>
              <span>资产总览</span>
            </div>
            <div class="action-item" @click="$router.push('/risk')">
              <el-icon :size="32" color="#F56C6C"><DataAnalysis /></el-icon>
              <span>风险测评</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>风险等级</span>
          </template>
          <div class="risk-info" v-if="userStore.userInfo">
            <el-progress 
              type="dashboard" 
              :percentage="riskPercentage" 
              :color="riskColor"
            >
              <template #default>
                <span class="risk-level">{{ userStore.userInfo.riskLevel || 'C1' }}</span>
                <span class="risk-desc">{{ riskDesc }}</span>
              </template>
            </el-progress>
          </div>
          <el-button type="primary" text @click="$router.push('/risk')">
            重新测评
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>推荐产品</span>
          </template>
          <el-table :data="products" style="width: 100%">
            <el-table-column prop="productName" label="产品名称" />
            <el-table-column prop="productType" label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag :type="getRiskTagType(row.productType)">{{ row.productType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="annualReturnRate" label="年化收益率" width="120">
              <template #default="{ row }">
                <span style="color: #F56C6C;">{{ row.annualReturnRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="termDays" label="期限" width="100">
              <template #default="{ row }">
                {{ row.termDays }}天
              </template>
            </el-table-column>
            <el-table-column prop="minAmount" label="起购金额" width="120">
              <template #default="{ row }">
                {{ formatMoney(row.minAmount) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="handleBuy(row)">购买</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getAssetInfo } from '@/api/account'
import { getProductList } from '@/api/product'
import { useRouter } from 'vue-router'
import { formatMoney, getRiskTagType, getRiskDesc } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const asset = ref({
  totalAsset: 0,
  availableBalance: 0,
  frozenBalance: 0,
  totalProfit: 0
})

const products = ref([])

const riskPercentage = computed(() => {
  const level = userStore.userInfo?.riskLevel || 'C1'
  return (parseInt(level.replace('C', '')) / 5) * 100
})

const riskColor = computed(() => {
  const level = userStore.userInfo?.riskLevel || 'C1'
  const colors = { C1: '#67C23A', C2: '#95D475', C3: '#E6A23C', C4: '#F56C6C', C5: '#F56C6C' }
  return colors[level] || '#67C23A'
})

const riskDesc = computed(() => {
  return getRiskDesc(userStore.userInfo?.riskLevel || 'C1')
})

const handleBuy = (product) => {
  router.push(`/products?id=${product.id}`)
}

const fetchData = async () => {
  try {
    const [assetRes, productRes] = await Promise.all([
      getAssetInfo(),
      getProductList({ pageNum: 1, pageSize: 5 })
    ])
    asset.value = assetRes.data
    products.value = productRes.data.records
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  userStore.fetchUserInfo()
  fetchData()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  height: 100px;
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

.risk-info {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.risk-level {
  font-size: 24px;
  font-weight: 600;
  display: block;
}

.risk-desc {
  font-size: 14px;
  color: #909399;
}
</style>
