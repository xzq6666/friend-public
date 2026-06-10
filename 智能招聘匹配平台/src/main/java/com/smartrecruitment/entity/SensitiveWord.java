package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sensitive_word")
public class SensitiveWord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("word")
    private String word;

    @TableField("category")
    private String category = "自定义";

    @TableField("source")
    private String source = "manual";

    @TableField("status")
    private Integer status = 1;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
