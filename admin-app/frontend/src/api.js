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

export async function adminUserResetPassword(id, newPassword) {
  const payload = {}
  if (newPassword != null && String(newPassword).trim()) payload.newPassword = String(newPassword).trim()
  return apiFetch(`/api/admin/users/${encodeURIComponent(id)}/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminGrantSubscription({ userId, planId, durationDays }) {
  const payload = { userId, planId }
  if (durationDays != null) payload.durationDays = durationDays
  return apiFetch('/api/admin/subscriptions/grant', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

export async function adminDevices({ userId, keyword }) {
  const url = `/api/admin/devices/list?userId=${encodeURIComponent(userId ?? '')}&keyword=${encodeURIComponent(keyword || '')}`
  return apiFetch(url, { method: 'GET' })
}

export async function adminDeviceRevoke(id) {
  return apiFetch(`/api/admin/devices/${encodeURIComponent(id)}/revoke`, { method: 'POST' })
}

export async function adminDevicesRevokeOthers({ userId, keepId }) {
  return apiFetch('/api/admin/devices/revoke-others', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, keepId })
  })
}

export async function adminDevicesRevokeAll({ userId }) {
  return apiFetch('/api/admin/devices/revoke-all', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId })
  })
}

export async function adminSubscriptionActive(userId) {
  return apiFetch(`/api/admin/subscriptions/active?userId=${encodeURIComponent(userId ?? '')}`, { method: 'GET' })
}

export async function adminSubscriptionRenew({ userId, addDays }) {
  return apiFetch('/api/admin/subscriptions/renew', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, addDays })
  })
}

export async function adminSubscriptionUpgrade({ userId, planId }) {
  return apiFetch('/api/admin/subscriptions/upgrade', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, planId })
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
