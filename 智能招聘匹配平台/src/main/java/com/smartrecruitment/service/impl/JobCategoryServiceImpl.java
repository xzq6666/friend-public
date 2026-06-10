package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.JobCategory;
import com.smartrecruitment.mapper.JobCategoryMapper;
import com.smartrecruitment.service.JobCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobCategoryServiceImpl extends ServiceImpl<JobCategoryMapper, JobCategory> implements JobCategoryService {
    @Override
    public List<JobCategory> getAllCategories() {
        return list(new LambdaQueryWrapper<JobCategory>().orderByAsc(JobCategory::getSortOrder));
    }
}
