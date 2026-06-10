package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.FavoriteFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FavoriteFolderMapper extends BaseMapper<FavoriteFolder> {

    /**
     * 获取文件夹列表，包含每个文件夹的收藏数量
     */
    @Select("SELECT ff.id, ff.user_id, ff.name, ff.sort_order, ff.create_time, ff.update_time, " +
            "COUNT(f.id) as count " +
            "FROM favorite_folder ff " +
            "LEFT JOIN favorite f ON ff.id = f.folder_id " +
            "WHERE ff.user_id = #{userId} " +
            "GROUP BY ff.id " +
            "ORDER BY ff.sort_order ASC, ff.create_time DESC")
    List<Map<String, Object>> getFolderListWithCount(@Param("userId") Long userId);
}
