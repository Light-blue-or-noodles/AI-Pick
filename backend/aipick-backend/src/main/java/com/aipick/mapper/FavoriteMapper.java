package com.aipick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aipick.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收藏 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询用户收藏的活动ID列表
     */
    @Select("SELECT target_id FROM t_favorite WHERE user_id = #{userId} AND target_type = 1 AND deleted = 0")
    List<Long> selectFavoriteActivityIds(@Param("userId") Long userId);

    /**
     * 查询用户收藏的搭子ID列表
     */
    @Select("SELECT target_id FROM t_favorite WHERE user_id = #{userId} AND target_type = 2 AND deleted = 0")
    List<Long> selectFavoritePartnerIds(@Param("userId") Long userId);
}
