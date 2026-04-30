<template>
  <div class="admin-assets">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>资产管理</span>
          <el-input v-model="keyword" placeholder="搜索用户名/姓名/手机号" style="width: 250px;" clearable @clear="fetchAssets" @keyup.enter="fetchAssets">
            <template #append>
              <el-button icon="Search" @click="fetchAssets" />
            </template>
          </el-input>
        </div>
      </template>
      <el-table :data="assets" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" :index="indexMethod" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="riskLevel" label="风险等级" width="90">
          <template #default="{ row }">
            <el-tag>{{ row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAsset" label="总资产" width="140">
          <template #default="{ row }">
            {{ formatMoney(row.totalAsset) }}
          </template>
        </el-table-column>
        <el-table-column prop="availableBalance" label="可用余额" width="140">
          <template #default="{ row }">
            {{ formatMoney(row.availableBalance) }}
          </template>
        </el-table-column>
        <el-table-column prop="frozenBalance" label="冻结金额" width="140">
          <template #default="{ row }">
            {{ formatMoney(row.frozenBalance) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalProfit" label="累计收益" width="140">
          <template #default="{ row }">
            <span :style="{ color: row.totalProfit >= 0 ? '#F56C6C' : '#67C23A' }">
              {{ formatMoney(row.totalProfit) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchAssets"
        @current-change="fetchAssets"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAssetList } from '@/api/admin'

const loading = ref(false)
const assets = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const formatMoney = (value) => {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const indexMethod = (index) => {
  return (pageNum.value - 1) * pageSize.value + index + 1
}

const fetchAssets = async () => {
  loading.value = true
  try {
    const res = await getAssetList({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    assets.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAssets()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
