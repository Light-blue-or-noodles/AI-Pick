package com.aipick.mapper;

import com.aipick.entity.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}