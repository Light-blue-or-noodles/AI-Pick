package com.sparklink.service;

import com.sparklink.dto.LoginRequest;
import com.sparklink.dto.LoginResponse;
import com.sparklink.dto.RegisterRequest;
import com.sparklink.dto.UpdateUserRequest;
import com.sparklink.dto.WechatLoginRequest;
import com.sparklink.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 *
 * @author AI-Pick
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户信息
     */
    User register(RegisterRequest request);

    /**
     * 用户登录（用户名+密码）
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 微信小程序登录
     *
     * @param request 微信登录请求
     * @return 登录响应
     */
    LoginResponse wechatLogin(WechatLoginRequest request);

    /**
     * 本地/联调测试登录（仅当 app.allow-test-login=true），签发固定用户 JWT
     *
     * @return 登录响应
     */
    LoginResponse testLogin();

    /**
     * 获取当前用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserInfo(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param request 更新请求
     * @return 用户信息
     */
    User updateUserInfo(Long userId, UpdateUserRequest request);

    /**
     * 头像文件上传成功后：写入用户表并同步腾讯云 IM（与小程序展示一致）。
     *
     * @param userId        用户ID
     * @param storedUrlPath 存储服务返回的路径，如 /static/avatars/xxx.jpg
     * @return 更新后的用户（已脱敏）
     */
    User saveUploadedAvatarAndSyncIm(Long userId, String storedUrlPath);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据微信openid查询用户
     *
     * @param openid 微信openid
     * @return 用户信息
     */
    User getUserByOpenid(String openid);

    /**
     * 加入公司
     *
     * @param userId      用户ID
     * @param companyName 公司名称
     * @return 用户信息
     */
    User joinCompany(Long userId, String companyName);

    /**
     * 加入学校
     *
     * @param userId     用户ID
     * @param schoolName 学校名称
     * @return 用户信息
     */
    User joinSchool(Long userId, String schoolName);

    /**
     * 获取用户统计数据（我的搭子、我的活动、消息数）
     *
     * @param userId 用户ID
     * @return 统计 Map：partners, activities, messages
     */
    Map<String, Integer> getUserStats(Long userId);
}