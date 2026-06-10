package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.JobSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职位订阅 Mapper
 */
@Mapper
public interface JobSubscriptionMapper extends BaseMapper<JobSubscription> {
    
    /**
     * 查询需要推送的订阅（按策略筛选）
     */
    @Select("SELECT * FROM job_subscription WHERE is_active = 1 AND push_strategy = #{strategy} " +
            "AND (last_push_time IS NULL OR last_push_time < DATE_SUB(NOW(), INTERVAL " +
            "CASE #{strategy} " +
            "WHEN 'daily' THEN 1 DAY " +
            "WHEN 'weekly' THEN 7 DAY " +
            "ELSE 0 MINUTE END))")
    List<JobSubscription> findSubscriptionsByStrategy(String strategy);
}
