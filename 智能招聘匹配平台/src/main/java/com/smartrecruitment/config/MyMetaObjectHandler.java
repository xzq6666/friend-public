package com.smartrecruitment.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ================================================
 * MyBatis-Plus 自动字段填充处理器
 * ================================================
 *
 * 功能说明：
 *   - insertFill：插入时自动填充 createTime 和 updateTime
 *   - updateFill：更新时自动填充 updateTime
 *
 * 对应实体注解：
 *   @TableField(value = "create_time", fill = FieldFill.INSERT)
 *   @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
 *
 * 如需扩展：
 *   - 添加操作人字段填充（如 createBy / updateBy）
 *   - 添加逻辑删除字段默认值
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
