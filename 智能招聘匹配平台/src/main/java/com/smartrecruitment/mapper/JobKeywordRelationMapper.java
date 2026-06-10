package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.JobKeywordRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位-关键词关联Mapper
 */
@Mapper
public interface JobKeywordRelationMapper extends BaseMapper<JobKeywordRelation> {
}
