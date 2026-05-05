<template>
  <div class="admin-knowledge">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>知识库文档</span>
          <div class="header-actions">
            <el-button 
              type="danger" 
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              :on-progress="handleUploadProgress"
              :show-file-list="false"
              multiple
              accept=".txt,.md,.pdf,.doc,.docx,.ppt,.pptx,.csv,.jpg,.jpeg,.png,.gif,.bmp,.webp"
            >
              <el-button type="primary">批量上传文档</el-button>
            </el-upload>
          </div>
        </div>
      </template>
      <el-table 
        :data="documents" 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" :index="indexMethod" />
        <el-table-column prop="originalName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切块数" width="80" />
        <el-table-column prop="createTime" label="上传时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="primary" 
              link 
              @click="handleReprocess(row)"
              v-if="row.status === 'failed'"
            >
              重试
            </el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchDocuments"
        @current-change="fetchDocuments"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { deleteKnowledgeDocument, deleteKnowledgeDocuments, reprocessDocument, getKnowledgeDocuments } from '@/api/admin'

const userStore = useUserStore()
const loading = ref(false)
const documents = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const selectedIds = ref([])
let pollingTimer = null
let uploadSuccessCount = 0
let uploadFailCount = 0
let uploadMessageTimer = null

const uploadUrl = computed(() => '/api/admin/knowledge/upload')
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const indexMethod = (index) => {
  return (pageNum.value - 1) * pageSize.value + index + 1
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const getStatusType = (status) => {
  const types = {
    pending: 'info',
    processing: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    failed: '失败'
  }
  return texts[status] || status
}

const fetchDocuments = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeDocuments({ pageNum: pageNum.value, pageSize: pageSize.value })
    documents.value = res.data.records || []
    total.value = res.data.total || 0
    checkPolling()
  } catch (e) {
    console.error(e)
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

const checkPolling = () => {
  const hasProcessing = documents.value.some(doc => 
    doc.status === 'processing' || doc.status === 'pending'
  )
  
  if (hasProcessing && !pollingTimer) {
    pollingTimer = setInterval(fetchDocuments, 3000)
  } else if (!hasProcessing && pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const beforeUpload = (file) => {
  const allowedExts = ['.txt', '.md', '.pdf', '.doc', '.docx', '.ppt', '.pptx', '.csv', '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  
  if (!allowedExts.includes(ext)) {
    ElMessage.error(`文件 ${file.name} 格式不支持，只支持 txt, md, pdf, doc, docx, ppt, pptx, csv, jpg, jpeg, png, gif, bmp, webp 格式的文件`)
    return false
  }
  return true
}

const handleUploadProgress = (event, file) => {
}

const handleUploadSuccess = (response, file, fileList) => {
  if (response.code === 200) {
    uploadSuccessCount++
  } else {
    uploadFailCount++
    ElMessage.error(`${file.name}: ${response.message || '上传失败'}`)
  }
  
  if (uploadMessageTimer) {
    clearTimeout(uploadMessageTimer)
  }
  
  uploadMessageTimer = setTimeout(() => {
    if (uploadSuccessCount > 0) {
      if (uploadSuccessCount === 1) {
        ElMessage.success('文档上传成功，正在处理中...')
      } else {
        ElMessage.success(`成功上传 ${uploadSuccessCount} 个文档，正在处理中...`)
      }
      fetchDocuments()
    }
    
    if (uploadFailCount > 0 && uploadSuccessCount === 0) {
      ElMessage.error('所有文档上传失败')
    } else if (uploadFailCount > 0) {
      ElMessage.warning(`${uploadFailCount} 个文档上传失败`)
    }
    
    uploadSuccessCount = 0
    uploadFailCount = 0
    uploadMessageTimer = null
  }, 500)
}

const handleUploadError = (error, file) => {
  ElMessage.error(`文件 ${file.name} 上传失败`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该文档吗？删除后无法恢复。', '提示', { type: 'warning' })
    await deleteKnowledgeDocument(row.id)
    ElMessage.success('删除成功')
    fetchDocuments()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的文档')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个文档吗？删除后无法恢复。`, 
      '批量删除确认', 
      { type: 'warning' }
    )
    
    await deleteKnowledgeDocuments(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchDocuments()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('批量删除失败')
    }
  }
}

const handleReprocess = async (row) => {
  try {
    await reprocessDocument(row.id)
    ElMessage.success('正在重新处理...')
    fetchDocuments()
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchDocuments()
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
  if (uploadMessageTimer) {
    clearTimeout(uploadMessageTimer)
    uploadMessageTimer = null
  }
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
  align-items: center;
}
</style>
