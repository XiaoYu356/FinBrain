<template>
  <div class="admin-orders">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <div class="filters">
            <el-input v-model="filterUserId" placeholder="用户ID" style="width: 100px; margin-right: 10px;" clearable />
            <el-select v-model="filterStatus" placeholder="订单状态" style="width: 120px; margin-right: 10px;" clearable>
              <el-option label="待确认" :value="0" />
              <el-option label="已确认" :value="1" />
              <el-option label="已取消" :value="2" />
              <el-option label="已赎回" :value="3" />
              <el-option label="已到期" :value="4" />
              <el-option label="已结算" :value="5" />
            </el-select>
            <el-button type="primary" @click="fetchOrders">搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="orders" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" :index="indexMethod" />
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderTime" label="下单时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.orderTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="handleView(row)">详情</el-button>
            <el-button 
              v-if="row.status === 0" 
              size="small" 
              type="primary" 
              @click="handleConfirm(row)"
            >确认</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchOrders"
        @current-change="fetchOrders"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="订单详情" width="500px">
      <el-descriptions :column="1" border v-if="currentOrder">
        <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentOrder.username }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ currentOrder.productName }}</el-descriptions-item>
        <el-descriptions-item label="产品代码">{{ currentOrder.productCode }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatMoney(currentOrder.amount) }}</el-descriptions-item>
        <el-descriptions-item label="份额">{{ currentOrder.shares }}</el-descriptions-item>
        <el-descriptions-item label="年化收益率">{{ currentOrder.annualReturnRate }}%</el-descriptions-item>
        <el-descriptions-item label="期限">{{ currentOrder.termDays }}天</el-descriptions-item>
        <el-descriptions-item label="预计收益">{{ formatMoney(currentOrder.expectedIncome) }}</el-descriptions-item>
        <el-descriptions-item label="实际收益">{{ formatMoney(currentOrder.actualIncome) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getOrderStatusTagType(currentOrder.status)">{{ currentOrder.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(currentOrder.orderTime) }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ formatTime(currentOrder.confirmTime) }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ currentOrder.maturityDate }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrderList, getAdminOrderDetail, confirmOrder } from '@/api/admin'
import { formatMoney, formatTime, getOrderStatusTagType } from '@/utils/format'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterUserId = ref('')
const filterStatus = ref(null)
const dialogVisible = ref(false)
const currentOrder = ref(null)

const indexMethod = (index) => {
  return (pageNum.value - 1) * pageSize.value + index + 1
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterUserId.value) params.userId = filterUserId.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    const res = await getAdminOrderList(params)
    orders.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleView = async (row) => {
  try {
    const res = await getAdminOrderDetail(row.id)
    currentOrder.value = res.data
    dialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const handleConfirm = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要确认订单 "${row.orderNo}" 吗？确认后将从用户冻结资金中扣除并开始计算收益。`,
      '确认订单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await confirmOrder(row.id)
    ElMessage.success('订单确认成功')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error(e.response?.data?.message || '订单确认失败')
    }
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filters {
  display: flex;
  align-items: center;
}
</style>
