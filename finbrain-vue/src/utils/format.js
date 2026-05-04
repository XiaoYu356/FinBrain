export const formatMoney = (value) => {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export const getRiskTagType = (type) => {
  const types = { R1: 'success', R2: 'success', R3: 'warning', R4: 'danger', R5: 'danger' }
  return types[type] || 'info'
}

export const getRiskDesc = (level) => {
  const descs = { C1: '保守型', C2: '谨慎型', C3: '稳健型', C4: '进取型', C5: '激进型' }
  return descs[level] || '保守型'
}

export const getOrderStatusTagType = (status) => {
  const types = { 
    0: 'warning',  // 待确认
    1: 'success',  // 已确认
    2: 'info',     // 已取消
    3: 'primary',  // 已赎回
    4: 'danger',   // 已到期
    5: ''          // 已结算
  }
  return types[status] || 'info'
}

export const getOrderStatusText = (status) => {
  const texts = { 
    0: '待确认', 
    1: '已确认', 
    2: '已取消', 
    3: '已赎回',
    4: '已到期',
    5: '已结算'
  }
  return texts[status] || '未知'
}

export const getProductStatusTagType = (status) => {
  const types = { 
    0: 'info',     // 草稿
    1: 'success',  // 募集期
    2: 'primary',  // 存续期
    3: 'warning',  // 已到期
    4: 'danger'    // 已下架
  }
  return types[status] || 'info'
}

export const getProductStatusText = (status) => {
  const texts = { 
    0: '草稿', 
    1: '募集期', 
    2: '存续期', 
    3: '已到期',
    4: '已下架'
  }
  return texts[status] || '未知'
}

export const getStatusTagType = (status) => {
  return getOrderStatusTagType(status)
}

export const getStatusText = (status) => {
  return getOrderStatusText(status)
}

export const getSaleText = (status) => {
  const texts = { 0: '停售', 1: '在售', 2: '售罄' }
  return texts[status] || '未知'
}

export const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

export const calculateExpectedIncome = (amount, annualRate, days) => {
  const income = (amount || 0) * (annualRate || 0) / 100 * (days || 0) / 365
  return income
}
