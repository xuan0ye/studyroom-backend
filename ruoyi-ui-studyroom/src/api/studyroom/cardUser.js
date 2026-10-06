import request from '@/utils/request'

// 查询优惠卡-会员关联列表
export function listCardUser(query) {
  return request({
    url: '/studyroom/cardUser/list',
    method: 'get',
    params: query
  })
}

// 查询优惠卡-会员关联详细
export function getCardUser(id) {
  return request({
    url: '/studyroom/cardUser/' + id,
    method: 'get'
  })
}

// 新增优惠卡-会员关联
export function addCardUser(data) {
  return request({
    url: '/studyroom/cardUser',
    method: 'post',
    data: data
  })
}

// 修改优惠卡-会员关联
export function updateCardUser(data) {
  return request({
    url: '/studyroom/cardUser',
    method: 'put',
    data: data
  })
}

// 删除优惠卡-会员关联
export function delCardUser(id) {
  return request({
    url: '/studyroom/cardUser/' + id,
    method: 'delete'
  })
}

// 发请假次数: 在用户卡原有请假额度上追加(每张长期卡默认 3 次)
export function grantLeave(data) {
  return request({
    url: '/studyroom/admin/card/leave/grant',
    method: 'post',
    data: data
  })
}

// 修改卡(到期时间 / 已请假次数 / 请假额度)
export function updateAdminCard(data) {
  return request({
    url: '/studyroom/admin/card',
    method: 'put',
    data: data
  })
}
