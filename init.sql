-- FinBrain 金融智能顾问 数据库初始化脚本
-- MySQL 8.0
-- 字符集: utf8mb4
-- 引擎: InnoDB

CREATE DATABASE IF NOT EXISTS finbrain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE finbrain;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `risk_level` VARCHAR(5) DEFAULT 'C1' COMMENT '风险等级(C1~C5)',
    `status` TINYINT DEFAULT 1 COMMENT '状态(0:禁用,1:正常)',
    `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色(user:普通用户,admin:管理员)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除,1:已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户资产账户表
CREATE TABLE IF NOT EXISTS `user_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_asset` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '总资产',
    `available_balance` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '可用余额',
    `frozen_balance` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '冻结金额',
    `total_profit` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '累计收益',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_user_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资产账户表';

-- 资产明细表
CREATE TABLE IF NOT EXISTS `asset_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_asset` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '总资产',
    `available_balance` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '可用余额',
    `frozen_balance` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '冻结金额',
    `holding_amount` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '持仓金额',
    `daily_profit` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '日收益',
    `total_profit` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '累计收益',
    `order_count` INT DEFAULT 0 COMMENT '订单数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产明细表';

-- 资产流水表
CREATE TABLE IF NOT EXISTS `asset_flow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `flow_no` VARCHAR(32) NOT NULL COMMENT '流水号',
    `flow_type` VARCHAR(20) NOT NULL COMMENT '流水类型(recharge:充值/withdraw:提现/buy:申购/redeem:赎回/profit:收益/refund:退款)',
    `amount` DECIMAL(18, 2) NOT NULL COMMENT '金额',
    `balance_before` DECIMAL(18, 2) NOT NULL COMMENT '变动前余额',
    `balance_after` DECIMAL(18, 2) NOT NULL COMMENT '变动后余额',
    `frozen_before` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '变动前冻结',
    `frozen_after` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '变动后冻结',
    `total_asset_before` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '变动前总资产',
    `total_asset_after` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '变动后总资产',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联ID',
    `related_type` VARCHAR(50) DEFAULT NULL COMMENT '关联类型(order:订单/product:产品)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_no` (`flow_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_flow_type` (`flow_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产流水表';

-- 理财产品表
CREATE TABLE IF NOT EXISTS `financial_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品ID',
    `product_code` VARCHAR(20) NOT NULL COMMENT '产品代码',
    `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称',
    `product_type` VARCHAR(5) NOT NULL COMMENT '产品类型(R1~R5)',
    `annual_return_rate` DECIMAL(5, 2) NOT NULL COMMENT '年化收益率(%)',
    `min_amount` DECIMAL(18, 2) NOT NULL COMMENT '起购金额',
    `max_amount` DECIMAL(18, 2) DEFAULT NULL COMMENT '最大购买金额',
    `term_days` INT NOT NULL COMMENT '期限(天)',
    `risk_level` INT DEFAULT 2 COMMENT '风险等级(1-5)',
    `issuer` VARCHAR(100) DEFAULT NULL COMMENT '发行机构',
    `status` TINYINT DEFAULT 0 COMMENT '产品状态(0:草稿,1:募集期,2:存续期,3:已到期,4:已下架)',
    `sale_status` TINYINT DEFAULT 1 COMMENT '销售状态(0:停售,1:在售,2:售罄)',
    `start_date` DATE DEFAULT NULL COMMENT '募集开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '募集结束日期',
    `maturity_date` DATE DEFAULT NULL COMMENT '到期日期',
    `description` TEXT COMMENT '产品描述',
    `instruction_url` VARCHAR(255) DEFAULT NULL COMMENT '产品说明书URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除,1:已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_product_type` (`product_type`),
    KEY `idx_status` (`status`),
    KEY `idx_sale_status` (`sale_status`),
    KEY `idx_maturity_date` (`maturity_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='理财产品表';

-- 产品生命周期日志表
CREATE TABLE IF NOT EXISTS `product_lifecycle_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `product_id` BIGINT NOT NULL COMMENT '产品ID',
    `product_code` VARCHAR(20) NOT NULL COMMENT '产品代码',
    `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称',
    `from_status` TINYINT DEFAULT NULL COMMENT '原状态',
    `from_status_name` VARCHAR(20) DEFAULT NULL COMMENT '原状态名称',
    `to_status` TINYINT NOT NULL COMMENT '新状态',
    `to_status_name` VARCHAR(20) NOT NULL COMMENT '新状态名称',
    `trigger_type` VARCHAR(20) NOT NULL COMMENT '触发类型(AUTO:自动/MANUAL:手动)',
    `trigger_reason` VARCHAR(255) DEFAULT NULL COMMENT '触发原因',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品生命周期日志表';

-- 风险测评记录表
CREATE TABLE IF NOT EXISTS `risk_assessment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测评ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` INT NOT NULL COMMENT '测评得分',
    `risk_level` VARCHAR(5) NOT NULL COMMENT '风险等级(C1~C5)',
    `answers` JSON COMMENT '测评答案(JSON格式)',
    `assessment_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '测评时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_risk_assessment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险测评记录表';

-- 订单/申购表
CREATE TABLE IF NOT EXISTS `product_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '产品ID',
    `amount` DECIMAL(18, 2) NOT NULL COMMENT '申购金额',
    `shares` DECIMAL(18, 4) DEFAULT 0.0000 COMMENT '份额',
    `status` TINYINT DEFAULT 0 COMMENT '状态(0:待确认,1:已确认,2:已取消,3:已赎回,4:已到期,5:已结算)',
    `expected_income` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '预期收益',
    `actual_income` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '实际收益',
    `annual_return_rate` DECIMAL(5, 2) DEFAULT NULL COMMENT '年化收益率(%)',
    `term_days` INT DEFAULT NULL COMMENT '期限(天)',
    `maturity_date` DATE DEFAULT NULL COMMENT '到期日期',
    `expire_time` DATETIME DEFAULT NULL COMMENT '订单过期时间',
    `order_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间',
    `success_time` DATETIME DEFAULT NULL COMMENT '成功时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `settle_time` DATETIME DEFAULT NULL COMMENT '结算时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`),
    KEY `idx_maturity_date` (`maturity_date`),
    CONSTRAINT `fk_product_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_product_order_product` FOREIGN KEY (`product_id`) REFERENCES `financial_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单/申购表';

-- 订单状态日志表
CREATE TABLE IF NOT EXISTS `order_status_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `from_status` TINYINT DEFAULT NULL COMMENT '原状态',
    `from_status_name` VARCHAR(20) DEFAULT NULL COMMENT '原状态名称',
    `to_status` TINYINT NOT NULL COMMENT '新状态',
    `to_status_name` VARCHAR(20) NOT NULL COMMENT '新状态名称',
    `trigger_type` VARCHAR(20) NOT NULL COMMENT '触发类型(AUTO:自动/MANUAL:手动)',
    `trigger_reason` VARCHAR(255) DEFAULT NULL COMMENT '触发原因',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态日志表';

-- 交易流水表
CREATE TABLE IF NOT EXISTS `transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `transaction_no` VARCHAR(32) NOT NULL COMMENT '流水号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '订单ID',
    `type` VARCHAR(20) NOT NULL COMMENT '类型(buy:申购/redeem:赎回/profit:收益/charge:充值)',
    `amount` DECIMAL(18, 2) NOT NULL COMMENT '金额',
    `balance_before` DECIMAL(18, 2) NOT NULL COMMENT '变动前余额',
    `balance_after` DECIMAL(18, 2) NOT NULL COMMENT '变动后余额',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_type` (`type`),
    CONSTRAINT `fk_transaction_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易流水表';

-- AI对话会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `session_id` VARCHAR(36) NOT NULL COMMENT '会话唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_chat_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- AI对话历史表
CREATE TABLE IF NOT EXISTS `chat_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(36) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色(user/assistant/system)',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `intent` VARCHAR(50) DEFAULT NULL COMMENT '识别意图',
    `tool_calls` JSON DEFAULT NULL COMMENT '工具调用记录(JSON格式)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    CONSTRAINT `fk_chat_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话历史表';

-- 插入管理员账号 (密码: 123456, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `email`, `risk_level`, `status`, `role`) VALUES
('admin', '$2a$10$5yCODmSrui7JRyYmmwPOUegqAAWe4GvIWgvwZmPadaWzVTD2SBTCS', '系统管理员', '13900139000', 'admin@finbrain.com', 'C5', 1, 'admin');

-- 插入测试用户 (密码: 123456, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `email`, `risk_level`, `status`, `role`) VALUES
('testuser', '$2a$10$5yCODmSrui7JRyYmmwPOUegqAAWe4GvIWgvwZmPadaWzVTD2SBTCS', '测试用户', '13800138000', 'test@finbrain.com', 'C3', 1, 'user');

-- 插入测试用户账户
INSERT INTO `user_account` (`user_id`, `total_asset`, `available_balance`, `frozen_balance`, `total_profit`) VALUES
(2, 100000.00, 80000.00, 20000.00, 5000.00);

-- 插入测试理财产品
INSERT INTO `financial_product` (`product_code`, `product_name`, `product_type`, `annual_return_rate`, `min_amount`, `max_amount`, `term_days`, `risk_level`, `issuer`, `status`, `sale_status`, `start_date`, `end_date`, `maturity_date`, `description`) VALUES
('FB001', '稳健增值计划A', 'R2', 3.50, 10000.00, 500000.00, 90, 2, 'FinBrain资管', 2, 1, DATE_SUB(CURDATE(), INTERVAL 100 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 80 DAY), '低风险稳健型理财产品，适合保守型投资者。投资于国债、央行票据等高信用等级债券，收益稳定，风险极低。'),
('FB002', '平衡成长计划B', 'R3', 5.20, 50000.00, 1000000.00, 180, 3, 'FinBrain资管', 2, 1, DATE_SUB(CURDATE(), INTERVAL 200 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 160 DAY), '中等风险平衡型理财产品，投资于债券和股票的混合组合，追求稳健增值。'),
('FB003', '进取收益计划C', 'R4', 7.80, 100000.00, 2000000.00, 365, 4, 'FinBrain资管', 1, 1, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 395 DAY), '中高风险进取型理财产品，主要投资于股票市场和优质企业债券，适合有一定风险承受能力的投资者。'),
('FB004', '灵活理财宝', 'R1', 2.80, 1000.00, 100000.00, 30, 1, 'FinBrain资管', 1, 1, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY), '超低风险灵活型理财产品，随时可赎回，适合短期闲置资金理财。'),
('FB005', '高端定制计划D', 'R5', 10.50, 500000.00, 5000000.00, 730, 5, 'FinBrain资管', 0, 0, NULL, NULL, NULL, '高风险高收益型理财产品，投资于股票、期货等高风险资产，仅适合高风险承受能力的专业投资者。');

-- 插入测试订单
INSERT INTO `product_order` (`order_no`, `user_id`, `product_id`, `amount`, `shares`, `status`, `order_time`, `success_time`) VALUES
('ORD202401010001', 1, 1, 20000.00, 20000.0000, 1, NOW(), NOW()),
('ORD202401020001', 1, 2, 50000.00, 50000.0000, 1, NOW(), NOW());

-- 插入测试交易流水
INSERT INTO `transaction` (`transaction_no`, `user_id`, `order_id`, `type`, `amount`, `balance_before`, `balance_after`, `remark`) VALUES
('TXN202401010001', 1, 1, 'buy', 20000.00, 100000.00, 80000.00, '申购稳健增值计划A'),
('TXN202401020001', 1, 2, 'buy', 50000.00, 80000.00, 30000.00, '申购平衡成长计划B');

-- 插入测试风险测评记录
INSERT INTO `risk_assessment` (`user_id`, `score`, `risk_level`, `answers`) VALUES
(1, 65, 'C3', '[{"question": "您的年龄范围是？", "answer": "30-40岁"}, {"question": "您的投资经验如何？", "answer": "3-5年"}, {"question": "您能接受的最大亏损比例是？", "answer": "10%-20%"}]');

-- 知识库文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_type` VARCHAR(50) NOT NULL COMMENT '文件类型',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `object_name` VARCHAR(500) NOT NULL COMMENT 'MinIO对象名',
    `bucket_name` VARCHAR(100) NOT NULL COMMENT 'MinIO桶名',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态(pending/processing/completed/failed)',
    `chunk_count` INT DEFAULT 0 COMMENT '切块数量',
    `error_message` TEXT COMMENT '错误信息',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `process_time` DATETIME DEFAULT NULL COMMENT '处理完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- 数据导入日志表
CREATE TABLE IF NOT EXISTS `data_import_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `import_no` VARCHAR(32) NOT NULL COMMENT '导入编号',
    `import_type` VARCHAR(20) NOT NULL COMMENT '导入类型(product:产品/nav:净值)',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `total_count` INT DEFAULT 0 COMMENT '总记录数',
    `success_count` INT DEFAULT 0 COMMENT '成功数',
    `fail_count` INT DEFAULT 0 COMMENT '失败数',
    `status` VARCHAR(20) DEFAULT 'processing' COMMENT '状态(processing/success/partial/failed)',
    `error_message` TEXT COMMENT '错误信息',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_import_no` (`import_no`),
    KEY `idx_import_type` (`import_type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据导入日志表';

-- 净值历史表
CREATE TABLE IF NOT EXISTS `nav_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '净值ID',
    `product_id` BIGINT NOT NULL COMMENT '产品ID',
    `product_code` VARCHAR(20) NOT NULL COMMENT '产品代码',
    `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称',
    `nav` DECIMAL(10, 4) NOT NULL COMMENT '单位净值',
    `accumulated_nav` DECIMAL(10, 4) DEFAULT NULL COMMENT '累计净值',
    `nav_date` DATE NOT NULL COMMENT '净值日期',
    `daily_return_rate` DECIMAL(5, 4) DEFAULT NULL COMMENT '日收益率(%)',
    `data_source` VARCHAR(20) DEFAULT 'IMPORT' COMMENT '数据来源(IMPORT/SYNC/API)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_date` (`product_id`, `nav_date`),
    KEY `idx_product_code` (`product_code`),
    KEY `idx_nav_date` (`nav_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='净值历史表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `trace_id` VARCHAR(32) NOT NULL COMMENT '追踪ID',
    `module` VARCHAR(50) NOT NULL COMMENT '模块',
    `operation` VARCHAR(50) NOT NULL COMMENT '操作',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `method` VARCHAR(255) DEFAULT NULL COMMENT '方法名',
    `request_url` VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    `request_params` TEXT COMMENT '请求参数',
    `response_data` TEXT COMMENT '响应数据',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `status` VARCHAR(20) DEFAULT 'success' COMMENT '状态(success/failed)',
    `error_msg` TEXT COMMENT '错误信息',
    `duration` BIGINT DEFAULT 0 COMMENT '执行时长(ms)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_module` (`module`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 用户偏好表 (长期记忆持久化)
CREATE TABLE IF NOT EXISTS `user_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '偏好ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `preference_key` VARCHAR(100) NOT NULL COMMENT '偏好键',
    `preference_value` TEXT COMMENT '偏好值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_key` (`user_id`, `preference_key`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_user_preference_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好表';
