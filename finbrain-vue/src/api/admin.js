import { request } from './request'

export function getUserList(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function getUserDetail(id) {
  return request({
    url: `/admin/users/${id}`,
    method: 'get'
  })
}

export function updateUserStatus(id, status) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function getAdminProductList(params) {
  return request({
    url: '/admin/products',
    method: 'get',
    params
  })
}

export function getProductDetail(id) {
  return request({
    url: `/admin/products/${id}`,
    method: 'get'
  })
}

export function addProduct(data) {
  return request({
    url: '/admin/products',
    method: 'post',
    data
  })
}

export function updateProduct(id, data) {
  return request({
    url: `/admin/products/${id}`,
    method: 'put',
    data
  })
}

export function publishProduct(id) {
  return request({
    url: `/admin/products/${id}/publish`,
    method: 'post'
  })
}

export function delistProduct(id, reason) {
  return request({
    url: `/admin/products/${id}/delist`,
    method: 'post',
    params: { reason }
  })
}

export function getProductLifecycle(id) {
  return request({
    url: `/admin/products/${id}/lifecycle`,
    method: 'get'
  })
}

export function getProductStatistics() {
  return request({
    url: '/admin/products/statistics',
    method: 'get'
  })
}

export function getExpiringProducts(days = 7) {
  return request({
    url: '/admin/products/expiring',
    method: 'get',
    params: { days }
  })
}

export function updateProductSaleStatus(id, saleStatus) {
  return request({
    url: `/admin/products/${id}/sale-status`,
    method: 'put',
    params: { saleStatus }
  })
}

export function deleteProduct(id) {
  return request({
    url: `/admin/products/${id}`,
    method: 'delete'
  })
}

export function getAdminOrderList(params) {
  return request({
    url: '/admin/orders',
    method: 'get',
    params
  })
}

export function getAdminOrderDetail(id) {
  return request({
    url: `/admin/orders/${id}`,
    method: 'get'
  })
}

export function confirmOrder(id) {
  return request({
    url: `/admin/orders/${id}/confirm`,
    method: 'post'
  })
}

export function getAssetList(params) {
  return request({
    url: '/admin/assets',
    method: 'get',
    params
  })
}

export function getStatistics() {
  return request({
    url: '/admin/statistics',
    method: 'get'
  })
}

export function getKnowledgeDocuments(params) {
  return request({
    url: '/admin/knowledge/documents',
    method: 'get',
    params
  })
}

export function addKnowledgeDocument(data) {
  return request({
    url: '/admin/knowledge/documents',
    method: 'post',
    data
  })
}

export function deleteKnowledgeDocument(id) {
  return request({
    url: `/admin/knowledge/documents/${id}`,
    method: 'delete'
  })
}

export function deleteKnowledgeDocuments(ids) {
  return request({
    url: '/admin/knowledge/documents/batch',
    method: 'delete',
    data: ids
  })
}

export function reprocessDocument(id) {
  return request({
    url: `/admin/knowledge/documents/${id}/process`,
    method: 'post'
  })
}

export function importProducts(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/import/products',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function importNavData(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/import/nav',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getImportLogs(params) {
  return request({
    url: '/admin/import/logs',
    method: 'get',
    params
  })
}

export function getOperationLogs(params) {
  return request({
    url: '/admin/audit/logs',
    method: 'get',
    params
  })
}
