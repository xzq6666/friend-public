package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_subscription_push_log")
public class JobSubscriptionPushLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subscriptionId;
    private Long jobId;
    private LocalDateTime pushTime;
}
