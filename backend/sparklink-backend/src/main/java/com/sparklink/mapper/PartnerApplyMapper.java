package com.sparklink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparklink.entity.PartnerApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 搭子应征 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface PartnerApplyMapper extends BaseMapper<PartnerApply> {

    /**
     * 我报名参加的搭子 ID（待审核/已通过，且非本人发布）
     */
    @Select("""
            SELECT a.partner_id
            FROM t_partner_apply a
            INNER JOIN t_partner p ON p.id = a.partner_id
            WHERE a.user_id = #{userId}
              AND a.status IN (0, 1)
              AND p.user_id <> #{userId}
            GROUP BY a.partner_id
            ORDER BY MAX(a.create_time) DESC
            """)
    List<Long> selectJoinedPartnerIds(@Param("userId") Long userId);
}