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

-- 理财产品表
CREATE TABLE IF NOT EXISTS `financial_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品ID',
    `product_code` VARCHAR(20) NOT NULL COMMENT '产品代码',
    `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称',
    `product_type` VARCHAR(5) NOT NULL COMMENT '产品类型(R1~R5)',
    `annual_return_rate` DECIMAL(5, 2) NOT NULL COMMENT '年化收益率(%)',
    `min_amount` DECIMAL(18, 2) NOT NULL COMMENT '起购金额',
    `term_days` INT NOT NULL COMMENT '期限(天)',
    `sale_status` TINYINT DEFAULT 1 COMMENT '销售状态(0:停售,1:在售,2:售罄)',
    `description` TEXT COMMENT '产品描述',
    `instruction_url` VARCHAR(255) DEFAULT NULL COMMENT '产品说明书URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除,1:已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_product_type` (`product_type`),
    KEY `idx_sale_status` (`sale_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='理财产品表';

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
    `status` TINYINT DEFAULT 0 COMMENT '状态(0:待确认,1:已确认,2:已取消,3:已赎回)',
    `order_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `success_time` DATETIME DEFAULT NULL COMMENT '确认时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_product_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_product_order_product` FOREIGN KEY (`product_id`) REFERENCES `financial_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单/申购表';

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

-- 插入测试用户 (密码: 123456, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `email`, `risk_level`, `status`) VALUES
('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户', '13800138000', 'test@finbrain.com', 'C3', 1);

-- 插入测试用户账户
INSERT INTO `user_account` (`user_id`, `total_asset`, `available_balance`, `frozen_balance`, `total_profit`) VALUES
(1, 100000.00, 80000.00, 20000.00, 5000.00);

-- 插入测试理财产品
INSERT INTO `financial_product` (`product_code`, `product_name`, `product_type`, `annual_return_rate`, `min_amount`, `term_days`, `sale_status`, `description`) VALUES
('FB001', '稳健增值计划A', 'R2', 3.50, 10000.00, 90, 1, '低风险稳健型理财产品，适合保守型投资者。投资于国债、央行票据等高信用等级债券，收益稳定，风险极低。'),
('FB002', '平衡成长计划B', 'R3', 5.20, 50000.00, 180, 1, '中等风险平衡型理财产品，投资于债券和股票的混合组合，追求稳健增值。'),
('FB003', '进取收益计划C', 'R4', 7.80, 100000.00, 365, 1, '中高风险进取型理财产品，主要投资于股票市场和优质企业债券，适合有一定风险承受能力的投资者。'),
('FB004', '灵活理财宝', 'R1', 2.80, 1000.00, 30, 1, '超低风险灵活型理财产品，随时可赎回，适合短期闲置资金理财。'),
('FB005', '高端定制计划D', 'R5', 10.50, 500000.00, 730, 1, '高风险高收益型理财产品，投资于股票、期货等高风险资产，仅适合高风险承受能力的专业投资者。');

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
