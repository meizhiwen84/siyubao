# 私信截图王

桌面端（user-app）+ 远程管理端（admin-app）的账号会员体系版本。

## 技术栈

- 后端：Spring Boot 2.7.x
- 数据库：SQLite
- user-app 前端：Vue SPA（输出到 `/app/`，JCEF 桌面内嵌）
- admin-app 前端：Vue SPA（输出到 `/admin/`）

## 核心能力

- 账号注册/登录（用户名+密码，无手机无邮箱）
- 会员套餐可后台维护（价格/时长/设备数/水印/每日次数）
- 免费用户每日次数限制（默认 3 次/日）
- 单设备/多设备限制与挤号（device session + 心跳）
- 生成聊天预览（dy/xhs/sph）+ 可编辑应用
- 历史对话记录查询（按账号隔离）
- 管理端：用户管理（启用/禁用、重置密码）、设备在线管理（踢下线/批量踢）、订阅开通/续费/升级、操作日志

## 运行方式

- 启动管理端：
  - `./mvnw -pl admin-app spring-boot:run`
  - 管理端页面：`http://localhost:6943/admin/`
- 启动用户端（指向管理端）：
  - `ADMIN_BASE_URL=http://localhost:6943 ./mvnw -pl user-app spring-boot:run`
  - 用户端页面：`http://localhost:6942/app/`

## 关键接口（摘要）

- admin-app（公开）
  - `POST /api/public/register`
  - `POST /api/public/login`
  - `GET /api/public/plans`
- admin-app（用户态，Bearer token）
  - `GET /api/me`
  - `POST /api/heartbeat`
  - `POST /api/generate/consume`
- admin-app（管理员态，Bearer token 且 role=ADMIN）
  - `/api/admin/plans`（套餐管理）
  - `/api/admin/users/*`（用户管理/重置密码）
  - `/api/admin/devices/*`（设备在线/踢下线）
  - `/api/admin/subscriptions/*`（开通/续费/升级）
  - `/api/admin/logs/*`（操作日志）
  - `/api/admin/settings/*`（系统设置）
