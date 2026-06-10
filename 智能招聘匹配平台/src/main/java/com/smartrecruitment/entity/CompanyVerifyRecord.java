package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 企业认证申请记录实体
 */
@Data
@TableName("company_verify_record")
public class CompanyVerifyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("user_id")
    private Long userId;

    @TableField("action")
    private String action; // submit-提交申请, approve-通过, reject-拒绝

    @TableField("status_before")
    private Integer statusBefore;

    @TableField("status_after")
    private Integer statusAfter;

    @TableField("remark")
    private String remark;

    @TableField("operator_id")
    private Long operatorId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
