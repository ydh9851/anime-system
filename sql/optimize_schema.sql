-- =====================================================================
-- optimize_schema.sql  表结构优化脚本（幂等，可重复执行）
-- ---------------------------------------------------------------------
-- 修复项：
--   1. t_user.wx_open_id 原定义为 varchar(0)，无法存任何微信 openid，改为 varchar(64)
--   2. t_user 补 idx_user_name / idx_wx_open_id 索引（登录按用户名查、按 openid 查）
--   3. t_video_info 仅主键，按名称模糊查、按热度排序都是全表扫描，
--      补 idx_video_name / idx_heat_score 索引
-- 用法：mysql --default-character-set=utf8mb4 -uroot -p anime_mangage_db < optimize_schema.sql
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. t_user.wx_open_id 字段长度修复
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `optimize_fix_wx_open_id`;
DELIMITER $$
CREATE PROCEDURE `optimize_fix_wx_open_id`()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'wx_open_id') THEN
    ALTER TABLE `t_user` MODIFY COLUMN `wx_open_id` varchar(64) NULL DEFAULT NULL COMMENT '微信openid';
  END IF;
END$$
DELIMITER ;
CALL `optimize_fix_wx_open_id`();
DROP PROCEDURE IF EXISTS `optimize_fix_wx_open_id`;


-- ---------------------------------------------------------------------
-- 2~3. 补索引：MySQL 不支持 ADD INDEX IF NOT EXISTS，
--     这里用「查 information_schema 判断 + 动态 SQL 执行」实现幂等
-- ---------------------------------------------------------------------
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND INDEX_NAME = 'idx_user_name') = 0,
               'ALTER TABLE `t_user` ADD INDEX `idx_user_name` (`user_name`)',
               'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND INDEX_NAME = 'idx_wx_open_id') = 0,
               'ALTER TABLE `t_user` ADD INDEX `idx_wx_open_id` (`wx_open_id`)',
               'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_video_info' AND INDEX_NAME = 'idx_video_name') = 0,
               'ALTER TABLE `t_video_info` ADD INDEX `idx_video_name` (`video_name`(50))',
               'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_video_info' AND INDEX_NAME = 'idx_heat_score') = 0,
               'ALTER TABLE `t_video_info` ADD INDEX `idx_heat_score` (`heat_score`)',
               'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
