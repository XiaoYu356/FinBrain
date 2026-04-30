<template>
  <div class="product-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>理财产品</span>
          <el-button type="primary" @click="showSearch = !showSearch">
            {{ showSearch ? '收起筛选' : '展开筛选' }}
          </el-button>
        </div>
      </template>
      
      <el-form v-show="showSearch" :model="searchForm" inline class="search-form">
        <el-form-item label="风险等级">
          <el-select v-model="searchForm.productType" placeholder="请选择" clearable>
            <el-option label="R1-低风险" value="R1" />
            <el-option label="R2-中低风险" value="R2" />
            <el-option label="R3-中风险" value="R3" />
            <el-option label="R4-中高风险" value="R4" />
            <el-option label="R5-高风险" value="R5" />
          </el-select>
        </el-form-item>
        <el-form-item label="收益率">
          <el-input-number v-model="searchForm.minRate" :min="0" :max="20" :precision="1" placeholder="最低" />
          <span style="margin: 0 8px;">-</span>
          <el-input-number v-model="searchForm.maxRate" :min="0" :max="20" :precision="1" placeholder="最高" />
        </el-form-item>
        <el-form-item label="期限">
          <el-input-number v-model="searchForm.minTerm" :min="0" placeholder="最短" />
          <span style="margin: 0 8px;">-</span>
          <el-input-number v-model="searchForm.maxTerm" :min="0" placeholder="最长" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="产品名称/代码" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="products" v-loading="loading" style="width: 100%">
        <el-table-column prop="productCode" label="产品代码" width="120" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="productType" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.productType)">{{ row.productType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="annualReturnRate" label="年化收益率" width="120">
          <template #default="{ row }">
            <span style="color: #F56C6C; font-weight: 600;">{{ row.annualReturnRate }}%</span>
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
        <el-table-column prop="saleStatus" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.saleStatus === 1 ? 'success' : 'info'">
              {{ row.saleStatus === 1 ? '在售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button type="success" size="small" @click="showBuyDialog(row)" :disabled="row.saleStatus !== 1">
              购买
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchProducts"
        @current-change="fetchProducts"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="产品详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="产品代码">{{ currentProduct.productCode }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ currentProduct.productName }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">{{ currentProduct.productType }}</el-descriptions-item>
        <el-descriptions-item label="年化收益率">{{ currentProduct.annualReturnRate }}%</el-descriptions-item>
        <el-descriptions-item label="期限">{{ currentProduct.termDays }}天</el-descriptions-item>
        <el-descriptions-item label="起购金额">{{ formatMoney(currentProduct.minAmount) }}</el-descriptions-item>
        <el-descriptions-item label="产品描述" :span="2">{{ currentProduct.description }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="buyVisible" title="购买产品" width="400px">
      <el-form :model="buyForm" label-width="80px">
        <el-form-item label="产品名称">
          <span>{{ currentProduct.productName }}</span>
        </el-form-item>
        <el-form-item label="年化收益">
          <span style="color: #F56C6C;">{{ currentProduct.annualReturnRate }}%</span>
        </el-form-item>
        <el-form-item label="起购金额">
          <span>{{ formatMoney(currentProduct.minAmount) }}</span>
        </el-form-item>
        <el-form-item label="购买金额" required>
          <el-input-number v-model="buyForm.amount" :min="Number(currentProduct.minAmount) || 0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="预计收益">
          <span style="color: #67C23A; font-weight: 600;">
            {{ calculateExpectedIncome() }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buyVisible = false">取消</el-button>
        <el-button type="primary" :loading="buyLoading" @click="handleBuy">确认购买</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { searchProducts } from '@/api/product'
import { createOrder } from '@/api/order'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const showSearch = ref(false)
const products = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  productType: '',
  minRate: null,
  maxRate: null,
  minTerm: null,
  maxTerm: null,
  keyword: ''
})

const detailVisible = ref(false)
const buyVisible = ref(false)
const buyLoading = ref(false)
const currentProduct = ref({})
const buyForm = reactive({
  amount: 0
})

const formatMoney = (value) => {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getRiskTagType = (type) => {
  const types = { R1: 'success', R2: 'success', R3: 'warning', R4: 'danger', R5: 'danger' }
  return types[type] || 'info'
}

const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await searchProducts({
      ...searchForm,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    products.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchProducts()
}

const resetSearch = () => {
  Object.assign(searchForm, {
    productType: '',
    minRate: null,
    maxRate: null,
    minTerm: null,
    maxTerm: null,
    keyword: ''
  })
  handleSearch()
}

const showDetail = (product) => {
  currentProduct.value = product
  detailVisible.value = true
}

const showBuyDialog = (product) => {
  currentProduct.value = product
  buyForm.amount = Number(product.minAmount) || 0
  buyVisible.value = true
}

const calculateExpectedIncome = () => {
  const amount = buyForm.amount || 0
  const rate = currentProduct.value.annualReturnRate || 0
  const days = currentProduct.value.termDays || 0
  const income = amount * rate / 100 * days / 365
  return formatMoney(income)
}

const handleBuy = async () => {
  if (!buyForm.amount || buyForm.amount < (currentProduct.value.minAmount || 0)) {
    ElMessage.warning('请输入正确的购买金额')
    return
  }
  buyLoading.value = true
  try {
    await createOrder({
      productId: currentProduct.value.id,
      amount: buyForm.amount
    })
    ElMessage.success('购买成功')
    buyVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    buyLoading.value = false
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.product-list {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
