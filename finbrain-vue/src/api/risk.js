import { request } from './request'

export function getRiskQuestions() {
  return request({
    url: '/risk/questions',
    method: 'get'
  })
}

export function submitRiskAssessment(data) {
  return request({
    url: '/risk/submit',
    method: 'post',
    data
  })
}

export function getRiskResult() {
  return request({
    url: '/risk/result',
    method: 'get'
  })
}
