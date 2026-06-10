package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.MatchBlock;
import com.smartrecruitment.mapper.MatchBlockMapper;
import com.smartrecruitment.service.MatchBlockService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchBlockServiceImpl extends ServiceImpl<MatchBlockMapper, MatchBlock> implements MatchBlockService {

    @Override
    public boolean addBlock(Long userId, Integer blockType, Long jobId, Long resumeId, String reason) {
        // 检查是否已屏蔽
        LambdaQueryWrapper<MatchBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchBlock::getUserId, userId).eq(MatchBlock::getBlockType, blockType);
        if (jobId != null) wrapper.eq(MatchBlock::getJobId, jobId);
        if (resumeId != null) wrapper.eq(MatchBlock::getResumeId, resumeId);
        if (getOne(wrapper) != null) return true; // 已存在

        MatchBlock block = new MatchBlock(userId, blockType, jobId, resumeId, reason);
        return save(block);
    }

    @Override
    public boolean removeBlock(Long userId, Integer blockType, Long targetId) {
        LambdaQueryWrapper<MatchBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchBlock::getUserId, userId).eq(MatchBlock::getBlockType, blockType);
        if (blockType == 1) wrapper.eq(MatchBlock::getJobId, targetId);
        else wrapper.eq(MatchBlock::getResumeId, targetId);
        return remove(wrapper);
    }

    @Override
    public boolean isBlocked(Long userId, Integer blockType, Long targetId) {
        LambdaQueryWrapper<MatchBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchBlock::getUserId, userId).eq(MatchBlock::getBlockType, blockType);
        if (blockType == 1) wrapper.eq(MatchBlock::getJobId, targetId);
        else wrapper.eq(MatchBlock::getResumeId, targetId);
        return count(wrapper) > 0;
    }

    @Override
    public Set<Long> getBlockedJobIds(Long userId) {
        LambdaQueryWrapper<MatchBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchBlock::getUserId, userId).eq(MatchBlock::getBlockType, 1);
        return list(wrapper).stream().map(MatchBlock::getJobId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getBlockedResumeIds(Long userId) {
        LambdaQueryWrapper<MatchBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchBlock::getUserId, userId).eq(MatchBlock::getBlockType, 2);
        return list(wrapper).stream().map(MatchBlock::getResumeId).collect(Collectors.toSet());
    }

    @Override
    public List<MatchBlock> getUserBlocks(Long userId, Integer blockType) {
        LambdaQueryWrapper<MatchBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchBlock::getUserId, userId);
        if (blockType != null) wrapper.eq(MatchBlock::getBlockType, blockType);
        wrapper.orderByDesc(MatchBlock::getCreateTime);
        return list(wrapper);
    }
}
