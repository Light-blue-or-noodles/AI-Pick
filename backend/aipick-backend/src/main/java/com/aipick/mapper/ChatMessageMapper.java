package com.aipick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aipick.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话消息 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}