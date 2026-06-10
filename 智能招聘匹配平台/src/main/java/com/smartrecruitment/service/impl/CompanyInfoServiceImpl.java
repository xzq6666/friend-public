package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.CompanyInfo;
import com.smartrecruitment.mapper.CompanyInfoMapper;
import com.smartrecruitment.service.CompanyInfoService;
import org.springframework.stereotype.Service;

@Service
public class CompanyInfoServiceImpl extends ServiceImpl<CompanyInfoMapper, CompanyInfo> implements CompanyInfoService {
    
    @Override
    public CompanyInfo getByUserId(Long userId) {
        return lambdaQuery()
                .eq(CompanyInfo::getUserId, userId)
                .one();
    }
    
    @Override
    public CompanyInfo saveOrUpdateCompany(CompanyInfo companyInfo) {
        CompanyInfo existing = getByUserId(companyInfo.getUserId());

        if (existing != null) {
            companyInfo.setId(existing.getId());

            // 如果控制器已将 verified 设为 1（重新提交认证），保留该值
            // 否则保留原有认证状态
            if (companyInfo.getVerified() == null || companyInfo.getVerified() != 1) {
                companyInfo.setVerified(existing.getVerified());
                companyInfo.setVerifyRemark(existing.getVerifyRemark());
                companyInfo.setVerifyTime(existing.getVerifyTime());
            } else {
                // 重新提交认证，清除之前的拒绝原因
                companyInfo.setVerifyRemark(null);
                companyInfo.setVerifyTime(null);
            }

            updateById(companyInfo);
        } else {
            save(companyInfo);
        }

        return getById(companyInfo.getId());
    }
}
