package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

/**
 * ================================================
 * 简历 Mapper - MyBatis-Plus 基础 CRUD
 * ================================================
 *
 * 提供 Resume 实体对 resume 表的自动 CRUD 操作。
 *
 * 如需扩展：
 *   - 添加按技能标签搜索的自定义 SQL
 *   - 添加按薪资范围筛选的查询
 *   - 添加统计简历数量的聚合查询
 *   - 添加全文搜索（需配合 MySQL FULLTEXT INDEX）
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
