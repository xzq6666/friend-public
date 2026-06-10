package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.JobCategory;

import java.util.List;

public interface JobCategoryService extends IService<JobCategory> {
    List<JobCategory> getAllCategories();
}
