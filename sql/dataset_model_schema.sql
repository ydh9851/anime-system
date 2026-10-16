-- =====================================================================
-- dataset_model_schema.sql  数据集 + 模型训练任务 建表脚本（幂等）
-- ---------------------------------------------------------------------
-- 对应实验要求「接口二：数据集/模型管理接口」，建两张表：
--   t_dataset     训练数据集（上传的 CSV 元信息）
--   t_train_task  模型训练任务（异步执行 + 状态机 + 评估指标落库）
-- 用法：mysql --default-character-set=utf8mb4 -uroot -p anime_mangage_db < dataset_model_schema.sql
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `t_dataset` (
  `dataset_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '数据集名称',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(512) NOT NULL COMMENT '落盘相对路径',
  `file_size` bigint(20) DEFAULT 0 COMMENT '字节数',
  `row_count` int(11) DEFAULT 0 COMMENT '数据行数（不含表头）',
  `columns_desc` varchar(512) DEFAULT NULL COMMENT '列名摘要（逗号分隔）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `uploader_id` int(11) DEFAULT NULL COMMENT '上传人id',
  `create_time` datetime(0) DEFAULT NULL,
  PRIMARY KEY (`dataset_id`) USING BTREE,
  UNIQUE KEY `uk_dataset_path` (`file_path`) USING BTREE,
  KEY `fk_dataset_uploader` (`uploader_id`) USING BTREE,
  CONSTRAINT `fk_dataset_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `t_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `t_train_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `algo` varchar(32) NOT NULL COMMENT '算法：lr/knn/svm/dt/rf/lgbm',
  `algo_type` varchar(32) DEFAULT 'regression' COMMENT '算法类型',
  `dataset_id` int(11) DEFAULT NULL COMMENT '使用的数据集id，为空表示默认数据集',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
  `progress` int(11) DEFAULT 0 COMMENT '进度百分比 0~100',
  `row_count` int(11) DEFAULT 0 COMMENT '训练样本行数',
  `train_seconds` double DEFAULT NULL COMMENT '训练耗时（秒）',
  `metrics_json` varchar(1024) DEFAULT NULL COMMENT '评估指标 JSON: rmse/mae/r2/r2_log',
  `cache_file` varchar(512) DEFAULT NULL COMMENT '模型缓存文件路径',
  `message` varchar(512) DEFAULT NULL COMMENT '失败原因/备注',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '发起请求的 traceId，便于按请求追踪日志',
  `submitter_id` int(11) DEFAULT NULL COMMENT '提交人id',
  `start_time` datetime(0) DEFAULT NULL,
  `finish_time` datetime(0) DEFAULT NULL,
  `create_time` datetime(0) DEFAULT NULL,
  PRIMARY KEY (`task_id`) USING BTREE,
  KEY `idx_task_status` (`status`, `create_time`) USING BTREE,
  KEY `idx_task_dataset` (`dataset_id`) USING BTREE,
  CONSTRAINT `fk_task_dataset` FOREIGN KEY (`dataset_id`) REFERENCES `t_dataset` (`dataset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_task_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `t_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;
