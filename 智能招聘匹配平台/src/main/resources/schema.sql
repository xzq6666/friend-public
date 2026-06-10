-- ================================================
-- 数据库迁移脚本 - 智能招聘匹配平台
-- 执行时间: 2026-05-26 / 2026-05-27
-- ================================================
-- 注意: 本脚本使用 CREATE TABLE IF NOT EXISTS 和 DROP/CREATE 模式
--       确保可以重复执行而不会报错

-- 1. 为 user 表添加 avatar 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'avatar');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(500) DEFAULT NULL COMMENT ''用户头像URL'' AFTER `user_type`',
  'SELECT ''avatar column already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 创建通知偏好表
CREATE TABLE IF NOT EXISTS `notification_prefs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `email_notify` TINYINT NOT NULL DEFAULT 1 COMMENT '邮件通知开关 (0关闭/1开启)',
    `sms_notify` TINYINT NOT NULL DEFAULT 1 COMMENT '短信通知开关',
    `job_match_notify` TINYINT NOT NULL DEFAULT 1 COMMENT '职位匹配通知开关',
    `interview_notify` TINYINT NOT NULL DEFAULT 1 COMMENT '面试通知开关',
    `system_notify` TINYINT NOT NULL DEFAULT 1 COMMENT '系统通知开关',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_notification_prefs_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知偏好设置表';

-- 3. 创建通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `type` VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT '通知类型: job_match/interview/system',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读 (0未读/1已读)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_notification_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

-- 4. 创建收藏文件夹表
CREATE TABLE IF NOT EXISTS `favorite_folder` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '文件夹名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_favorite_folder_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏文件夹表';

-- 5. 为 favorite 表添加 folder_id 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND COLUMN_NAME = 'folder_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `favorite` ADD COLUMN `folder_id` BIGINT DEFAULT NULL COMMENT ''所属文件夹ID'' AFTER `target_id`',
  'SELECT ''folder_id column already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 为 favorite 表添加 folder_id 索引（如果不存在）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND INDEX_NAME = 'idx_folder_id');
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE `favorite` ADD KEY `idx_folder_id` (`folder_id`)',
  'SELECT ''folder_id index already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. 为 interview 表添加 chat_type 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'interview' AND COLUMN_NAME = 'chat_type');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `interview` ADD COLUMN `chat_type` TINYINT NOT NULL DEFAULT 0 COMMENT ''类型：0-正式面试邀请 1-直接沟通（聊天）'' AFTER `notes`',
  'SELECT ''chat_type column already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8. 将已有的直接沟通记录（interview_location = '在线沟通'）标记为 chat_type = 1
UPDATE `interview` SET `chat_type` = 1 WHERE `interview_location` = '在线沟通' AND `chat_type` = 0;

-- 9. 插入示例通知数据（仅在 notification 表为空时执行）
INSERT INTO `notification` (`user_id`, `title`, `content`, `type`)
SELECT 1, '系统通知', '欢迎使用智能招聘匹配平台！', 'system'
WHERE EXISTS (SELECT 1 FROM `user` WHERE `id` = 1)
  AND NOT EXISTS (SELECT 1 FROM `notification` WHERE `user_id` = 1);

-- 10. 为 interview_chat 表添加 file_url 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'interview_chat' AND COLUMN_NAME = 'file_url');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `interview_chat` ADD COLUMN `file_url` VARCHAR(500) DEFAULT NULL COMMENT ''文件URL'' AFTER `content`',
  'SELECT ''file_url column already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 11. 为 interview_chat 表添加 file_name 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'interview_chat' AND COLUMN_NAME = 'file_name');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `interview_chat` ADD COLUMN `file_name` VARCHAR(255) DEFAULT NULL COMMENT ''原始文件名'' AFTER `file_url`',
  'SELECT ''file_name column already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ================================================
-- v11 新增功能：职位订阅系统
-- ================================================

-- 12. 创建职位订阅表
CREATE TABLE IF NOT EXISTS `job_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '订阅名称',
    `keywords` TEXT COMMENT '关键词列表（JSON数组）',
    `category_id` BIGINT DEFAULT NULL COMMENT '行业分类ID',
    `salary_min` DECIMAL(10,2) DEFAULT NULL COMMENT '最低薪资',
    `salary_max` DECIMAL(10,2) DEFAULT NULL COMMENT '最高薪资',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '工作地点',
    `experience_required` VARCHAR(50) DEFAULT NULL COMMENT '经验要求',
    `education_required` VARCHAR(50) DEFAULT NULL COMMENT '学历要求',
    `job_type` VARCHAR(20) DEFAULT NULL COMMENT '工作性质（全职/兼职/远程）',
    `company_scale` VARCHAR(50) DEFAULT NULL COMMENT '公司规模',
    `push_strategy` VARCHAR(20) NOT NULL DEFAULT 'daily' COMMENT '推送策略：realtime/daily/weekly',
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否激活（0停用/1启用）',
    `last_push_time` DATETIME DEFAULT NULL COMMENT '最后推送时间',
    `match_count` INT NOT NULL DEFAULT 0 COMMENT '累计匹配职位数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_active` (`is_active`),
    CONSTRAINT `fk_job_subscription_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位订阅表';

CREATE TABLE IF NOT EXISTS `job_subscription_push_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
    `job_id` BIGINT NOT NULL COMMENT '职位ID',
    `push_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sub_job` (`subscription_id`, `job_id`),
    KEY `idx_subscription_id` (`subscription_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位订阅推送记录表';

-- 13. 创建简历版本表
CREATE TABLE IF NOT EXISTS `resume_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resume_id` BIGINT NOT NULL COMMENT '简历ID',
    `version_number` VARCHAR(20) NOT NULL COMMENT '版本号（如v1.0, v1.1）',
    `version_tag` VARCHAR(50) DEFAULT NULL COMMENT '版本标签（投递前/优化后等）',
    `version_note` TEXT COMMENT '版本备注',
    `snapshot_data` LONGTEXT NOT NULL COMMENT '简历快照数据（JSON）',
    `skills_snapshot` TEXT COMMENT '技能快照（JSON数组）',
    `work_exp_snapshot` TEXT COMMENT '工作经历快照（JSON）',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_resume_id` (`resume_id`),
    KEY `idx_version_number` (`version_number`),
    CONSTRAINT `fk_resume_version_resume_id` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历版本表';

-- ================================================
-- v12 新增功能：公告系统
-- ================================================

-- 15. 创建公告表
CREATE TABLE IF NOT EXISTS `announcement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT COMMENT '公告内容',
    `target_type` VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '目标用户: ALL/EMPLOYEE/EMPLOYER',
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效 (0删除/1有效)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_target_type` (`target_type`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_announcement_publisher_id` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- ================================================
-- v11 新增功能：求职进度看板
-- ================================================

-- 14. 为 job_application 表添加 status 字段映射说明
-- 现有 status 字段：0-待处理 1-已通过 2-已拒绝 3-面试中 4-已录用
-- 看板阶段映射：
--   已收藏 (favorites表)
--   已投递 (status=0)
--   初筛中 (status=0, 投递后3天内)
--   面试中 (status=3)
--   已录用 (status=4)
--   已拒绝 (status=2)

-- ================================================
-- v13 新增功能：访问历史系统
-- ================================================

-- 16. 创建访问历史表
CREATE TABLE IF NOT EXISTS `visit_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `visitor_id` BIGINT NOT NULL COMMENT '访问者用户ID',
    `visitor_username` VARCHAR(100) NOT NULL COMMENT '访问者用户名快照',
    `visitor_user_type` VARCHAR(20) NOT NULL COMMENT '访问者类型: EMPLOYEE/EMPLOYER',
    `visitor_avatar` VARCHAR(500) DEFAULT NULL COMMENT '访问者头像快照',
    `target_type` TINYINT NOT NULL COMMENT '访问类型：1=简历/主页 2=职位',
    `target_id` BIGINT NOT NULL COMMENT '目标ID(简历ID或职位ID)',
    `target_owner_id` BIGINT NOT NULL COMMENT '被访问内容的所有者ID',
    `is_anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名访问(0否/1是)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`id`),
    KEY `idx_target_owner` (`target_owner_id`, `create_time`),
    KEY `idx_visitor` (`visitor_id`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问历史记录表';

-- 17. 创建用户隐私设置表
CREATE TABLE IF NOT EXISTS `user_privacy_settings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `default_anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '默认匿名访问(0否/1是)',
    `show_visit_history` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示访问记录(0隐藏/1显示)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_privacy_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户隐私设置表';

-- 19. 为 job 表添加 work_type 字段（工作类型：remote/onsite/hybrid）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'job' AND COLUMN_NAME = 'work_type');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `job` ADD COLUMN `work_type` VARCHAR(20) DEFAULT NULL COMMENT ''工作类型：remote-远程 onsite-现场 hybrid-混合'' AFTER `education_required`',
  'SELECT ''work_type column already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 18. 为 notification_prefs 表添加 chat_notify 字段
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_prefs' AND COLUMN_NAME = 'chat_notify');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `notification_prefs` ADD COLUMN `chat_notify` TINYINT NOT NULL DEFAULT 1 COMMENT ''聊天通知开关'' AFTER `system_notify`',
  'SELECT ''chat_notify column already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ================================================
-- v14 新增功能：职位论坛系统
-- ================================================

-- 20. 创建论坛帖子表
CREATE TABLE IF NOT EXISTS `forum_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_id` BIGINT NOT NULL COMMENT '关联职位ID',
    `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
    `content` TEXT COMMENT '帖子内容',
    `is_pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶 (0否/1是)',
    `is_closed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否关闭讨论 (0否/1是)',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_forum_post_job_id` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_forum_post_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位论坛帖子表';

-- 21. 创建论坛评论表
CREATE TABLE IF NOT EXISTS `forum_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id` BIGINT NOT NULL COMMENT '关联帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID (NULL为一级评论)',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '被回复用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    CONSTRAINT `fk_forum_comment_post_id` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_forum_comment_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛评论表';

-- ================================================
-- v15 新增功能：敏感词系统
-- ================================================

-- 22. 创建敏感词表
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `word` VARCHAR(100) NOT NULL COMMENT '敏感词',
    `category` VARCHAR(50) NOT NULL DEFAULT '自定义' COMMENT '分类：政治/色情/暴力/广告/歧视/自定义/AI检测',
    `source` VARCHAR(20) NOT NULL DEFAULT 'manual' COMMENT '来源：manual-手动添加/ai-AI检测',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 23. 预置基础敏感词数据（仅在表为空时插入）
INSERT INTO `sensitive_word` (`word`, `category`, `source`, `status`)
SELECT * FROM (
    -- 辱骂类
    SELECT '操你妈', '歧视', 'manual', 1 UNION ALL
    SELECT '他妈的', '歧视', 'manual', 1 UNION ALL
    SELECT '你妈死了', '歧视', 'manual', 1 UNION ALL
    SELECT '狗日的', '歧视', 'manual', 1 UNION ALL
    SELECT '王八蛋', '歧视', 'manual', 1 UNION ALL
    SELECT '混蛋', '歧视', 'manual', 1 UNION ALL
    SELECT '傻逼', '歧视', 'manual', 1 UNION ALL
    SELECT '草泥马', '歧视', 'manual', 1 UNION ALL
    SELECT '尼玛', '歧视', 'manual', 1 UNION ALL
    SELECT '你妈逼', '歧视', 'manual', 1 UNION ALL
    SELECT '他妈', '歧视', 'manual', 1 UNION ALL
    SELECT '贱人', '歧视', 'manual', 1 UNION ALL
    SELECT '婊子', '歧视', 'manual', 1 UNION ALL
    SELECT '废物', '歧视', 'manual', 1 UNION ALL
    SELECT '白痴', '歧视', 'manual', 1 UNION ALL
    SELECT '蠢货', '歧视', 'manual', 1 UNION ALL
    SELECT '脑子有病', '歧视', 'manual', 1 UNION ALL
    SELECT '去死', '暴力', 'manual', 1 UNION ALL
    SELECT '打死你', '暴力', 'manual', 1 UNION ALL
    SELECT '弄死你', '暴力', 'manual', 1 UNION ALL
    -- 色情类
    SELECT '约炮', '色情', 'manual', 1 UNION ALL
    SELECT '嫖娼', '色情', 'manual', 1 UNION ALL
    SELECT '卖淫', '色情', 'manual', 1 UNION ALL
    SELECT '裸聊', '色情', 'manual', 1 UNION ALL
    SELECT '色情', '色情', 'manual', 1 UNION ALL
    -- 政治敏感
    SELECT '法轮功', '政治', 'manual', 1 UNION ALL
    SELECT '六四事件', '政治', 'manual', 1 UNION ALL
    -- 违法类
    SELECT '代开发票', '广告', 'manual', 1 UNION ALL
    SELECT '办证', '广告', 'manual', 1 UNION ALL
    SELECT '赌博', '暴力', 'manual', 1 UNION ALL
    SELECT '毒品', '暴力', 'manual', 1 UNION ALL
    SELECT '冰毒', '暴力', 'manual', 1 UNION ALL
    SELECT '海洛因', '暴力', 'manual', 1 UNION ALL
    -- 广告类
    SELECT '刷单', '广告', 'manual', 1 UNION ALL
    SELECT '日赚万元', '广告', 'manual', 1 UNION ALL
    SELECT '兼职日结', '广告', 'manual', 1 UNION ALL
    SELECT '加微信', '广告', 'manual', 1 UNION ALL
    SELECT '加QQ', '广告', 'manual', 1
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `sensitive_word` LIMIT 1);

-- ================================================
-- v16 新增功能：举报系统
-- ================================================

-- 24. 创建举报表
CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
    `reported_type` TINYINT NOT NULL COMMENT '举报类型：1-帖子 2-评论 3-用户 4-职位',
    `reported_id` BIGINT NOT NULL COMMENT '被举报对象ID',
    `reason` VARCHAR(100) NOT NULL COMMENT '举报原因',
    `description` TEXT COMMENT '详细描述',
    `evidence_urls` TEXT COMMENT '证据图片URL列表（JSON）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-处理中 2-已处理 3-已驳回',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
    `handle_result` TEXT COMMENT '处理结果',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_reporter` (`reporter_id`),
    KEY `idx_reported` (`reported_type`, `reported_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';
