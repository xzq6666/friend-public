package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.Favorite;

import java.util.List;
import java.util.Map;

public interface FavoriteService extends IService<Favorite> {

    Favorite addFavorite(Long userId, Integer targetType, Long targetId);

    boolean removeFavorite(Long userId, Integer targetType, Long targetId);

    boolean isFavorited(Long userId, Integer targetType, Long targetId);

    List<Map<String, Object>> getFavoriteJobs(Long userId, Long folderId);

    List<Map<String, Object>> getFavoriteResumes(Long userId, Long folderId);

    /**
     * 移动收藏到指定文件夹
     * @param favoriteId 收藏记录ID
     * @param folderId 目标文件夹ID，null 表示移出文件夹（未分类）
     * @param userId 当前用户ID（用于权限校验）
     */
    boolean moveToFolder(Long favoriteId, Long folderId, Long userId);
}
