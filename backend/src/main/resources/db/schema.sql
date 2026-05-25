-- ========================================
-- 房易查 数据库初始化脚本 (Schema)
-- 兼容 H2 Database (MySQL Mode)
-- ========================================

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
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
    password VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    status TINYINT DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 客户表
CREATE TABLE IF NOT EXISTS customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
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
);

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
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (developer_id) REFERENCES developer(id)
);

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
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (customer_id) REFERENCES customer(id),
    FOREIGN KEY (developer_id) REFERENCES developer(id)
);

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
);

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
);
