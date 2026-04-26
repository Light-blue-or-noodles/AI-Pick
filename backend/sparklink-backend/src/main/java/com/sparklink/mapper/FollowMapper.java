package com.sparklink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparklink.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 重新关注：将已逻辑删除的关注行恢复为有效（避免唯一键 user_id + follow_user_id 与软删行冲突导致 insert 失败）
     */
    @Update("UPDATE t_follow SET deleted = 0, update_time = NOW() WHERE user_id = #{userId} AND follow_user_id = #{followUserId} AND deleted = 1")
    int restoreIfLogicallyDeleted(@Param("userId") Long userId, @Param("followUserId") Long followUserId);
}
