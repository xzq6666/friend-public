package com.smartrecruitment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartrecruitment.entity.FavoriteFolder;

import java.util.List;
import java.util.Map;

public interface FavoriteFolderService extends IService<FavoriteFolder> {

    /**
     * 获取文件夹列表，包含每个文件夹的收藏数量
     */
    List<Map<String, Object>> getFolderListWithCount(Long userId);
}
