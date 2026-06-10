package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.MatchBlock;

import java.util.List;
import java.util.Set;

public interface MatchBlockService extends IService<MatchBlock> {

    /** 添加屏蔽 */
    boolean addBlock(Long userId, Integer blockType, Long jobId, Long resumeId, String reason);

    /** 取消屏蔽 */
    boolean removeBlock(Long userId, Integer blockType, Long targetId);

    /** 检查是否已屏蔽 */
    boolean isBlocked(Long userId, Integer blockType, Long targetId);

    /** 获取用户所有屏蔽的职位ID */
    Set<Long> getBlockedJobIds(Long userId);

    /** 获取用户所有屏蔽的简历ID */
    Set<Long> getBlockedResumeIds(Long userId);

    /** 获取用户的屏蔽列表 */
    List<MatchBlock> getUserBlocks(Long userId, Integer blockType);
}
