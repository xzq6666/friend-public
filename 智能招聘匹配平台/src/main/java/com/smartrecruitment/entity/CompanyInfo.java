package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 企业信息实体
 */
@Data
@TableName("company_info")
public class CompanyInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("company_name")
    private String companyName;

    @TableField("company_scale")
    private String companyScale; // 1-20人以下, 20-99人, 100-499人, 500-999人, 1000人以上

    @TableField("industry")
    private String industry;

    @TableField("description")
    private String description;

    @TableField("address")
    private String address;

    @TableField("website")
    private String website;

    @TableField("logo_url")
    private String logoUrl;

    @TableField("business_license")
    private String businessLicense;

    @TableField("contact_person")
    private String contactPerson;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    @TableField(value = "verified", updateStrategy = FieldStrategy.ALWAYS)
    private Integer verified = 0; // 0-未认证, 1-待审核, 2-已认证, 3-认证失败

    @TableField(value = "verify_remark", updateStrategy = FieldStrategy.ALWAYS)
    private String verifyRemark;

    @TableField(value = "verify_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime verifyTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
