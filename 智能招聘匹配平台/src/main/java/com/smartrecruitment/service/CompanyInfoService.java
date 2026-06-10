package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.CompanyInfo;

public interface CompanyInfoService extends IService<CompanyInfo> {
    /**
     * 获取企业信息（根据用户ID）
     */
    CompanyInfo getByUserId(Long userId);
    
    /**
     * 保存或更新企业信息
     */
    CompanyInfo saveOrUpdateCompany(CompanyInfo companyInfo);
}
