import { request } from './request'

export function getProductList(params) {
  return request({
    url: '/product/list',
    method: 'get',
    params
  })
}

export function searchProducts(data) {
  return request({
    url: '/product/search',
    method: 'post',
    data
  })
}

export function getProductById(id) {
  return request({
    url: `/product/${id}`,
    method: 'get'
  })
}
