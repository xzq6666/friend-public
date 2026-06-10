package com.smartrecruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruitment.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * ================================================
 * 用户 Mapper - MyBatis-Plus 基础 CRUD
 * ================================================
 *
 * 提供 User 实体对 user 表的自动 CRUD 操作。
 * 继承 BaseMapper 后无需写 XML，自带方法：
 *   insert/deleteById/updateById/selectById/selectList 等
 *
 * 如需扩展：
 *   - 添加自定义 SQL（如复杂统计查询），在此接口中定义方法
 *   - 对应 XML 文件放在 resources/mapper/UserMapper.xml
 *   - 示例：@Select("SELECT * FROM user WHERE ...") List<User> findCustom();
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
