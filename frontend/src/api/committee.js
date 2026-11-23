import request from '../utils/request'

/** 委员会：分页 + 多条件组合查询 */
export function getCommittees(params) {
  return request.get('/committees', { params })
}

/** 委员会统计：总数 / 在任 / 已换届 / 已解散 / 按类别分组 / 任期 90 天内到期 / 会议 / 决议聚合统计 */
export function getCommitteeStats() {
  return request.get('/committees/stats')
}

/** 按委员会类别汇总统计 */
export function getCommitteeStatsByType() {
  return request.get('/committees/stats/by-type')
}

/** 按月会议趋势统计 */
export function getCommitteeStatsByMonth() {
  return request.get('/committees/stats/by-month')
}

/** 委员会详情 */
export function getCommitteeById(id) {
  return request.get(`/committees/${id}`)
}

/** 新增委员会（默认在任、第 1 届） */
export function createCommittee(data) {
  return request.post('/committees', data)
}

/** 修改委员会 */
export function updateCommittee(id, data) {
  return request.put(`/committees/${id}`, data)
}

/** 删除委员会（同时删除其会议与决议） */
export function deleteCommittee(id) {
  return request.delete(`/committees/${id}`)
}

/** 批量删除委员会 */
export function batchDeleteCommittees(ids) {
  return request.delete('/committees/batch', { data: ids })
}

/** 换届：生成新一届记录（届次 +1），原记录改为已换届 */
export function renewCommittee(id, data) {
  return request.post(`/committees/${id}/renew`, data || {})
}

/** 解散：只有在任的委员会能解散 */
export function dissolveCommittee(id) {
  return request.post(`/committees/${id}/dissolve`)
}

/** 导出委员会 Excel（按当前查询条件） */
export function exportCommittees(params) {
  return request.get('/committees/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadCommitteeTemplate() {
  return request.get('/committees/template', { responseType: 'blob' })
}

/** 会议：分页 + 多条件组合查询 */
export function getCommitteeMeetings(params) {
  return request.get('/committee-meetings', { params })
}

/** 查某委员会的全部会议（按会议日期降序） */
export function getMeetingsByCommittee(committeeId) {
  return request.get(`/committee-meetings/by-committee/${committeeId}`)
}

/** 新增会议记录（只有在任的委员会才能新增） */
export function createCommitteeMeeting(data) {
  return request.post('/committee-meetings', data)
}

/** 修改会议记录 */
export function updateCommitteeMeeting(id, data) {
  return request.put(`/committee-meetings/${id}`, data)
}

/** 删除会议记录（同时删除其决议） */
export function deleteCommitteeMeeting(id) {
  return request.delete(`/committee-meetings/${id}`)
}

/** 决议：分页 + 多条件组合查询（含只看逾期） */
export function getCommitteeResolutions(params) {
  return request.get('/committee-resolutions', { params })
}

/** 查某会议的全部决议（按决议期限升序） */
export function getResolutionsByMeeting(meetingId) {
  return request.get(`/committee-resolutions/by-meeting/${meetingId}`)
}

/** 新增决议（默认待执行） */
export function createCommitteeResolution(data) {
  return request.post('/committee-resolutions', data)
}

/** 修改决议（只有未结束的决议能修改） */
export function updateCommitteeResolution(id, data) {
  return request.put(`/committee-resolutions/${id}`, data)
}

/** 删除决议 */
export function deleteCommitteeResolution(id) {
  return request.delete(`/committee-resolutions/${id}`)
}

/** 开始执行：待执行 → 执行中 */
export function startCommitteeResolution(id) {
  return request.post(`/committee-resolutions/${id}/start`)
}

/** 完成：执行中 → 已完成 */
export function completeCommitteeResolution(id) {
  return request.post(`/committee-resolutions/${id}/complete`)
}

/** 取消：待执行/执行中 → 已取消 */
export function cancelCommitteeResolution(id) {
  return request.post(`/committee-resolutions/${id}/cancel`)
}
