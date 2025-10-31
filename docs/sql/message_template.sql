-- ========================================
-- 消息模板版本管理 - 数据库表结构
-- ========================================

-- 消息模板表
DROP TABLE IF EXISTS `t_msg_template`;
CREATE TABLE `t_msg_template` (
    `template_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_type` TINYINT(6) NOT NULL COMMENT '模板类型：1-支付通知，2-退款通知，3-转账通知',
    `current_version` INT(11) NOT NULL DEFAULT 0 COMMENT '当前生效版本号',
    `state` TINYINT(6) NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注说明',
    `created_by` VARCHAR(50) NOT NULL COMMENT '创建人',
    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_template_type` (`template_type`),
    KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板表';

-- 消息模板版本表
DROP TABLE IF EXISTS `t_msg_template_version`;
CREATE TABLE `t_msg_template_version` (
    `version_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `template_id` BIGINT(20) NOT NULL COMMENT '模板ID',
    `version_no` INT(11) NOT NULL COMMENT '版本号，从1开始递增',
    `template_content` TEXT NOT NULL COMMENT '模板内容，支持变量占位符',
    `content_format` VARCHAR(20) NOT NULL DEFAULT 'QUERY_STRING' COMMENT '内容格式：JSON、QUERY_STRING',
    `variable_list` JSON DEFAULT NULL COMMENT '变量列表定义',
    `state` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '版本状态：0-草稿，1-已发布，2-已归档',
    `publish_time` TIMESTAMP(3) NULL DEFAULT NULL COMMENT '发布时间',
    `created_by` VARCHAR(50) NOT NULL COMMENT '创建人',
    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`version_id`),
    UNIQUE KEY `uk_template_version` (`template_id`, `version_no`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板版本表';

-- 消息变量定义表
DROP TABLE IF EXISTS `t_msg_variable_define`;
CREATE TABLE `t_msg_variable_define` (
    `variable_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '变量ID',
    `variable_code` VARCHAR(50) NOT NULL COMMENT '变量编码',
    `variable_name` VARCHAR(100) NOT NULL COMMENT '变量名称',
    `data_type` VARCHAR(20) NOT NULL COMMENT '数据类型：STRING、NUMBER、DATE、AMOUNT',
    `data_source` VARCHAR(50) NOT NULL COMMENT '数据来源：PAY_ORDER、REFUND_ORDER、TRANSFER_ORDER、MCH_APP',
    `source_field` VARCHAR(50) NOT NULL COMMENT '来源字段名',
    `format_rule` VARCHAR(200) DEFAULT NULL COMMENT '格式化规则，如日期格式、金额转换',
    `default_value` VARCHAR(100) DEFAULT NULL COMMENT '默认值',
    `is_required` TINYINT(6) NOT NULL DEFAULT 0 COMMENT '是否必填：0-否，1-是',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注说明',
    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`variable_id`),
    UNIQUE KEY `uk_variable_code` (`variable_code`),
    KEY `idx_data_source` (`data_source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息变量定义表';

-- ========================================
-- 初始化内置变量定义
-- ========================================

-- 支付通知变量
INSERT INTO `t_msg_variable_define` (`variable_code`, `variable_name`, `data_type`, `data_source`, `source_field`, `format_rule`, `is_required`, `remark`) VALUES
('pay_order_id', '支付订单号', 'STRING', 'PAY_ORDER', 'payOrderId', NULL, 1, '支付订单ID'),
('mch_order_no', '商户订单号', 'STRING', 'PAY_ORDER', 'mchOrderNo', NULL, 1, '商户系统订单号'),
('amount', '支付金额', 'AMOUNT', 'PAY_ORDER', 'amount', 'yuan', 1, '支付金额，单位：分'),
('currency', '货币代码', 'STRING', 'PAY_ORDER', 'currency', 'upper', 1, '三位货币代码'),
('state', '订单状态', 'NUMBER', 'PAY_ORDER', 'state', 'stateMap', 1, '订单状态'),
('success_time', '成功时间', 'DATE', 'PAY_ORDER', 'successTime', 'yyyy-MM-dd HH:mm:ss', 0, '支付成功时间'),
('channel_order_no', '渠道订单号', 'STRING', 'PAY_ORDER', 'channelOrderNo', NULL, 0, '渠道订单号'),
('subject', '商品标题', 'STRING', 'PAY_ORDER', 'subject', NULL, 0, '商品标题'),
('body', '商品描述', 'STRING', 'PAY_ORDER', 'body', NULL, 0, '商品描述'),
('client_ip', '客户端IP', 'STRING', 'PAY_ORDER', 'clientIp', NULL, 0, '客户端IP地址'),
('if_code', '接口代码', 'STRING', 'PAY_ORDER', 'ifCode', NULL, 0, '支付接口代码'),
('way_code', '支付方式', 'STRING', 'PAY_ORDER', 'wayCode', NULL, 0, '支付方式代码'),
('ext_param', '扩展参数', 'STRING', 'PAY_ORDER', 'extParam', NULL, 0, '商户扩展参数'),
('created_at', '订单创建时间', 'DATE', 'PAY_ORDER', 'createdAt', 'yyyy-MM-dd HH:mm:ss', 0, '订单创建时间'),
('app_id', '应用ID', 'STRING', 'PAY_ORDER', 'appId', NULL, 1, '应用ID'),
('mch_no', '商户号', 'STRING', 'PAY_ORDER', 'mchNo', NULL, 1, '商户号'),
('req_time', '请求时间', 'DATE', 'SYSTEM', 'currentTime', 'yyyy-MM-dd HH:mm:ss', 1, '当前请求时间'),
('sign', '签名值', 'STRING', 'COMPUTED', 'sign', NULL, 1, 'MD5/RSA签名');

-- 退款通知变量
INSERT INTO `t_msg_variable_define` (`variable_code`, `variable_name`, `data_type`, `data_source`, `source_field`, `format_rule`, `is_required`, `remark`) VALUES
('refund_order_id', '退款订单号', 'STRING', 'REFUND_ORDER', 'refundOrderId', NULL, 1, '退款订单ID'),
('mch_refund_no', '商户退款单号', 'STRING', 'REFUND_ORDER', 'mchRefundNo', NULL, 1, '商户退款单号'),
('refund_amount', '退款金额', 'AMOUNT', 'REFUND_ORDER', 'refundAmount', 'yuan', 1, '退款金额，单位：分'),
('pay_amount', '支付金额', 'AMOUNT', 'REFUND_ORDER', 'payAmount', 'yuan', 1, '原支付金额'),
('err_code', '错误码', 'STRING', 'REFUND_ORDER', 'errCode', NULL, 0, '渠道错误码'),
('err_msg', '错误描述', 'STRING', 'REFUND_ORDER', 'errMsg', NULL, 0, '渠道错误描述');

-- 转账通知变量
INSERT INTO `t_msg_variable_define` (`variable_code`, `variable_name`, `data_type`, `data_source`, `source_field`, `format_rule`, `is_required`, `remark`) VALUES
('transfer_id', '转账订单号', 'STRING', 'TRANSFER_ORDER', 'transferId', NULL, 1, '转账订单ID'),
('entry_type', '入账方式', 'STRING', 'TRANSFER_ORDER', 'entryType', NULL, 0, '入账方式'),
('account_no', '收款账号', 'STRING', 'TRANSFER_ORDER', 'accountNo', NULL, 0, '收款账号'),
('account_name', '收款人姓名', 'STRING', 'TRANSFER_ORDER', 'accountName', NULL, 0, '收款人姓名'),
('bank_name', '收款银行', 'STRING', 'TRANSFER_ORDER', 'bankName', NULL, 0, '收款银行'),
('transfer_desc', '转账备注', 'STRING', 'TRANSFER_ORDER', 'transferDesc', NULL, 0, '转账备注');

-- ========================================
-- 初始化默认模板
-- ========================================

-- 支付通知默认模板
INSERT INTO `t_msg_template` (`template_code`, `template_name`, `template_type`, `current_version`, `state`, `created_by`, `remark`) VALUES
('PAY_ORDER_NOTIFY', '支付订单通知', 1, 1, 1, 'system', '支付订单成功/失败通知商户的默认模板');

INSERT INTO `t_msg_template_version` (`template_id`, `version_no`, `template_content`, `content_format`, `variable_list`, `state`, `publish_time`, `created_by`) VALUES
(1, 1, 'payOrderId=${pay_order_id}&mchOrderNo=${mch_order_no}&amount=${amount}&currency=${currency}&state=${state}&subject=${subject}&body=${body}&clientIp=${client_ip}&channelOrderNo=${channel_order_no}&ifCode=${if_code}&wayCode=${way_code}&extParam=${ext_param}&successTime=${success_time}&createdAt=${created_at}&appId=${app_id}&mchNo=${mch_no}&reqTime=${req_time}&sign=${sign}', 
'QUERY_STRING', 
'["pay_order_id","mch_order_no","amount","currency","state","subject","body","client_ip","channel_order_no","if_code","way_code","ext_param","success_time","created_at","app_id","mch_no","req_time","sign"]', 
1, NOW(3), 'system');

-- 退款通知默认模板
INSERT INTO `t_msg_template` (`template_code`, `template_name`, `template_type`, `current_version`, `state`, `created_by`, `remark`) VALUES
('REFUND_ORDER_NOTIFY', '退款订单通知', 2, 1, 1, 'system', '退款订单完成通知商户的默认模板');

INSERT INTO `t_msg_template_version` (`template_id`, `version_no`, `template_content`, `content_format`, `variable_list`, `state`, `publish_time`, `created_by`) VALUES
(2, 1, 'refundOrderId=${refund_order_id}&payOrderId=${pay_order_id}&mchRefundNo=${mch_refund_no}&refundAmount=${refund_amount}&payAmount=${pay_amount}&currency=${currency}&state=${state}&channelOrderNo=${channel_order_no}&errCode=${err_code}&errMsg=${err_msg}&extParam=${ext_param}&successTime=${success_time}&createdAt=${created_at}&appId=${app_id}&mchNo=${mch_no}&sign=${sign}', 
'QUERY_STRING', 
'["refund_order_id","pay_order_id","mch_refund_no","refund_amount","pay_amount","currency","state","channel_order_no","err_code","err_msg","ext_param","success_time","created_at","app_id","mch_no","sign"]', 
1, NOW(3), 'system');

-- 转账通知默认模板
INSERT INTO `t_msg_template` (`template_code`, `template_name`, `template_type`, `current_version`, `state`, `created_by`, `remark`) VALUES
('TRANSFER_ORDER_NOTIFY', '转账订单通知', 3, 1, 1, 'system', '转账订单完成通知商户的默认模板');

INSERT INTO `t_msg_template_version` (`template_id`, `version_no`, `template_content`, `content_format`, `variable_list`, `state`, `publish_time`, `created_by`) VALUES
(3, 1, 'transferId=${transfer_id}&mchOrderNo=${mch_order_no}&amount=${amount}&currency=${currency}&entryType=${entry_type}&state=${state}&accountNo=${account_no}&accountName=${account_name}&bankName=${bank_name}&transferDesc=${transfer_desc}&channelOrderNo=${channel_order_no}&errCode=${err_code}&errMsg=${err_msg}&extParam=${ext_param}&successTime=${success_time}&createdAt=${created_at}&ifCode=${if_code}&appId=${app_id}&mchNo=${mch_no}&sign=${sign}', 
'QUERY_STRING', 
'["transfer_id","mch_order_no","amount","currency","entry_type","state","account_no","account_name","bank_name","transfer_desc","channel_order_no","err_code","err_msg","ext_param","success_time","created_at","if_code","app_id","mch_no","sign"]', 
1, NOW(3), 'system');
