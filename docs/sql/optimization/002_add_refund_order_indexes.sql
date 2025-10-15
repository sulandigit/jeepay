-- ========================================
-- 退款订单表索引优化脚本
-- 创建时间: 2025-10-15
-- 优化目标: 提升退款订单表查询性能
-- ========================================

-- 说明: 在生产环境执行前请先在测试环境验证
-- 建议: 在业务低峰期执行，避免对业务造成影响

USE jeepaydb;

-- 1. 支付订单关联查询索引
-- 应用场景: 根据支付订单查询退款记录
-- 查询示例: WHERE pay_order_id = 'P202501150001' AND state = 2
-- 预期收益: 快速定位支付订单的退款记录
CREATE INDEX idx_pay_order ON t_refund_order (pay_order_id, state);

-- 2. 商户退款订单列表索引
-- 应用场景: 商户退款订单列表查询，按商户号+状态过滤，按创建时间倒序
-- 查询示例: WHERE mch_no = 'M001' AND state = 2 ORDER BY created_at DESC
-- 预期收益: 避免全表扫描，提升查询性能5-10倍
CREATE INDEX idx_mch_state_created ON t_refund_order (mch_no, state, created_at DESC);

-- 3. 渠道支付订单号查询索引
-- 应用场景: 根据渠道支付订单号查询退款
-- 查询示例: WHERE channel_pay_order_no = 'WX202501150001'
-- 预期收益: 快速定位退款订单
CREATE INDEX idx_channel_pay_order ON t_refund_order (channel_pay_order_no);

-- 验证索引创建结果
SHOW INDEX FROM t_refund_order;

-- ========================================
-- 执行完毕后，请使用 EXPLAIN 分析查询执行计划
-- 示例: EXPLAIN SELECT * FROM t_refund_order WHERE pay_order_id = 'P001' ORDER BY created_at DESC;
-- ========================================
