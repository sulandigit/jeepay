-- =============================================
-- API签名验证机制 - 数据库迁移脚本
-- 版本: 1.0
-- 日期: 2025-10-17
-- 说明: 为t_sys_user表添加API密钥相关字段
-- =============================================

USE jeepay;

-- 1. 为系统用户表添加API密钥相关字段
ALTER TABLE t_sys_user
ADD COLUMN api_secret VARCHAR(64) DEFAULT NULL COMMENT '用户API密钥,用于签名验证' AFTER updated_at,
ADD COLUMN secret_status TINYINT(1) DEFAULT 1 COMMENT '密钥状态: 0-禁用 1-启用' AFTER api_secret,
ADD COLUMN secret_create_time DATETIME DEFAULT NULL COMMENT '密钥生成时间' AFTER secret_status;

-- 2. 为api_secret字段添加索引(可选,用于快速查询)
-- CREATE INDEX idx_api_secret ON t_sys_user(api_secret);

-- 3. 数据迁移: 为现有用户生成API密钥(可选)
-- 注意: 以下语句会为所有现有用户生成随机API密钥
-- 如果不需要立即生成,可以注释掉此部分,后续通过应用程序生成

-- UPDATE t_sys_user 
-- SET api_secret = REPLACE(UUID(), '-', ''),
--     secret_status = 1,
--     secret_create_time = NOW()
-- WHERE api_secret IS NULL;

-- =============================================
-- 验证脚本执行结果
-- =============================================

-- 查看表结构
DESC t_sys_user;

-- 查看新增字段数据
SELECT sys_user_id, login_username, api_secret, secret_status, secret_create_time 
FROM t_sys_user 
LIMIT 10;

-- =============================================
-- 回滚脚本(如需回滚,执行以下语句)
-- =============================================

-- ALTER TABLE t_sys_user
-- DROP COLUMN api_secret,
-- DROP COLUMN secret_status,
-- DROP COLUMN secret_create_time;
