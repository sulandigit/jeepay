-- ========================================
-- 转账订单表和商户通知记录表索引优化脚本
-- 创建时间: 2025-10-15
-- 优化目标: 提升转账订单和通知记录查询性能
-- ========================================

-- 说明: 在生产环境执行前请先在测试环境验证
-- 建议: 在业务低峰期执行，避免对业务造成影响

USE jeepaydb;

-- ========================================
-- 转账订单表索引优化
-- ========================================

-- 1. 商户转账订单列表索引
-- 应用场景: 商户转账订单列表查询，按商户号+状态过滤，按创建时间倒序
-- 查询示例: WHERE mch_no = 'M001' AND state = 2 ORDER BY created_at DESC
-- 预期收益: 避免全表扫描，提升查询性能5-10倍
CREATE INDEX idx_mch_state_created ON t_transfer_order (mch_no, state, created_at DESC);

-- 2. 按状态和时间查询索引
-- 应用场景: 按状态查询转账订单，按时间排序
-- 查询示例: WHERE state = 2 ORDER BY created_at DESC
-- 预期收益: 提升状态过滤查询性能
CREATE INDEX idx_state_created ON t_transfer_order (state, created_at);

-- 验证转账订单表索引创建结果
SHOW INDEX FROM t_transfer_order;

-- ========================================
-- 商户通知记录表索引优化
-- ========================================

-- 1. 通知重试任务查询索引
-- 应用场景: 查询需要重试通知的记录
-- 查询示例: WHERE state = 1 AND last_notify_time < NOW() ORDER BY last_notify_time
-- 预期收益: 快速定位待重试通知记录
CREATE INDEX idx_state_notify_time ON t_mch_notify_record (state, last_notify_time);

-- 注意: 表中已存在唯一索引 Uni_OrderId_Type (order_id, order_type)，无需额外添加

-- 验证商户通知记录表索引创建结果
SHOW INDEX FROM t_mch_notify_record;

-- ========================================
-- 执行完毕后，请使用 EXPLAIN 分析查询执行计划
-- 示例: 
--   EXPLAIN SELECT * FROM t_transfer_order WHERE mch_no = 'M001' AND state = 2 ORDER BY created_at DESC;
--   EXPLAIN SELECT * FROM t_mch_notify_record WHERE state = 1 AND last_notify_time < NOW();
-- ========================================
