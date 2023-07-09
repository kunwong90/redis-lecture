DROP TABLE IF EXISTS distribute_lock;
CREATE TABLE distribute_lock (
	`id` BIGINT (30) NOT NULL PRIMARY KEY AUTO_INCREMENT,
	`lock_key` VARCHAR (100) NOT NULL,
	`lock_value` VARCHAR(100) NOT NULL,
	`lease_time` INT(10) NOT NULL,
	`expire_date` DATETIME NOT NULL COMMENT '过期时间',
	`reentrant_times` INT(10) NOT NULL DEFAULT 0 COMMENT '重入次数',
	`version` INT(10) NOT NULL DEFAULT 0 COMMENT '版本号',
	`add_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
	`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
	UNIQUE KEY `uiq_idx_lock_key` (`lock_key`)
);