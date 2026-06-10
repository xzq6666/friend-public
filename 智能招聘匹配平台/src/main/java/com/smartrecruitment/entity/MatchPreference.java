package com.smartrecruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 匹配偏好记录
 * 根据用户浏览/投递/邀请行为自动学习偏好，用于调整后续推荐排序
 */
@TableName("match_preference")
public class MatchPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 偏好类型：1=行业偏好, 2=薪资偏好, 3=地点偏好, 4=技能偏好, 5=学历偏好 */
    private Integer prefType;

    /** 偏好值（如行业ID、城市名、技能名等） */
    private String prefValue;

    /** 偏好权重（0~100，越高表示越偏好） */
    private Integer weight;

    /** 来源：1=显式设置, 2=浏览行为, 3=投递行为, 4=邀请行为 */
    private Integer source;

    /** 关联的职位/简历ID（用于追溯） */
    private Long relatedId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public MatchPreference() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getPrefType() { return prefType; }
    public void setPrefType(Integer prefType) { this.prefType = prefType; }
    public String getPrefValue() { return prefValue; }
    public void setPrefValue(String prefValue) { this.prefValue = prefValue; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Integer getSource() { return source; }
    public void setSource(Integer source) { this.source = source; }
    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
