package com.aipick.controller;

import com.aipick.common.BusinessException;
import com.aipick.common.Result;
import com.aipick.dto.LoginRequest;

import java.util.Map;

import com.aipick.storage.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import com.aipick.dto.LoginResponse;
import com.aipick.dto.RegisterRequest;
import com.aipick.dto.UpdateUserRequest;
import com.aipick.dto.WechatLoginRequest;
import com.aipick.dto.UserInfoDTO;
import com.aipick.entity.User;
import com.aipick.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] AVATAR_ALLOWED = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final UserService userService;

    private final ImageStorageService imageStorageService;

    public UserController(UserService userService, ImageStorageService imageStorageService) {
        this.userService = userService;
        this.imageStorageService = imageStorageService;
    }

    /**
     * 微信小程序一键登录
     */
    @PostMapping("/wechat-login")
    public Result<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        LoginResponse response = userService.wechatLogin(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册（用户名 + 密码）
     */
    @PostMapping("/register")
    public Result<UserInfoDTO> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        UserInfoDTO dto = toUserInfoDTO(user);
        return Result.success("注册成功", dto);
    }

    /**
     * 用户登录（用户名 + 密码）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoDTO> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        User user = userService.getUserInfo(userId);
        UserInfoDTO dto = toUserInfoDTO(user);
        return Result.success(dto);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<UserInfoDTO> updateUserInfo(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUserInfo(userId, request);
        UserInfoDTO dto = toUserInfoDTO(user);
        return Result.success("更新成功", dto);
    }

    /**
     * 获取用户统计数据（我的搭子、我的活动、消息数）
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getUserStats(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Integer> stats = userService.getUserStats(userId);
        return Result.success(stats);
    }

    /**
     * 上传头像（临时保存到项目目录 uploads/avatars，返回可访问 URL）
     * 兼容 form 字段名 file 或 image（部分客户端上传使用 image）
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "file", required = false) MultipartFile filePart,
            @RequestParam(value = "image", required = false) MultipartFile imagePart) {
        MultipartFile file = (filePart != null && !filePart.isEmpty()) ? filePart : (imagePart != null && !imagePart.isEmpty() ? imagePart : null);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !java.util.Arrays.asList(AVATAR_ALLOWED).contains(contentType)) {
            throw new BusinessException("仅支持 JPG/PNG/GIF/WEBP");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 2MB");
        }
        Map<String, String> stored = imageStorageService.storeAvatar(file, userId);
        String urlPath = stored.get("url");
        return Result.success("上传成功", Map.of("url", urlPath));
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/bind-phone")
    public Result<Void> bindPhone(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> request) {
        // TODO: 实现绑定手机号逻辑
        return Result.success("绑定成功", null);
    }

    /**
     * 加入公司
     */
    @PostMapping("/company")
    public Result<UserInfoDTO> joinCompany(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> request) {
        String companyName = request != null ? request.get("companyName") : null;
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new BusinessException("公司名称不能为空");
        }
        User user = userService.joinCompany(userId, companyName.trim());
        UserInfoDTO dto = toUserInfoDTO(user);
        return Result.success("加入成功", dto);
    }

    /**
     * 加入学校
     */
    @PostMapping("/school")
    public Result<UserInfoDTO> joinSchool(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> request) {
        String schoolName = request != null ? request.get("schoolName") : null;
        if (schoolName == null || schoolName.trim().isEmpty()) {
            throw new BusinessException("学校名称不能为空");
        }
        User user = userService.joinSchool(userId, schoolName.trim());
        UserInfoDTO dto = toUserInfoDTO(user);
        return Result.success("加入成功", dto);
    }

    private UserInfoDTO toUserInfoDTO(User user) {
        if (user == null) {
            return null;
        }
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(com.aipick.util.AvatarUtil.sanitizeForResponse(user.getAvatar()));
        dto.setGender(user.getGender());
        dto.setBio(user.getBio());
        dto.setCompanyName(user.getCompanyName());
        dto.setCompanyVerified(user.getCompanyVerified());
        dto.setSchoolName(user.getSchoolName());
        dto.setSchoolVerified(user.getSchoolVerified());
        dto.setBirthday(user.getBirthday());
        // 保证前端总能拿到 tags 字段（空时返回空字符串，便于编辑页回显）
        dto.setTags(user.getTags() != null ? user.getTags() : "");
        return dto;
    }
}
