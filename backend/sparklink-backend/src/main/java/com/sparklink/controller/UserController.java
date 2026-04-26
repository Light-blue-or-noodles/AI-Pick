package com.sparklink.controller;

import com.sparklink.common.BusinessException;
import com.sparklink.common.Result;
import com.sparklink.dto.LoginRequest;

import java.util.Map;

import com.sparklink.storage.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import com.sparklink.dto.LoginResponse;
import com.sparklink.dto.RegisterRequest;
import com.sparklink.dto.UpdateUserRequest;
import com.sparklink.dto.WechatLoginRequest;
import com.sparklink.dto.UserInfoDTO;
import com.sparklink.dto.UserProfileResponse;
import com.sparklink.dto.UserPartnerResponse;
import com.sparklink.dto.PageRequest;
import com.sparklink.entity.User;
import com.sparklink.service.UserService;
import com.sparklink.service.FollowService;
import com.sparklink.service.PartnerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /** 与 spring.servlet.multipart.max-file-size 一致 */
    private static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] AVATAR_ALLOWED = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final UserService userService;
    private final FollowService followService;
    private final PartnerService partnerService;
    private final ImageStorageService imageStorageService;

    public UserController(UserService userService, FollowService followService, 
                          PartnerService partnerService, ImageStorageService imageStorageService) {
        this.userService = userService;
        this.followService = followService;
        this.partnerService = partnerService;
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
     * 小程序开发联调：签发 id=1 用户的真实 JWT，便于 IM UserSig 等需鉴权接口。
     * 仅当 app.allow-test-login=true；生产环境须关闭。
     */
    @PostMapping("/test-login")
    public Result<LoginResponse> testLogin() {
        LoginResponse response = userService.testLogin();
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
        String contentType = normalizeAvatarContentType(file.getContentType());
        if (contentType == null || !java.util.Arrays.asList(AVATAR_ALLOWED).contains(contentType)) {
            throw new BusinessException("仅支持 JPG/PNG/GIF/WEBP");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        Map<String, String> stored = imageStorageService.storeAvatar(file, userId);
        String urlPath = stored.get("url");
        userService.saveUploadedAvatarAndSyncIm(userId, urlPath);
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

    /**
     * 获取用户资料（用户详情页）
     * GET /api/user/{id}/profile
     */
    @GetMapping("/{id}/profile")
    public Result<UserProfileResponse> getUserProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @PathVariable("id") Long userId) {
        User user = userService.getUserInfo(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(com.sparklink.util.AvatarUtil.sanitizeForResponse(user.getAvatar()));
        response.setBio(user.getBio());
        response.setCompanyName(user.getCompanyName());
        response.setSchoolName(user.getSchoolName());
        response.setFollowerCount(followService.getFollowerCount(userId));
        response.setFollowingCount(followService.getFollowingCount(userId));
        
        // 检查当前登录用户是否已关注该用户
        boolean isFollowed = false;
        if (currentUserId != null && !currentUserId.equals(userId)) {
            isFollowed = followService.isFollowing(currentUserId, userId);
        }
        response.setIsFollowed(isFollowed);
        
        // 获取当前登录用户信息（用于权限判断）
        User currentUser = null;
        if (currentUserId != null) {
            try {
                currentUser = userService.getUserInfo(currentUserId);
            } catch (Exception e) {
                // 忽略错误，currentUser 保持为 null
            }
        }
        
        // 获取用户可见的搭子数量
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNum(1);
        pageRequest.setPageSize(Integer.MAX_VALUE);
        List<UserPartnerResponse> visiblePartners = partnerService.getUserVisiblePartners(userId, currentUser, pageRequest);
        response.setPartnersCount(visiblePartners.size());
        
        return Result.success(response);
    }

    /**
     * 获取用户发布的搭子列表（用户详情页）
     * GET /api/user/{id}/partners
     * 权限控制：仅返回当前用户可见的搭子
     */
    @GetMapping("/{id}/partners")
    public Result<List<UserPartnerResponse>> getUserPartners(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @PathVariable("id") Long userId,
            @Valid @ModelAttribute PageRequest pageRequest) {
        // 获取目标用户信息
        User targetUser = userService.getUserInfo(userId);
        if (targetUser == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 获取当前登录用户信息（用于权限判断）
        User currentUser = null;
        if (currentUserId != null) {
            try {
                currentUser = userService.getUserInfo(currentUserId);
            } catch (Exception e) {
                // 忽略错误，currentUser 保持为 null
            }
        }
        
        // 调用 Service 获取可见搭子列表
        List<UserPartnerResponse> partners = partnerService.getUserVisiblePartners(
                userId, currentUser, pageRequest);
        
        return Result.success(partners);
    }

    /** 部分客户端/系统上报 image/jpg 或与标准有大小写差异，统一为校验用 MIME。 */
    private static String normalizeAvatarContentType(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim().toLowerCase();
        if ("image/jpg".equals(t)) {
            return "image/jpeg";
        }
        return t;
    }

    private UserInfoDTO toUserInfoDTO(User user) {
        if (user == null) {
            return null;
        }
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(com.sparklink.util.AvatarUtil.sanitizeForResponse(user.getAvatar()));
        dto.setGender(user.getGender());
        dto.setBio(user.getBio());
        dto.setCompanyName(user.getCompanyName());
        dto.setCompanyVerified(user.getCompanyVerified());
        dto.setSchoolName(user.getSchoolName());
        dto.setSchoolVerified(user.getSchoolVerified());
        dto.setBirthday(user.getBirthday());
        // 保证前端总能拿到 tags 字段（空时返回空字符串，便于编辑页回显）
        dto.setTags(user.getTags() != null ? user.getTags() : "");
        dto.setLocation(user.getLocation() != null ? user.getLocation() : "");
        return dto;
    }
}
