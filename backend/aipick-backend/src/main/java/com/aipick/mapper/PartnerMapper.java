package com.aipick.mapper;

import com.aipick.entity.Partner;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 搭子 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface PartnerMapper extends BaseMapper<Partner> {

    /**
     * 分页筛选搭子：按兴趣类型、位置、计划时间、发布者性别
     */
    IPage<Partner> selectPageByFilter(Page<Partner> page,
                                      @Param("type") Integer type,
                                      @Param("location") String location,
                                      @Param("planTimeStart") LocalDateTime planTimeStart,
                                      @Param("planTimeEnd") LocalDateTime planTimeEnd,
                                      @Param("gender") Integer gender);
}