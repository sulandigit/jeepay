-- 站内消息表
-- @author [mybatis plus generator]
-- @since 2025

CREATE TABLE IF NOT EXISTS `t_sys_message` (
  `msg_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `title` VARCHAR(128) NOT NULL COMMENT '消息标题',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `msg_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '消息类型: 1-系统消息, 2-通知消息, 3-告警消息, 9-自定义消息',
  `receiver_user_id` BIGINT(20) DEFAULT NULL COMMENT '接收用户ID',
  `receiver_username` VARCHAR(32) DEFAULT NULL COMMENT '接收用户名',
  `sender_user_id` BIGINT(20) DEFAULT NULL COMMENT '发送用户ID',
  `sender_username` VARCHAR(32) DEFAULT NULL COMMENT '发送用户名',
  `state` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '消息状态: 0-未读, 1-已读',
  `push_type` TINYINT(2) NOT NULL DEFAULT 2 COMMENT '推送方式: 1-全体推送, 2-指定用户推送',
  `sys_type` VARCHAR(8) NOT NULL DEFAULT 'MGR' COMMENT '所属系统: MGR-运营平台, MCH-商户中心',
  `read_time` DATETIME DEFAULT NULL COMMENT '读取时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`msg_id`),
  KEY `idx_receiver_user` (`receiver_user_id`, `sys_type`, `state`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统站内消息表';
