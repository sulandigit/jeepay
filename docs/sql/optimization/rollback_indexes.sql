-- ========================================
-- 索引回滚脚本
-- 创建时间: 2025-10-15
-- 用途: 在优化效果不理想或出现问题时，删除所有新增索引
-- ========================================

-- 说明: 在生产环境执行前请先备份数据
-- 建议: 在业务低峰期执行

USE jeepaydb;

-- ========================================
-- 删除支付订单表索引
-- ========================================
DROP INDEX IF EXISTS idx_mch_state_created ON t_pay_order;
DROP INDEX IF EXISTS idx_state_expired ON t_pay_order;
DROP INDEX IF EXISTS idx_way_state_created ON t_pay_order;
DROP INDEX IF EXISTS idx_channel_order ON t_pay_order;
DROP INDEX IF EXISTS idx_notify_state ON t_pay_order;
DROP INDEX IF EXISTS idx_division_state ON t_pay_order;

-- 验证删除结果
SELECT 'Pay Order Indexes After Rollback:' AS Status;
SHOW INDEX FROM t_pay_order;

-- ========================================
-- 删除退款订单表索引
-- ========================================
DROP INDEX IF EXISTS idx_pay_order ON t_refund_order;
DROP INDEX IF EXISTS idx_mch_state_created ON t_refund_order;
DROP INDEX IF EXISTS idx_channel_pay_order ON t_refund_order;

-- 验证删除结果
SELECT 'Refund Order Indexes After Rollback:' AS Status;
SHOW INDEX FROM t_refund_order;

-- ========================================
-- 删除转账订单表索引
-- ========================================
DROP INDEX IF EXISTS idx_mch_state_created ON t_transfer_order;
DROP INDEX IF EXISTS idx_state_created ON t_transfer_order;

-- 验证删除结果
SELECT 'Transfer Order Indexes After Rollback:' AS Status;
SHOW INDEX FROM t_transfer_order;

-- ========================================
-- 删除商户通知记录表索引
-- ========================================
DROP INDEX IF EXISTS idx_state_notify_time ON t_mch_notify_record;

-- 验证删除结果
SELECT 'Notify Record Indexes After Rollback:' AS Status;
SHOW INDEX FROM t_mch_notify_record;

-- ========================================
-- 回滚完成
-- ========================================
SELECT 'Rollback Completed Successfully!' AS Status;
