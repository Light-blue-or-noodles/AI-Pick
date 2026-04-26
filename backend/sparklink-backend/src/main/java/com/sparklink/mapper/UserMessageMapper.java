package com.sparklink.mapper;

import com.sparklink.entity.UserMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {
}