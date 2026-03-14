package com.aipick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aipick.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author AI-Pick
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}