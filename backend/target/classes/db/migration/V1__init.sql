CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role(user_id, role_id),
  KEY idx_user(user_id),
  KEY idx_role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS org_building (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  address VARCHAR(255),
  lng DOUBLE,
  lat DOUBLE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS org_area (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  building_id BIGINT,
  name VARCHAR(64) NOT NULL,
  floor VARCHAR(32),
  room VARCHAR(32),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_building(building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dict_trade (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS worker_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  trade_code VARCHAR(32) NOT NULL,
  accept_orders TINYINT NOT NULL DEFAULT 1,
  last_lng DOUBLE,
  last_lat DOUBLE,
  last_location_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_trade(trade_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS work_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  requester_id BIGINT NOT NULL,
  trade_code VARCHAR(32) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  building_id BIGINT,
  area_id BIGINT,
  address VARCHAR(255),
  lng DOUBLE,
  lat DOUBLE,
  status VARCHAR(32) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  assigned_worker_id BIGINT,
  escalation_level INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  assigned_at DATETIME,
  accepted_at DATETIME,
  finished_at DATETIME,
  closed_at DATETIME,
  cancelled_at DATETIME,
  KEY idx_requester(requester_id),
  KEY idx_trade_status(trade_code, status),
  KEY idx_worker_status(assigned_worker_id, status),
  KEY idx_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS work_order_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  kind VARCHAR(32) NOT NULL,
  url VARCHAR(512) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wo(work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS work_order_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  message VARCHAR(1000),
  operator_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wo(work_order_id),
  KEY idx_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS work_order_assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  worker_id BIGINT NOT NULL,
  distance_km DOUBLE,
  active_count INT,
  performance_score DOUBLE,
  final_score DOUBLE,
  chosen TINYINT NOT NULL DEFAULT 0,
  reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wo(work_order_id),
  KEY idx_worker(worker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS work_order_escalation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  level INT NOT NULL,
  stage VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  note VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wo(work_order_id),
  KEY idx_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_thread (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  thread_id BIGINT NOT NULL,
  work_order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  msg_type VARCHAR(32) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_thread(thread_id),
  KEY idx_wo(work_order_id),
  KEY idx_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS work_order_rating (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL UNIQUE,
  rater_id BIGINT NOT NULL,
  stars INT NOT NULL,
  tags VARCHAR(255),
  comment VARCHAR(1000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_rater(rater_id),
  KEY idx_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kpi_daily_worker (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL,
  worker_id BIGINT NOT NULL,
  done_cnt INT NOT NULL DEFAULT 0,
  avg_accept_min DOUBLE,
  avg_first_update_min DOUBLE,
  sla_rate DOUBLE,
  avg_rating DOUBLE,
  bad_rate DOUBLE,
  kpi_score DOUBLE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_date_worker(stat_date, worker_id),
  KEY idx_worker(worker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kpi_monthly_worker (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_month VARCHAR(7) NOT NULL,
  worker_id BIGINT NOT NULL,
  done_cnt INT NOT NULL DEFAULT 0,
  avg_accept_min DOUBLE,
  avg_first_update_min DOUBLE,
  sla_rate DOUBLE,
  avg_rating DOUBLE,
  bad_rate DOUBLE,
  kpi_score DOUBLE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_month_worker(stat_month, worker_id),
  KEY idx_worker(worker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_role(code, name) VALUES
('USER', '报修用户'),
('WORKER', '维修人员'),
('ADMIN', '管理员')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO dict_trade(code, name) VALUES
('WATER_ELEC', '水电'),
('CARPENTER', '木工'),
('NETWORK', '网络')
ON DUPLICATE KEY UPDATE name=VALUES(name);

