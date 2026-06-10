package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.Favorite;
import com.smartrecruitment.mapper.FavoriteMapper;
import com.smartrecruitment.service.FavoriteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Override
    public Favorite addFavorite(Long userId, Integer targetType, Long targetId) {
        if (isFavorited(userId, targetType, targetId)) {
            throw new RuntimeException("已经收藏过了");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        save(favorite);
        return favorite;
    }

    @Override
    public boolean removeFavorite(Long userId, Integer targetType, Long targetId) {
        return lambdaUpdate()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId)
                .remove();
    }

    @Override
    public boolean isFavorited(Long userId, Integer targetType, Long targetId) {
        return lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId)
                .count() > 0;
    }

    @Override
    public List<Map<String, Object>> getFavoriteJobs(Long userId, Long folderId) {
        return baseMapper.getFavoriteJobs(userId, folderId);
    }

    @Override
    public List<Map<String, Object>> getFavoriteResumes(Long userId, Long folderId) {
        return baseMapper.getFavoriteResumes(userId, folderId);
    }

    @Override
    public boolean moveToFolder(Long favoriteId, Long folderId, Long userId) {
        Favorite favorite = getById(favoriteId);
        if (favorite == null) {
            throw new RuntimeException("收藏记录不存在");
        }
        if (!favorite.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此收藏记录");
        }
        // folderId 为 null 表示移出文件夹（未分类）
        favorite.setFolderId(folderId);
        return updateById(favorite);
    }
}
