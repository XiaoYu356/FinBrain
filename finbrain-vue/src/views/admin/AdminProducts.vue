<template>
  <div class="admin-products">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>产品管理</span>
          <div class="header-actions">
            <el-button type="success" @click="showImportDialog = true">导入产品</el-button>
            <el-button type="primary" @click="handleAdd">添加产品</el-button>
          </div>
        </div>
      </template>
      
      <div class="status-statistics">
        <el-tag type="info">草稿: {{ statistics.draft || 0 }}</el-tag>
        <el-tag type="warning">募集期: {{ statistics.fundraising || 0 }}</el-tag>
        <el-tag type="success">存续期: {{ statistics.operating || 0 }}</el-tag>
        <el-tag>已到期: {{ statistics.matured || 0 }}</el-tag>
        <el-tag type="danger">已下架: {{ statistics.delisted || 0 }}</el-tag>
      </div>
      
      <el-table :data="products" v-loading="loading" style="margin-top: 15px;">
        <el-table-column type="index" label="序号" width="60" :index="indexMethod" />
        <el-table-column prop="productCode" label="产品代码" width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="150" />
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
        <el-table-column prop="status" label="产品状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maturityDate" label="到期日" width="110">
          <template #default="{ row }">
            <span v-if="row.maturityDate">{{ row.maturityDate }}</span>
            <span v-else style="color: #909399;">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewLifecycle(row)" link type="primary">日志</el-button>
            <el-button size="small" @click="handleEdit(row)" v-if="row.status === 0">编辑</el-button>
            <el-button size="small" type="success" @click="handlePublish(row)" v-if="row.status === 0">发布</el-button>
            <el-button size="small" type="warning" @click="handleDelist(row)" v-if="row.status === 1 || row.status === 2">下架</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-if="row.status === 0">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑产品' : '添加产品'" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="请输入产品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级" prop="productType">
              <el-select v-model="form.productType" style="width: 100%;">
                <el-option label="R1 - 低风险" value="R1" />
                <el-option label="R2 - 中低风险" value="R2" />
                <el-option label="R3 - 中风险" value="R3" />
                <el-option label="R4 - 中高风险" value="R4" />
                <el-option label="R5 - 高风险" value="R5" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年化收益率" prop="annualReturnRate">
              <el-input-number v-model="form.annualReturnRate" :precision="2" :min="0" :max="100" style="width: 100%;">
                <template #suffix>%</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期限(天)" prop="termDays">
              <el-input-number v-model="form.termDays" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="起购金额" prop="minAmount">
              <el-input-number v-model="form.minAmount" :precision="2" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大金额" prop="maxAmount">
              <el-input-number v-model="form.maxAmount" :precision="2" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="风险评级" prop="riskLevel">
              <el-select v-model="form.riskLevel" style="width: 100%;">
                <el-option label="1级 - 极低风险" :value="1" />
                <el-option label="2级 - 低风险" :value="2" />
                <el-option label="3级 - 中风险" :value="3" />
                <el-option label="4级 - 较高风险" :value="4" />
                <el-option label="5级 - 高风险" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发行机构" prop="issuer">
              <el-input v-model="form.issuer" placeholder="请输入发行机构" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="募集开始日" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" style="width: 100%;" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="募集结束日" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" placeholder="选择日期" style="width: 100%;" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品到期日" prop="maturityDate">
              <el-date-picker v-model="form.maturityDate" type="date" placeholder="选择日期" style="width: 100%;" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="产品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入产品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="lifecycleDialogVisible" title="产品生命周期日志" width="800px">
      <el-table :data="lifecycleLogs" v-loading="lifecycleLoading">
        <el-table-column prop="fromStatusName" label="原状态" width="100">
          <template #default="{ row }">
            <span v-if="row.fromStatusName">{{ row.fromStatusName }}</span>
            <span v-else style="color: #909399;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="toStatusName" label="新状态" width="100" />
        <el-table-column prop="triggerType" label="触发方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.triggerType === 'AUTO' ? 'info' : 'success'" size="small">
              {{ row.triggerType === 'AUTO' ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerReason" label="触发原因" min-width="150" />
        <el-table-column prop="operatorName" label="操作人" width="100">
          <template #default="{ row }">
            <span v-if="row.operatorName">{{ row.operatorName }}</span>
            <span v-else style="color: #909399;">系统</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="showImportDialog" title="导入产品数据" width="500px">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
      >
        <template #trigger>
          <el-button type="primary">选择文件</el-button>
        </template>
        <template #tip>
          <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { 
  getAdminProductList, 
  addProduct, 
  updateProduct, 
  publishProduct,
  delistProduct,
  getProductLifecycle,
  getProductStatistics,
  deleteProduct,
  importProducts
} from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatMoney, getRiskTagType } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const products = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref()
const uploadRef = ref()
const selectedFile = ref(null)
const showImportDialog = ref(false)

const statistics = ref({
  draft: 0,
  fundraising: 0,
  operating: 0,
  matured: 0,
  delisted: 0
})

const lifecycleDialogVisible = ref(false)
const lifecycleLogs = ref([])
const lifecycleLoading = ref(false)

const form = reactive({
  productName: '',
  productType: 'R2',
  annualReturnRate: 3.5,
  minAmount: 10000,
  maxAmount: null,
  termDays: 90,
  riskLevel: 2,
  issuer: '',
  startDate: '',
  endDate: '',
  maturityDate: '',
  description: ''
})

const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productType: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  annualReturnRate: [{ required: true, message: '请输入年化收益率', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入起购金额', trigger: 'blur' }],
  termDays: [{ required: true, message: '请输入期限', trigger: 'blur' }]
}

const indexMethod = (index) => {
  return (pageNum.value - 1) * pageSize.value + index + 1
}

const getStatusTagType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: '', 4: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '草稿', 1: '募集期', 2: '存续期', 3: '已到期', 4: '已下架' }
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

const fetchStatistics = async () => {
  try {
    const res = await getProductStatistics()
    statistics.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const resetForm = () => {
  form.productName = ''
  form.productType = 'R2'
  form.annualReturnRate = 3.5
  form.minAmount = 10000
  form.maxAmount = null
  form.termDays = 90
  form.riskLevel = 2
  form.issuer = ''
  form.startDate = ''
  form.endDate = ''
  form.maturityDate = ''
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
  Object.assign(form, {
    productName: row.productName,
    productType: row.productType,
    annualReturnRate: row.annualReturnRate,
    minAmount: row.minAmount,
    maxAmount: row.maxAmount,
    termDays: row.termDays,
    riskLevel: row.riskLevel || 2,
    issuer: row.issuer || '',
    startDate: row.startDate || '',
    endDate: row.endDate || '',
    maturityDate: row.maturityDate || '',
    description: row.description || ''
  })
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
      ElMessage.success('添加成功，产品当前为草稿状态')
    }
    dialogVisible.value = false
    fetchProducts()
    fetchStatistics()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm(
      '发布后产品将进入募集期，确定要发布吗？',
      '发布产品',
      { type: 'warning' }
    )
    await publishProduct(row.id)
    ElMessage.success('发布成功，产品已进入募集期')
    fetchProducts()
    fetchStatistics()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleDelist = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入下架原因', '下架产品', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '请输入下架原因'
    })
    await delistProduct(row.id, value)
    ElMessage.success('下架成功')
    fetchProducts()
    fetchStatistics()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleViewLifecycle = async (row) => {
  lifecycleDialogVisible.value = true
  lifecycleLoading.value = true
  try {
    const res = await getProductLifecycle(row.id)
    lifecycleLogs.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    lifecycleLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该产品吗？', '提示', { type: 'warning' })
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    fetchProducts()
    fetchStatistics()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  
  importing.value = true
  try {
    const res = await importProducts(selectedFile.value)
    const data = res.data
    ElMessage.success(`导入完成：成功 ${data.successCount} 条，失败 ${data.failCount} 条`)
    showImportDialog.value = false
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    fetchProducts()
    fetchStatistics()
  } catch (e) {
    console.error(e)
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  fetchProducts()
  fetchStatistics()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.status-statistics {
  display: flex;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}
</style>
