<template>
  <div class="orders-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单记录</span>
        </div>
      </template>
      
      <el-table :data="orders" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单编号" width="200" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="amount" label="申购金额" width="150">
          <template #default="{ row }">
            {{ formatMoney(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="shares" label="份额" width="150">
          <template #default="{ row }">
            {{ row.shares?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="annualReturnRate" label="年化收益" width="100">
          <template #default="{ row }">
            <span style="color: #F56C6C;">{{ row.annualReturnRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="expectedIncome" label="预计收益" width="120">
          <template #default="{ row }">
            <span style="color: #67C23A;">{{ formatMoney(row.expectedIncome) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderTime" label="下单时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.orderTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0" 
              type="danger" 
              size="small" 
              @click="handleCancel(row)"
            >
              撤单
            </el-button>
            <el-button type="primary" size="small" text @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchOrders"
        @current-change="fetchOrders"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ currentOrder.productName }}</el-descriptions-item>
        <el-descriptions-item label="申购金额">{{ formatMoney(currentOrder.amount) }}</el-descriptions-item>
        <el-descriptions-item label="份额">{{ currentOrder.shares?.toFixed(2) || '0.00' }}</el-descriptions-item>
        <el-descriptions-item label="年化收益率">{{ currentOrder.annualReturnRate }}%</el-descriptions-item>
        <el-descriptions-item label="期限">{{ currentOrder.termDays }}天</el-descriptions-item>
        <el-descriptions-item label="预计收益">{{ formatMoney(currentOrder.expectedIncome) }}</el-descriptions-item>
        <el-descriptions-item label="实际收益">{{ formatMoney(currentOrder.actualIncome) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentOrder.status)">{{ currentOrder.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(currentOrder.orderTime) }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ formatTime(currentOrder.confirmTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ currentOrder.maturityDate || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrderList, getOrderDetail, cancelOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatMoney, formatTime, getStatusTagType } from '@/utils/format'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const currentOrder = ref({})

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    orders.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要撤销该订单吗？', '提示', {
      type: 'warning'
    })
    await cancelOrder(order.id)
    ElMessage.success('撤单成功')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const showDetail = async (order) => {
  try {
    const res = await getOrderDetail(order.id)
    currentOrder.value = res.data
    detailVisible.value = true
  } catch (e) {
    currentOrder.value = order
    detailVisible.value = true
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.orders-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
