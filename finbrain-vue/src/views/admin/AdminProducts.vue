<template>
  <div class="admin-products">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>产品管理</span>
          <el-button type="primary" @click="handleAdd">添加产品</el-button>
        </div>
      </template>
      <el-table :data="products" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" :index="indexMethod" />
        <el-table-column prop="productCode" label="产品代码" width="120" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="productType" label="风险等级" width="90">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.productType)">{{ row.productType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="annualReturnRate" label="年化收益率" width="110">
          <template #default="{ row }">
            <span style="color: #F56C6C;">{{ row.annualReturnRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="minAmount" label="起购金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.minAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="termDays" label="期限(天)" width="90" />
        <el-table-column prop="saleStatus" label="销售状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getSaleTagType(row.saleStatus)">
              {{ getSaleText(row.saleStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.saleStatus === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">
              {{ row.saleStatus === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchProducts"
        @current-change="fetchProducts"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑产品' : '添加产品'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="产品代码" prop="productCode">
          <el-input v-model="form.productCode" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" />
        </el-form-item>
        <el-form-item label="风险等级" prop="productType">
          <el-select v-model="form.productType" style="width: 100%;">
            <el-option label="R1 - 低风险" value="R1" />
            <el-option label="R2 - 中低风险" value="R2" />
            <el-option label="R3 - 中风险" value="R3" />
            <el-option label="R4 - 中高风险" value="R4" />
            <el-option label="R5 - 高风险" value="R5" />
          </el-select>
        </el-form-item>
        <el-form-item label="年化收益率" prop="annualReturnRate">
          <el-input-number v-model="form.annualReturnRate" :precision="2" :min="0" :max="100" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="起购金额" prop="minAmount">
          <el-input-number v-model="form.minAmount" :precision="2" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="期限(天)" prop="termDays">
          <el-input-number v-model="form.termDays" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="产品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminProductList, addProduct, updateProduct, updateProductSaleStatus, deleteProduct } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const products = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref()

const form = reactive({
  productCode: '',
  productName: '',
  productType: 'R2',
  annualReturnRate: 3.5,
  minAmount: 10000,
  termDays: 90,
  description: ''
})

const rules = {
  productCode: [{ required: true, message: '请输入产品代码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productType: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  annualReturnRate: [{ required: true, message: '请输入年化收益率', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入起购金额', trigger: 'blur' }],
  termDays: [{ required: true, message: '请输入期限', trigger: 'blur' }]
}

const formatMoney = (value) => {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const indexMethod = (index) => {
  return (pageNum.value - 1) * pageSize.value + index + 1
}

const getRiskTagType = (type) => {
  const types = { R1: 'success', R2: 'success', R3: 'warning', R4: 'danger', R5: 'danger' }
  return types[type] || 'info'
}

const getSaleTagType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'warning' }
  return types[status] || 'info'
}

const getSaleText = (status) => {
  const texts = { 0: '停售', 1: '在售', 2: '售罄' }
  return texts[status] || '未知'
}

const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await getAdminProductList({ pageNum: pageNum.value, pageSize: pageSize.value })
    products.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.productCode = ''
  form.productName = ''
  form.productType = 'R2'
  form.annualReturnRate = 3.5
  form.minAmount = 10000
  form.termDays = 90
  form.description = ''
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await addProduct(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchProducts()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.saleStatus === 1 ? 0 : 1
  const action = newStatus === 0 ? '下架' : '上架'
  
  try {
    await ElMessageBox.confirm(`确定要${action}该产品吗？`, '提示', { type: 'warning' })
    await updateProductSaleStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该产品吗？', '提示', { type: 'warning' })
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
