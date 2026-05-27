-- ========================================
-- 房易查 数据库初始化脚本 (Schema)
-- 适配 MySQL 8.0，无外键约束
-- 数据完整性由应用层保证
-- ========================================

-- ========================================
-- AI 会话表
-- ========================================
CREATE TABLE IF NOT EXISTS ai_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    title VARCHAR(100) COMMENT '会话标题（自动生成，截取30字）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话';

-- ========================================
-- AI 消息表
-- ========================================
CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '所属会话ID',
    role VARCHAR(10) NOT NULL COMMENT '角色: user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型: text/recommendation/favorites/order_summary',
    metadata JSON COMMENT '结构化数据（卡片渲染用JSON）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_created (session_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息';

-- ========================================
-- 待确认订单表（AI下单两阶段用）
-- ========================================
CREATE TABLE IF NOT EXISTS pending_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT 'AI会话ID',
    customer_id BIGINT COMMENT '客户ID',
    property_id BIGINT NOT NULL COMMENT '楼盘ID',
    customer_name VARCHAR(50) COMMENT '客户姓名',
    customer_phone VARCHAR(20) COMMENT '客户电话',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/confirmed/cancelled',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    confirmed_time DATETIME COMMENT '确认时间',
    UNIQUE KEY uk_session_pending (session_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI待确认订单';

-- ========================================
-- 知识库文档表
-- ========================================
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    developer_id BIGINT NOT NULL COMMENT '上传的开发商ID',
    filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_filename VARCHAR(255) NOT NULL COMMENT '存储文件名（data/rag-uploads/下）',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    status VARCHAR(20) DEFAULT 'uploaded' COMMENT '状态: uploaded/indexed/error',
    error_message VARCHAR(500) COMMENT '错误信息',
    uploaded_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    indexed_time DATETIME COMMENT '索引时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档';

-- 开发商表
CREATE TABLE IF NOT EXISTS developer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL COMMENT '公司名称',
    contact_person VARCHAR(100) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(500) COMMENT '公司地址',
    business_license VARCHAR(100) COMMENT '营业执照号',
    description TEXT COMMENT '公司简介',
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    status TINYINT DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='开发商';

-- 客户表
CREATE TABLE IF NOT EXISTS customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    real_name VARCHAR(100) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    id_card VARCHAR(18) COMMENT '身份证号',
    intention VARCHAR(200) COMMENT '购房意向(逗号分隔)',
    preferred_locations VARCHAR(500) COMMENT '偏好区域',
    budget_min DECIMAL(12,2) COMMENT '最低预算',
    budget_max DECIMAL(12,2) COMMENT '最高预算',
    urgency VARCHAR(20) COMMENT '购房紧迫度: 一个月内/三个月内/半年内/一年内/不限',
    status TINYINT DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户';

-- 房产表
CREATE TABLE IF NOT EXISTS property (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    developer_id BIGINT NOT NULL COMMENT '所属开发商ID',
    property_name VARCHAR(200) NOT NULL COMMENT '楼盘名称',
    location VARCHAR(500) NOT NULL COMMENT '地理位置',
    longitude DECIMAL(10,7) COMMENT '经度',
    latitude DECIMAL(10,7) COMMENT '纬度',
    floor_min INT COMMENT '最低楼层',
    floor_max INT COMMENT '最高楼层',
    floor_plan_type VARCHAR(50) NOT NULL COMMENT '户型: 一室一厅/两室一厅/三室两厅/四室两厅/复式/别墅',
    total_units INT NOT NULL COMMENT '总户数',
    vacant_units INT NOT NULL COMMENT '空置户数',
    vacancy_rate DECIMAL(5,2) COMMENT '空置率(自动计算)',
    price_per_sqm DECIMAL(12,2) COMMENT '单价(元/平米)',
    total_price DECIMAL(14,2) COMMENT '总价',
    area_sqm DECIMAL(10,2) COMMENT '面积(平米)',
    decoration VARCHAR(50) COMMENT '装修情况: 毛坯/简装/精装/豪装',
    status VARCHAR(20) DEFAULT '在售' COMMENT '状态: 在售/已售/待开盘',
    description TEXT COMMENT '楼盘描述',
    image_urls VARCHAR(2000) COMMENT '图片URL(逗号分隔)',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房产';

-- 建议/意向表
CREATE TABLE IF NOT EXISTS suggestion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    developer_id BIGINT NOT NULL COMMENT '开发商ID',
    preferred_type VARCHAR(50) COMMENT '偏好户型',
    price_min DECIMAL(12,2) COMMENT '最低预算',
    price_max DECIMAL(12,2) COMMENT '最高预算',
    notes TEXT COMMENT '备注说明',
    status VARCHAR(20) DEFAULT '待回复' COMMENT '状态: 待回复/已回复/已关闭',
    reply_content TEXT COMMENT '回复内容',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='建议/意向';

-- 报表记录表
CREATE TABLE IF NOT EXISTS report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    user_role VARCHAR(20) NOT NULL COMMENT '角色: ROLE_CUSTOMER/ROLE_DEVELOPER',
    report_type VARCHAR(50) COMMENT '报表类型: 查询结果/统计报表',
    query_params TEXT COMMENT '查询参数(JSON)',
    result_summary TEXT COMMENT '结果摘要(JSON)',
    file_path VARCHAR(500) COMMENT 'PDF文件路径',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表记录';

-- 价格历史表
CREATE TABLE IF NOT EXISTS price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL COMMENT '房产ID',
    record_date DATE NOT NULL COMMENT '记录日期（月）',
    price_per_sqm DECIMAL(12,2) NOT NULL COMMENT '当月单价(元/平米)',
    total_price DECIMAL(14,2) NOT NULL COMMENT '当月总价',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_property_date (property_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格历史';

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    property_id BIGINT NOT NULL COMMENT '楼盘ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_customer_property (customer_id, property_id),
    INDEX idx_customer_created (customer_id, created_time DESC),
    INDEX idx_property (property_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏';

-- 活动日志表
CREATE TABLE IF NOT EXISTS activity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL COMMENT '操作人ID',
    actor_role VARCHAR(20) NOT NULL COMMENT '操作人角色',
    action VARCHAR(50) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id BIGINT COMMENT '实体ID',
    detail TEXT COMMENT '操作详情',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动日志';
