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

export const getStatusTagType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return types[status] || 'info'
}

export const getStatusText = (status) => {
  const texts = { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已赎回' }
  return texts[status] || '未知'
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
