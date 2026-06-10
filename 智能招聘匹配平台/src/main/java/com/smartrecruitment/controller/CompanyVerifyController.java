package com.smartrecruitment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruitment.entity.CompanyInfo;
import com.smartrecruitment.entity.CompanyVerifyRecord;
import com.smartrecruitment.entity.User;
import com.smartrecruitment.service.CompanyInfoService;
import com.smartrecruitment.service.NotificationService;
import com.smartrecruitment.service.UserService;
import com.smartrecruitment.mapper.CompanyVerifyRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 企业认证审核控制器（管理员）
 */
@RestController
@RequestMapping("/admin/company-verify")
@CrossOrigin
public class CompanyVerifyController {

    @Autowired
    private CompanyInfoService companyInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CompanyVerifyRecordMapper verifyRecordMapper;

    /**
     * 获取待审核的企业列表
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingCompanies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        IPage<CompanyInfo> companyPage = new Page<>(page, size);
        LambdaQueryWrapper<CompanyInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompanyInfo::getVerified, 1) // 待审核
                .orderByDesc(CompanyInfo::getCreateTime);
        
        companyPage = companyInfoService.page(companyPage, wrapper);
        
        return ResponseEntity.ok(companyPage);
    }

    /**
     * 获取所有企业认证列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAllCompanies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer verified,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        IPage<CompanyInfo> companyPage = new Page<>(page, size);
        LambdaQueryWrapper<CompanyInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (verified != null) {
            wrapper.eq(CompanyInfo::getVerified, verified);
        }
        
        wrapper.orderByDesc(CompanyInfo::getCreateTime);
        
        companyPage = companyInfoService.page(companyPage, wrapper);
        
        return ResponseEntity.ok(companyPage);
    }

    /**
     * 审核通过
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveCompany(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        CompanyInfo company = companyInfoService.getById(id);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }

        // 更新认证状态
        company.setVerified(2); // 已认证
        company.setVerifyRemark(body.getOrDefault("remark", "认证通过"));
        company.setVerifyTime(LocalDateTime.now());
        companyInfoService.updateById(company);

        // 记录审核日志
        CompanyVerifyRecord record = new CompanyVerifyRecord();
        record.setCompanyId(id);
        record.setUserId(company.getUserId());
        record.setAction("approve");
        record.setStatusBefore(1);
        record.setStatusAfter(2);
        record.setRemark(body.getOrDefault("remark", "认证通过"));
        record.setOperatorId(currentUser.getId());
        verifyRecordMapper.insert(record);

        // 通知企业用户认证已通过
        notificationService.send(company.getUserId(),
            "企业认证已通过",
            "您的企业「" + company.getCompanyName() + "」认证已通过审核，求职者将看到认证标识。",
            "system");

        return ResponseEntity.ok(Map.of("message", "审核通过"));
    }

    /**
     * 审核拒绝
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectCompany(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        String remark = body.get("remark");
        if (remark == null || remark.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请填写拒绝原因"));
        }

        CompanyInfo company = companyInfoService.getById(id);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }

        // 更新认证状态
        company.setVerified(3); // 认证失败
        company.setVerifyRemark(remark);
        company.setVerifyTime(LocalDateTime.now());
        companyInfoService.updateById(company);

        // 记录审核日志
        CompanyVerifyRecord record = new CompanyVerifyRecord();
        record.setCompanyId(id);
        record.setUserId(company.getUserId());
        record.setAction("reject");
        record.setStatusBefore(1);
        record.setStatusAfter(3);
        record.setRemark(remark);
        record.setOperatorId(currentUser.getId());
        verifyRecordMapper.insert(record);

        // 通知企业用户认证被拒绝
        notificationService.send(company.getUserId(),
            "企业认证未通过",
            "您的企业「" + company.getCompanyName() + "」认证未通过，原因：" + remark + "。请修改后重新提交。",
            "system");

        return ResponseEntity.ok(Map.of("message", "已拒绝"));
    }

    /**
     * 获取企业认证详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyDetail(@PathVariable Long id, Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }

        User currentUser = userService.findByUsername(auth.getName());
        if (currentUser == null || !"ADMIN".equals(currentUser.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("error", "权限不足，仅管理员可访问"));
        }

        CompanyInfo company = companyInfoService.getById(id);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(company);
    }
}
