import { request } from './request'

export function getAssetInfo() {
  return request({
    url: '/account/my-asset',
    method: 'get'
  })
}

export function getUserAsset(userId) {
  return request({
    url: '/account/asset',
    method: 'get',
    params: { userId }
  })
}

export function recharge(amount) {
  return request({
    url: '/account/recharge',
    method: 'post',
    params: { amount }
  })
}
