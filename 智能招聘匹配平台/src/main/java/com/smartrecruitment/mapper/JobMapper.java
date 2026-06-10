package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * ================================================
 * 职位 Mapper - MyBatis-Plus 基础 CRUD
 * ================================================
 *
 * 提供 Job 实体对 job 表的自动 CRUD 操作。
 *
 * 如需扩展：
 *   - 添加按地点/薪资/经验筛选的查询
 *   - 添加职位发布统计（按天/月）
 *   - 添加企业发布的职位列表查询
 *   - 添加热门职位排行查询
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
