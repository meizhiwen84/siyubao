const TOKEN_KEY = 'sxjw-admin-token'
const DEVICE_KEY = 'sxjw-admin-device'

export function getToken() {
  const t = localStorage.getItem(TOKEN_KEY)
  return t ? String(t).trim() : ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token ? String(token).trim() : '')
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getOrCreateDeviceId() {
  const cur = localStorage.getItem(DEVICE_KEY)
  if (cur && String(cur).trim()) return String(cur).trim()
  const id = `web-${Math.random().toString(16).slice(2)}${Date.now().toString(16)}`
  localStorage.setItem(DEVICE_KEY, id)
  return id
}

export async function apiFetch(path, options = {}) {
  const token = getToken()
  const headers = new Headers(options.headers || {})
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (!headers.has('Accept')) headers.set('Accept', 'application/json')
  const res = await fetch(path, { ...options, headers })
  const json = await res.json().catch(() => ({}))
  if (!res.ok) {
    const msg = (json && (json.message || json.error)) ? (json.message || json.error) : res.statusText
    const err = new Error(msg || '请求失败')
    err.status = res.status
    err.body = json
    throw err
  }
  return json
}

export async function login(username, password) {
  const body = {
    username: username ? String(username).trim() : '',
    password: password ? String(password).trim() : '',
    deviceId: getOrCreateDeviceId()
  }
  const res = await fetch('/api/public/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body)
  })
  const json = await res.json().catch(() => ({}))
  if (!res.ok || !json || !json.success) {
    throw new Error((json && json.message) || '登录失败')
  }
  if (json.token) setToken(json.token)
  return json
}

export async function me() {
  return apiFetch('/api/me', { method: 'GET' })
}

export async function plans() {
  const res = await fetch('/api/public/plans', { method: 'GET', headers: { Accept: 'application/json' } })
  const json = await res.json().catch(() => ({}))
  if (!res.ok || !json || !json.success) {
    throw new Error((json && json.message) || '获取套餐失败')
  }
  return json
}

export async function adminPlans() {
  return apiFetch('/api/admin/plans', { method: 'GET' })
}

export async function adminPlanCreate(payload) {
  return apiFetch('/api/admin/plans', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload || {}) })
}

export async function adminPlanUpdate(id, payload) {
  return apiFetch(`/api/admin/plans/${encodeURIComponent(id)}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload || {}) })
}

export async function adminUsers({ keyword, page, size }) {
  const url =
    `/api/admin/users/list?keyword=${encodeURIComponent(keyword || '')}` +
    `&page=${encodeURIComponent(page ?? 0)}` +
    `&size=${encodeURIComponent(size ?? 20)}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminUserSetEnabled(id, enabled) {
  return apiFetch(`/api/admin/users/${encodeURIComponent(id)}/enabled`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled: !!enabled })
  })
}

export async function adminUserSetEnabledByUserNo(userNo, enabled) {
  return apiFetch(`/api/admin/users/by-userNo/${encodeURIComponent(userNo)}/enabled`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled: !!enabled })
  })
}

export async function adminUserResetPassword(id, newPassword) {
  const payload = {}
  if (newPassword != null && String(newPassword).trim()) payload.newPassword = String(newPassword).trim()
  return apiFetch(`/api/admin/users/${encodeURIComponent(id)}/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminUserResetPasswordByUserNo(userNo, newPassword) {
  const payload = {}
  if (newPassword != null && String(newPassword).trim()) payload.newPassword = String(newPassword).trim()
  return apiFetch(`/api/admin/users/by-userNo/${encodeURIComponent(userNo)}/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminGrantSubscription({ userId, userNo, planId, durationDays }) {
  const payload = { planId }
  if (userId != null) payload.userId = userId
  if (userNo != null) payload.userNo = userNo
  if (durationDays != null) payload.durationDays = durationDays
  return apiFetch('/api/admin/subscriptions/grant', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminDevices({ userId, userNo, keyword, page, size }) {
  let url = `/api/admin/devices/list?keyword=${encodeURIComponent(keyword || '')}`
  if (userId != null) url += `&userId=${encodeURIComponent(userId)}`
  if (userNo != null) url += `&userNo=${encodeURIComponent(userNo)}`
  if (page != null) url += `&page=${encodeURIComponent(page)}`
  if (size != null) url += `&size=${encodeURIComponent(size)}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminDeviceRevoke(id) {
  return apiFetch(`/api/admin/devices/${encodeURIComponent(id)}/revoke`, { method: 'POST' })
}

export async function adminDevicesRevokeOthers({ userId, userNo, keepId }) {
  const payload = { keepId }
  if (userId != null) payload.userId = userId
  if (userNo != null) payload.userNo = userNo
  return apiFetch('/api/admin/devices/revoke-others', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminDevicesRevokeAll({ userId, userNo }) {
  const payload = {}
  if (userId != null) payload.userId = userId
  if (userNo != null) payload.userNo = userNo
  return apiFetch('/api/admin/devices/revoke-all', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminSubscriptionActive(userIdOrUserNo, isUserNo = false) {
  const paramName = isUserNo ? 'userNo' : 'userId'
  return apiFetch(`/api/admin/subscriptions/active?${paramName}=${encodeURIComponent(userIdOrUserNo ?? '')}`, { method: 'GET' })
}

export async function adminSubscriptionRenew({ userId, userNo, addDays }) {
  const payload = { addDays }
  if (userId != null) payload.userId = userId
  if (userNo != null) payload.userNo = userNo
  return apiFetch('/api/admin/subscriptions/renew', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminSubscriptionUpgrade({ userId, userNo, planId }) {
  const payload = { planId }
  if (userId != null) payload.userId = userId
  if (userNo != null) payload.userNo = userNo
  return apiFetch('/api/admin/subscriptions/upgrade', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminLogs({ keyword, page, size }) {
  const url =
    `/api/admin/logs/list?keyword=${encodeURIComponent(keyword || '')}` +
    `&page=${encodeURIComponent(page ?? 0)}` +
    `&size=${encodeURIComponent(size ?? 20)}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminSettingsGet() {
  return apiFetch('/api/admin/settings/get', { method: 'GET' })
}

export async function adminSettingsUpdate(payload) {
  return apiFetch('/api/admin/settings/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload || {})
  })
}

export async function adminOrders({ status, keyword, page, size }) {
  const url =
    `/api/admin/orders/list?status=${encodeURIComponent(status || '')}` +
    `&keyword=${encodeURIComponent(keyword || '')}` +
    `&page=${encodeURIComponent(page ?? 0)}` +
    `&size=${encodeURIComponent(size ?? 20)}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminOrderApprove(id) {
  return apiFetch(`/api/admin/orders/${encodeURIComponent(id)}/approve`, { method: 'POST' })
}

export async function adminOrderReject(id, remark) {
  const payload = {}
  if (remark != null && String(remark).trim()) payload.remark = String(remark).trim()
  return apiFetch(`/api/admin/orders/${encodeURIComponent(id)}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminQrCodesList() {
  return apiFetch('/api/admin/qr-codes/list', { method: 'GET' })
}

export async function adminQrCodeCreate(payload) {
  return apiFetch('/api/admin/qr-codes/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload || {})
  })
}

export async function adminQrCodeUpdate(id, payload) {
  return apiFetch(`/api/admin/qr-codes/${encodeURIComponent(id)}/update`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload || {})
  })
}

export async function adminQrCodeDelete(id) {
  return apiFetch(`/api/admin/qr-codes/${encodeURIComponent(id)}/delete`, {
    method: 'POST'
  })
}

export async function adminUpload(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiFetch('/api/admin/upload', {
    method: 'POST',
    body: formData
  })
}

export async function adminUserDailyUsage(userIdOrUserNo, page = 1, size = 30, isUserNo = false) {
  const paramName = isUserNo ? 'userNo' : 'userId'
  const url = `/api/admin/user-stats/daily-usage?${paramName}=${encodeURIComponent(userIdOrUserNo)}&page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminUserSubscriptions(userIdOrUserNo, page = 1, size = 20, isUserNo = false) {
  const paramName = isUserNo ? 'userNo' : 'userId'
  const url = `/api/admin/user-stats/subscriptions?${paramName}=${encodeURIComponent(userIdOrUserNo)}&page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminAnnouncementsList({ page, size }) {
  const url = `/api/admin/announcements/list?page=${encodeURIComponent(page ?? 0)}&size=${encodeURIComponent(size ?? 20)}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminAnnouncementCreate(payload) {
  return apiFetch('/api/admin/announcements/create', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
}

export async function adminAnnouncementUpdate(id, payload) {
  return apiFetch(`/api/admin/announcements/${encodeURIComponent(id)}/update`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
}

export async function adminAnnouncementDelete(id) {
  return apiFetch(`/api/admin/announcements/${encodeURIComponent(id)}/delete`, { method: 'POST' })
}

export async function api(method, path, body) {
  const options = { method: method || 'GET' }
  if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
    options.headers = { 'Content-Type': 'application/json' }
    options.body = JSON.stringify(body)
  }
  const json = await apiFetch(path, options)
  if (!json.success) {
    throw new Error(json.message || '请求失败')
  }
  return json
}
