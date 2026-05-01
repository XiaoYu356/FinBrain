<template>
  <div class="asset-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="asset-card">
          <template #header>
            <div class="card-header">
              <span>资产概览</span>
              <el-button type="primary" size="small" @click="showRechargeDialog">充值</el-button>
            </div>
          </template>
          <div class="asset-overview">
            <div class="asset-item">
              <span class="label">总资产</span>
              <span class="value">{{ formatMoney(asset.totalAsset) }}</span>
            </div>
            <div class="asset-item">
              <span class="label">可用余额</span>
              <span class="value">{{ formatMoney(asset.availableBalance) }}</span>
            </div>
            <div class="asset-item">
              <span class="label">冻结金额</span>
              <span class="value">{{ formatMoney(asset.frozenBalance) }}</span>
            </div>
            <div class="asset-item">
              <span class="label">累计收益</span>
              <span class="value profit">{{ formatMoney(asset.totalProfit) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>资产分布</span>
          </template>
          <div ref="pieChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>持仓明细</span>
          </template>
          <el-table :data="holdings" style="width: 100%" v-loading="loading">
            <el-table-column prop="productName" label="产品名称" />
            <el-table-column prop="productType" label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag :type="getRiskTagType(row.productType)">{{ row.productType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="持仓金额" width="150">
              <template #default="{ row }">
                {{ formatMoney(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="annualReturnRate" label="年化收益率" width="120">
              <template #default="{ row }">
                <span style="color: #F56C6C;">{{ row.annualReturnRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="termDays" label="期限" width="80">
              <template #default="{ row }">
                {{ row.termDays }}天
              </template>
            </el-table-column>
            <el-table-column prop="expectedIncome" label="预计收益" width="150">
              <template #default="{ row }">
                <span style="color: #67C23A;">{{ formatMoney(row.expectedIncome) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="danger" size="small" @click="handleRedeem(row)">赎回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && holdings.length === 0" description="暂无持仓" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="rechargeVisible" title="账户充值" width="400px">
      <el-form label-width="80px">
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeAmount" :min="100" :max="10000000" :step="1000" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="快捷金额">
          <el-button-group>
            <el-button size="small" @click="rechargeAmount = 1000">1000</el-button>
            <el-button size="small" @click="rechargeAmount = 5000">5000</el-button>
            <el-button size="small" @click="rechargeAmount = 10000">1万</el-button>
            <el-button size="small" @click="rechargeAmount = 50000">5万</el-button>
            <el-button size="small" @click="rechargeAmount = 100000">10万</el-button>
          </el-button-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="rechargeLoading" @click="handleRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { getAssetInfo, recharge } from '@/api/account'
import { getOrderList, redeemOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatMoney, getRiskTagType, calculateExpectedIncome } from '@/utils/format'

const pieChartRef = ref()
const loading = ref(false)
const rechargeVisible = ref(false)
const rechargeAmount = ref(10000)
const rechargeLoading = ref(false)

const asset = ref({
  totalAsset: 0,
  availableBalance: 0,
  frozenBalance: 0,
  totalProfit: 0
})

const holdings = ref([])

const showRechargeDialog = () => {
  rechargeAmount.value = 10000
  rechargeVisible.value = true
}

const handleRecharge = async () => {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    ElMessage.warning('请输入正确的充值金额')
    return
  }
  
  rechargeLoading.value = true
  try {
    const res = await recharge(rechargeAmount.value)
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    rechargeLoading.value = false
  }
}

const handleRedeem = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要赎回「${row.productName}」吗？赎回金额: ${formatMoney(row.amount)}`,
      '赎回确认',
      {
        confirmButtonText: '确定赎回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await redeemOrder(row.id)
    ElMessage.success('赎回成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const initPieChart = () => {
  const chart = echarts.init(pieChartRef.value)
  
  const totalAsset = Number(asset.value.totalAsset) || 0
  const availableBalance = Number(asset.value.availableBalance) || 0
  const frozenBalance = Number(asset.value.frozenBalance) || 0
  const investedAmount = totalAsset - availableBalance - frozenBalance
  
  const pieData = []
  
  if (investedAmount > 0) {
    pieData.push({ value: investedAmount, name: '投资金额', itemStyle: { color: '#409EFF' } })
  }
  if (availableBalance > 0) {
    pieData.push({ value: availableBalance, name: '可用余额', itemStyle: { color: '#67C23A' } })
  }
  if (frozenBalance > 0) {
    pieData.push({ value: frozenBalance, name: '冻结金额', itemStyle: { color: '#E6A23C' } })
  }
  
  if (pieData.length === 0) {
    pieData.push({ value: 1, name: '暂无资产', itemStyle: { color: '#909399' } })
  }
  
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '资产分布',
      type: 'pie',
      radius: '50%',
      data: pieData
    }]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAssetInfo()
    asset.value = res.data
    
    const orderRes = await getOrderList({ pageNum: 1, pageSize: 100 })
    holdings.value = (orderRes.data.records || [])
      .filter(o => o.status === 1)
      .map(o => ({
        ...o,
        expectedIncome: calculateExpectedIncome(o.amount, o.annualReturnRate, o.termDays)
      }))
    
    initPieChart()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.asset-container {
  padding: 0;
}

.asset-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.asset-overview {
  padding: 10px 0;
}

.asset-item {
  display: flex;
  justify-content: space-between;
  padding: 15px 0;
  border-bottom: 1px solid #EBEEF5;
}

.asset-item:last-child {
  border-bottom: none;
}

.asset-item .label {
  color: #909399;
  font-size: 14px;
}

.asset-item .value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.asset-item .value.profit {
  color: #67C23A;
}
</style>
