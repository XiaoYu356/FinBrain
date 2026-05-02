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

export function reprocessDocument(id) {
  return request({
    url: `/admin/knowledge/documents/${id}/process`,
    method: 'post'
  })
}
