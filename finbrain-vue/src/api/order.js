import { request } from './request'

export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

export function cancelOrder(orderId) {
  return request({
    url: `/order/cancel/${orderId}`,
    method: 'post'
  })
}

export function redeemOrder(orderId) {
  return request({
    url: `/order/redeem/${orderId}`,
    method: 'post'
  })
}

export function getOrderList(params) {
  return request({
    url: '/order/list',
    method: 'get',
    params
  })
}

export function getOrderDetail(orderId) {
  return request({
    url: `/order/${orderId}`,
    method: 'get'
  })
}
