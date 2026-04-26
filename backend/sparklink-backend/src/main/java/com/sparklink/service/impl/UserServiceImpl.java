package com.sparklink.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sparklink.common.BusinessException;
import com.sparklink.config.JwtUtils;
import com.sparklink.dto.LoginRequest;
import com.sparklink.dto.LoginResponse;
import com.sparklink.dto.RegisterRequest;
import com.sparklink.dto.UpdateUserRequest;
import com.sparklink.dto.WechatLoginRequest;
import com.sparklink.entity.Activity;
import com.sparklink.entity.ActivityRegistration;
import com.sparklink.entity.Partner;
import com.sparklink.entity.User;
import com.sparklink.entity.UserMessage;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.ActivityRegistrationMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.mapper.UserMessageMapper;
import com.sparklink.service.FollowService;
import com.sparklink.service.IMService;
import com.sparklink.service.UserService;
import com.sparklink.util.AvatarUtil;
import com.sparklink.util.MediaPathUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 用户服务实现
 *
 * @author AI-Pick
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PartnerMapper partnerMapper;
    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper activityRegistrationMapper;
    private final UserMessageMapper userMessageMapper;
    private final IMService imService;
    private final FollowService followService;
    
    /** 公司/学校名称最小长度 */
    private static final int MIN_NAME_LENGTH = 2;
    
    /** 公司/学校名称最大长度 */
    private static final int MAX_NAME_LENGTH = 100;
    
    /** 变更频率限制（天） */
    private static final int CHANGE_COOLDOWN_DAYS = 30;
    
    /** 公司名称验证正则（中文、英文、数字、常见符号） */
    private static final Pattern COMPANY_NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9\\-\\&\\(\\)\\s]+$");
    
    /** 学校名称验证正则（中文、英文、数字、常见符号） */
    private static final Pattern SCHOOL_NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9\\-\\&\\(\\)\\s]+(大学 | 学院 | 学校 | 中学 | 小学)?$");
    
    @Value("${wechat.appid:}")
    private String appid;

    @Value("${wechat.secret:}")
    private String secret;

    @Value("${app.allow-test-login:false}")
    private boolean allowTestLogin;

    /**
     * 微信侧未拿到真实昵称或隐私策略时，getUserProfile 常见占位，不可用来覆盖库里已完善的资料。
     */
    private static final String WECHAT_PLACEHOLDER_NICKNAME = "微信用户";

    public UserServiceImpl(UserMapper userMapper, JwtUtils jwtUtils,
                           PartnerMapper partnerMapper, ActivityMapper activityMapper,
                           ActivityRegistrationMapper activityRegistrationMapper,
                           UserMessageMapper userMessageMapper,
                           IMService imService,
                           FollowService followService) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.partnerMapper = partnerMapper;
        this.activityMapper = activityMapper;
        this.activityRegistrationMapper = activityRegistrationMapper;
        this.userMessageMapper = userMessageMapper;
        this.imService = imService;
        this.followService = followService;
    }

    @Override
    public User register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existUser = getUserByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setStatus(0);
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);
        imService.importUserToIm(user.getId(), user.getNickname(),
                AvatarUtil.sanitizeForResponse(user.getAvatar()));
        return user;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = getUserByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        imService.importUserToIm(user.getId(), user.getNickname(),
                AvatarUtil.sanitizeForResponse(user.getAvatar()));

        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getNickname(), AvatarUtil.sanitizeForResponse(user.getAvatar()));
    }

    @Override
    public LoginResponse wechatLogin(WechatLoginRequest request) {
        String openid = getOpenidFromWechat(request.getCode());
        
        if (openid == null) {
            throw new BusinessException("微信登录失败，请重试");
        }
        
        // 查询是否已存在用户
        User user = getUserByOpenid(openid);
        boolean isNew = false;
        
        if (user == null) {
            // 新用户，自动创建（表 t_user 要求 username/password 非空，微信用户用占位；BCrypt 要求密码 8-56 字符，不可用空串）
            // username 限制 VARCHAR(50)，openid 可能很长，用 wx_ + SHA256(openid) 前 44 位 hex 保证唯一且 ≤50 字符
            user = new User();
            user.setUsername(wechatUsername(openid));
            user.setPassword(BCrypt.hashpw("wechat_placeholder", BCrypt.gensalt()));
            user.setOpenid(openid);
            user.setNickname("微信用户");
            user.setStatus(0);
            user.setCreateTime(LocalDateTime.now());
            
            // 如果有传入用户信息
            if (request.getUserInfo() != null) {
                if (shouldApplyWechatProfileNickname(request.getUserInfo().getNickname())) {
                    user.setNickname(request.getUserInfo().getNickname().trim());
                }
                if (StringUtils.hasText(request.getUserInfo().getAvatar())) {
                    String av = MediaPathUtil.normalizeForPersistence(request.getUserInfo().getAvatar().trim());
                    if (AvatarUtil.isValidAvatarUrl(av)) {
                        user.setAvatar(av);
                    }
                }
                if (request.getUserInfo().getGender() != null) {
                    user.setGender(request.getUserInfo().getGender());
                }
            }
            
            userMapper.insert(user);
            isNew = true;
        } else {
            // 检查用户状态
            if (user.getStatus() != null && user.getStatus() == 1) {
                throw new BusinessException("账号已被禁用");
            }
            
            /* 更新用户信息：若本次仅带回微信占位昵称，则整块 userInfo 视为不可信，避免头像/性别也被错误同步 */
            if (request.getUserInfo() != null) {
                WechatLoginRequest.UserInfo ui = request.getUserInfo();
                boolean nickIsOnlyWechatPlaceholder = StringUtils.hasText(ui.getNickname())
                        && isWechatPlaceholderNickname(ui.getNickname());
                if (!nickIsOnlyWechatPlaceholder) {
                    boolean needUpdate = false;
                    if (shouldApplyWechatProfileNickname(ui.getNickname())) {
                        user.setNickname(ui.getNickname().trim());
                        needUpdate = true;
                    }
                    if (shouldApplyWechatLoginAvatarToExistingUser(user.getAvatar(), ui.getAvatar())) {
                        String av = MediaPathUtil.normalizeForPersistence(ui.getAvatar().trim());
                        if (AvatarUtil.isValidAvatarUrl(av)) {
                            user.setAvatar(av);
                            needUpdate = true;
                        }
                    }
                    if (ui.getGender() != null) {
                        user.setGender(ui.getGender());
                        needUpdate = true;
                    }
                    if (needUpdate) {
                        userMapper.updateById(user);
                    }
                }
            }
        }

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), "wechat");

        imService.importUserToIm(user.getId(), user.getNickname(),
                AvatarUtil.sanitizeForResponse(user.getAvatar()));

        return new LoginResponse(token, user.getId(),
                String.valueOf(user.getId()),
                user.getNickname(), AvatarUtil.sanitizeForResponse(user.getAvatar()), isNew);
    }

    @Override
    public LoginResponse testLogin() {
        if (!allowTestLogin) {
            throw new BusinessException("测试登录未开启");
        }
        final Long testUserId = 1L;
        User user = userMapper.selectById(testUserId);
        if (user == null) {
            throw new BusinessException("测试用户不存在，请保证数据库中存在 id=1 的用户");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("账号已被禁用");
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        imService.importUserToIm(user.getId(), user.getNickname(),
                AvatarUtil.sanitizeForResponse(user.getAvatar()));
        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getNickname(), AvatarUtil.sanitizeForResponse(user.getAvatar()));
    }

    private static boolean isWechatPlaceholderNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return true;
        }
        return WECHAT_PLACEHOLDER_NICKNAME.equals(nickname.trim());
    }

    /**
     * 仅非空且非微信默认占位昵称时写入昵称，避免 getUserProfile 返回「微信用户」覆盖老用户资料。
     */
    private static boolean shouldApplyWechatProfileNickname(String nickname) {
        return StringUtils.hasText(nickname) && !isWechatPlaceholderNickname(nickname);
    }

    /**
     * 老用户微信登录：库里已是本站上传的 static 头像时，不要用 getUserProfile 的 thirdwx 地址覆盖。
     * （库不会被 IM 回写；覆盖来源是每次 wechat-login 请求的 userInfo.avatar。）
     */
    private static boolean shouldApplyWechatLoginAvatarToExistingUser(String currentDbAvatar, String incomingWechatAvatar) {
        if (!StringUtils.hasText(incomingWechatAvatar)) {
            return false;
        }
        if (AvatarUtil.isSiteStaticAvatarRef(currentDbAvatar)) {
            return false;
        }
        String av = MediaPathUtil.normalizeForPersistence(incomingWechatAvatar.trim());
        return AvatarUtil.isValidAvatarUrl(av);
    }

    /**
     * 生成微信用户唯一用户名，满足 t_user.username VARCHAR(50) 且唯一
     */
    private static String wechatUsername(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "wx_anonymous";
        }
        if (openid.length() <= 47) {
            return "wx_" + openid;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(openid.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return "wx_" + hex.substring(0, 44);
        } catch (NoSuchAlgorithmException e) {
            return "wx_" + openid.substring(0, 47);
        }
    }

    /**
     * 调用微信 API 获取 openid
     *
     * @param code 微信授权码
     * @return openid
     */
    private String getOpenidFromWechat(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
            throw new BusinessException("微信配置不完整：请设置环境变量 WECHAT_SECRET，"
                    + "或在 backend/sparklink-backend 目录的 .env 中填写（与公众平台小程序 AppSecret 一致）；"
                    + "并确认 wechat.appid 与当前小程序 AppID 一致。");
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.weixin.qq.com/sns/jscode2session" +
                    "?appid=" + appid +
                    "&secret=" + secret +
                    "&js_code=" + code +
                    "&grant_type=authorization_code";

            // 有些环境可能将返回类型标记为 text/plain，这里先拿到原始字符串再手动解析
            String body = restTemplate.getForObject(url, String.class);
            if (!StringUtils.hasText(body)) {
                throw new BusinessException("微信 API 返回为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(body, Map.class);

            // 检查错误码
            if (response.containsKey("errcode")) {
                Integer errcode = (Integer) response.get("errcode");
                String errmsg = (String) response.get("errmsg");
                throw new BusinessException("微信登录失败：" + errmsg + " (errcode=" + errcode + ")");
            }

            String openid = (String) response.get("openid");
            if (!StringUtils.hasText(openid)) {
                throw new BusinessException("微信 API 未返回 openid");
            }

            return openid;
        } catch (Exception e) {
            throw new BusinessException("调用微信 API 失败：" + e.getMessage());
        }
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 隐藏敏感信息
        user.setPassword(null);
        user.setOpenid(null);
        return user;
    }

    @Override
    public User updateUserInfo(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        boolean syncImProfile = false;

        // 更新非空字段
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
            syncImProfile = true;
        }
        if (StringUtils.hasText(request.getAvatar())) {
            String avatar = MediaPathUtil.normalizeForPersistence(request.getAvatar().trim());
            if (AvatarUtil.isValidAvatarUrl(avatar)) {
                user.setAvatar(avatar);
                syncImProfile = true;
            }
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (StringUtils.hasText(request.getBio())) {
            user.setBio(request.getBio());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday().trim().isEmpty() ? null : request.getBirthday().trim());
        }
        if (request.getTags() != null) {
            user.setTags(request.getTags().trim().isEmpty() ? null : request.getTags().trim());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation().trim().isEmpty() ? null : request.getLocation().trim());
        }
        if (request.getCompanyName() != null) {
            user.setCompanyName(request.getCompanyName());
        }
        if (request.getSchoolName() != null) {
            user.setSchoolName(request.getSchoolName());
        }

        userMapper.updateById(user);
        if (syncImProfile) {
            imService.importUserToIm(user.getId(), user.getNickname(),
                    AvatarUtil.sanitizeForResponse(user.getAvatar()));
        }
        user.setPassword(null);
        user.setOpenid(null);
        return user;
    }

    @Override
    public User saveUploadedAvatarAndSyncIm(Long userId, String storedUrlPath) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID无效");
        }
        if (!StringUtils.hasText(storedUrlPath)) {
            throw new BusinessException("头像路径无效");
        }
        String normalized = MediaPathUtil.normalizeForPersistence(storedUrlPath.trim());
        if (!AvatarUtil.isValidAvatarUrl(normalized)) {
            throw new BusinessException("头像路径不合法");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setAvatar(normalized);
        userMapper.updateById(user);
        imService.importUserToIm(user.getId(), user.getNickname(),
                AvatarUtil.sanitizeForResponse(user.getAvatar()));
        user.setPassword(null);
        user.setOpenid(null);
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public User getUserByOpenid(String openid) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenid, openid);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public Map<String, Integer> getUserStats(Long userId) {
        LambdaQueryWrapper<Partner> partnerWrapper = new LambdaQueryWrapper<>();
        partnerWrapper.eq(Partner::getUserId, userId);
        long partners = partnerMapper.selectCount(partnerWrapper);

        // 活动数：我报名的活动（报名记录数，status=0 表示已报名）
        LambdaQueryWrapper<ActivityRegistration> regWrapper = new LambdaQueryWrapper<>();
        regWrapper.eq(ActivityRegistration::getUserId, userId)
                .eq(ActivityRegistration::getStatus, 0);
        long activities = activityRegistrationMapper.selectCount(regWrapper);

        LambdaQueryWrapper<UserMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(UserMessage::getReceiverId, userId).or().eq(UserMessage::getSenderId, userId);
        long messages = userMessageMapper.selectCount(messageWrapper);
        int following = followService.getFollowingCount(userId);
        int followers = followService.getFollowerCount(userId);

        Map<String, Integer> stats = new HashMap<>();
        stats.put("partners", (int) partners);
        stats.put("activities", (int) activities);
        stats.put("messages", (int) messages);
        stats.put("following", following);
        stats.put("followers", followers);
        return stats;
    }

    @Override
    public User joinCompany(Long userId, String companyName) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 输入验证
        validateCompanyName(companyName);
        
        // 检查变更频率限制
        checkCompanyChangeFrequency(user);
        
        LocalDateTime now = LocalDateTime.now();
        boolean isFirstJoin = user.getCompanyJoinTime() == null;
        
        // 设置公司信息
        user.setCompanyName(companyName);
        
        // 如果是首次加入，设置加入时间；否则更新变更时间
        if (isFirstJoin) {
            user.setCompanyJoinTime(now);
            // 首次加入默认未验证，需要后续验证流程
            user.setCompanyVerified(false);
        } else {
            user.setLastCompanyChangeTime(now);
            // 变更公司后，验证状态重置为未验证
            user.setCompanyVerified(false);
        }
        
        userMapper.updateById(user);
        user.setPassword(null);
        user.setOpenid(null);
        return user;
    }
    
    /**
     * 验证公司名称
     */
    private void validateCompanyName(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            throw new BusinessException("公司名称不能为空");
        }
        
        String trimmedName = companyName.trim();
        if (trimmedName.length() < MIN_NAME_LENGTH) {
            throw new BusinessException("公司名称长度不能少于" + MIN_NAME_LENGTH + "个字符");
        }
        
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("公司名称长度不能超过" + MAX_NAME_LENGTH + "个字符");
        }
        
        if (!COMPANY_NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new BusinessException("公司名称格式不正确，只能包含中文、英文、数字和常见符号");
        }
    }
    
    /**
     * 检查公司变更频率
     */
    private void checkCompanyChangeFrequency(User user) {
        if (user.getLastCompanyChangeTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            long daysSinceLastChange = ChronoUnit.DAYS.between(user.getLastCompanyChangeTime(), now);
            
            if (daysSinceLastChange < CHANGE_COOLDOWN_DAYS) {
                throw new BusinessException("公司信息变更过于频繁，请距上次变更" + CHANGE_COOLDOWN_DAYS + "天后再修改");
            }
        }
    }

    @Override
    public User joinSchool(Long userId, String schoolName) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 输入验证
        validateSchoolName(schoolName);
        
        // 检查变更频率限制
        checkSchoolChangeFrequency(user);
        
        LocalDateTime now = LocalDateTime.now();
        boolean isFirstJoin = user.getSchoolJoinTime() == null;
        
        // 设置学校信息
        user.setSchoolName(schoolName);
        
        // 如果是首次加入，设置加入时间；否则更新变更时间
        if (isFirstJoin) {
            user.setSchoolJoinTime(now);
            // 首次加入默认未验证，需要后续验证流程
            user.setSchoolVerified(false);
        } else {
            user.setLastSchoolChangeTime(now);
            // 变更学校后，验证状态重置为未验证
            user.setSchoolVerified(false);
        }
        
        userMapper.updateById(user);
        user.setPassword(null);
        user.setOpenid(null);
        return user;
    }
    
    /**
     * 验证学校名称
     */
    private void validateSchoolName(String schoolName) {
        if (!StringUtils.hasText(schoolName)) {
            throw new BusinessException("学校名称不能为空");
        }
        
        String trimmedName = schoolName.trim();
        if (trimmedName.length() < MIN_NAME_LENGTH) {
            throw new BusinessException("学校名称长度不能少于" + MIN_NAME_LENGTH + "个字符");
        }
        
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("学校名称长度不能超过" + MAX_NAME_LENGTH + "个字符");
        }
        
        if (!SCHOOL_NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new BusinessException("学校名称格式不正确");
        }
    }
    
    /**
     * 检查学校变更频率
     */
    private void checkSchoolChangeFrequency(User user) {
        if (user.getLastSchoolChangeTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            long daysSinceLastChange = ChronoUnit.DAYS.between(user.getLastSchoolChangeTime(), now);
            
            if (daysSinceLastChange < CHANGE_COOLDOWN_DAYS) {
                throw new BusinessException("学校信息变更过于频繁，请距上次变更" + CHANGE_COOLDOWN_DAYS + "天后再修改");
            }
        }
    }
}
