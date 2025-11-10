-- ========================================
-- 支付订单表索引优化脚本
-- 创建时间: 2025-10-15
-- 优化目标: 提升支付订单表查询性能
-- ========================================

-- 说明: 在生产环境执行前请先在测试环境验证
-- 建议: 在业务低峰期执行，避免对业务造成影响

USE jeepaydb;

-- 1. 商户订单列表查询索引
-- 应用场景: 商户订单列表查询，按商户号+状态过滤，按创建时间倒序
-- 查询示例: WHERE mch_no = 'M001' AND state = 2 ORDER BY created_at DESC
-- 预期收益: 避免全表扫描，提升查询性能5-10倍
CREATE INDEX idx_mch_state_created ON t_pay_order (mch_no, state, created_at DESC);

-- 2. 超时订单扫描索引
-- 应用场景: 定时任务扫描超时订单
-- 查询示例: WHERE state IN (0, 1) AND expired_time <= NOW()
-- 预期收益: 快速定位超时订单，避免全表扫描
CREATE INDEX idx_state_expired ON t_pay_order (state, expired_time);

-- 3. 支付方式统计索引
-- 应用场景: 按支付方式分组统计
-- 查询示例: WHERE way_code = 'wxpay_jsapi' AND state = 2 GROUP BY way_code
-- 预期收益: 提升支付方式统计查询性能3-5倍
CREATE INDEX idx_way_state_created ON t_pay_order (way_code, state, created_at);

-- 4. 渠道订单号查询索引
-- 应用场景: 根据第三方渠道订单号反查支付订单
-- 查询示例: WHERE channel_order_no = 'WX202501150001'
-- 预期收益: 快速定位订单，响应时间从秒级降至毫秒级
CREATE INDEX idx_channel_order ON t_pay_order (channel_order_no);

-- 5. 商户通知状态查询索引
-- 应用场景: 查询需要重试通知的订单
-- 查询示例: WHERE notify_state = 0 ORDER BY created_at
-- 预期收益: 快速定位待通知订单
CREATE INDEX idx_notify_state ON t_pay_order (notify_state, created_at);

-- 6. 分账任务查询索引
-- 应用场景: 查询待分账或分账中的订单
-- 查询示例: WHERE division_mode > 0 AND division_state IN (1, 2) ORDER BY created_at
-- 预期收益: 提升分账任务查询性能
CREATE INDEX idx_division_state ON t_pay_order (division_mode, division_state, created_at);

-- 验证索引创建结果
SHOW INDEX FROM t_pay_order;

-- ========================================
-- 执行完毕后，请使用 EXPLAIN 分析查询执行计划
-- 示例: EXPLAIN SELECT * FROM t_pay_order WHERE mch_no = 'M001' AND state = 2 ORDER BY created_at DESC LIMIT 20;
-- ========================================
