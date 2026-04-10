CREATE TABLE IF NOT EXISTS app_user (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(120) NOT NULL,
  enabled BOOLEAN NOT NULL,
  role VARCHAR(16) NOT NULL,
  create_time TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_username ON app_user(username);

CREATE TABLE IF NOT EXISTS membership_plan (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  price_cents INT NOT NULL,
  duration_days INT NOT NULL,
  device_limit INT NOT NULL,
  daily_free_limit INT NOT NULL,
  watermark BOOLEAN NOT NULL,
  enabled BOOLEAN NOT NULL,
  sort_order INT NOT NULL,
  create_time TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_membership_plan_code ON membership_plan(code);

CREATE TABLE IF NOT EXISTS user_subscription (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NULL,
  status VARCHAR(16) NOT NULL,
  create_time TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_user_subscription_user ON user_subscription(user_id);
CREATE INDEX IF NOT EXISTS idx_user_subscription_end ON user_subscription(end_time);

CREATE TABLE IF NOT EXISTS device_session (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id BIGINT NOT NULL,
  device_id VARCHAR(128) NOT NULL,
  token VARCHAR(64) NOT NULL,
  create_time TIMESTAMP NOT NULL,
  last_seen_time TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_device_session_token ON device_session(token);
CREATE INDEX IF NOT EXISTS idx_device_session_user ON device_session(user_id);
CREATE INDEX IF NOT EXISTS idx_device_session_last ON device_session(last_seen_time);

CREATE TABLE IF NOT EXISTS daily_usage (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id BIGINT NOT NULL,
  day DATE NOT NULL,
  count INT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_daily_usage_user_day ON daily_usage(user_id, day);

CREATE TABLE IF NOT EXISTS admin_op_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  admin_user_id BIGINT NOT NULL,
  admin_username VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32),
  target_id VARCHAR(64),
  detail_json TEXT,
  ip VARCHAR(64),
  create_time TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_admin_op_log_time ON admin_op_log(create_time);
CREATE INDEX IF NOT EXISTS idx_admin_op_log_admin ON admin_op_log(admin_user_id, create_time);

CREATE TABLE IF NOT EXISTS app_setting (
  k VARCHAR(128) PRIMARY KEY,
  v TEXT,
  update_time TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_app_setting_time ON app_setting(update_time);
