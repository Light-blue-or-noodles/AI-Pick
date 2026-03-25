package com.aipick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aipick.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 关注 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 查询用户关注的用户ID列表
     */
    @Select("SELECT follow_user_id FROM t_follow WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectFollowingUserIds(@Param("userId") Long userId);

    /**
     * 查询关注该用户的用户ID列表
     */
    @Select("SELECT user_id FROM t_follow WHERE follow_user_id = #{userId} AND deleted = 0")
    List<Long> selectFollowerUserIds(@Param("userId") Long userId);

    /**
     * 查询关注数
     */
    @Select("SELECT COUNT(*) FROM t_follow WHERE user_id = #{userId} AND deleted = 0")
    int selectFollowingCount(@Param("userId") Long userId);

    /**
     * 查询粉丝数
     */
    @Select("SELECT COUNT(*) FROM t_follow WHERE follow_user_id = #{userId} AND deleted = 0")
    int selectFollowerCount(@Param("userId") Long userId);
}
