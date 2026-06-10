package com.smartrecruitment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartrecruitment.entity.FavoriteFolder;
import com.smartrecruitment.mapper.FavoriteFolderMapper;
import com.smartrecruitment.service.FavoriteFolderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FavoriteFolderServiceImpl extends ServiceImpl<FavoriteFolderMapper, FavoriteFolder> implements FavoriteFolderService {

    @Override
    public List<Map<String, Object>> getFolderListWithCount(Long userId) {
        return baseMapper.getFolderListWithCount(userId);
    }
}
