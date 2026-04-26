package com.sparklink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparklink.common.BusinessException;
import com.sparklink.common.PartnerPreferenceConstants;
import com.sparklink.common.PartnerScopeConstants;
import com.sparklink.common.PartnerTypeConstants;
import com.sparklink.dto.ApplyPartnerRequest;
import com.sparklink.dto.CreatePartnerRequest;
import com.sparklink.dto.PageRequest;
import com.sparklink.dto.UserPartnerResponse;
import com.sparklink.entity.Partner;
import com.sparklink.entity.PartnerApply;
import com.sparklink.entity.User;
import com.sparklink.mapper.PartnerApplyMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.service.PartnerService;
import com.sparklink.util.AvatarUtil;
import com.sparklink.util.MediaPathUtil;
import com.sparklink.vo.PartnerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搭子服务实现
 *
 * @author AI-Pick
 */
@Service
public class PartnerServiceImpl implements PartnerService {

    private final PartnerMapper partnerMapper;
    private final PartnerApplyMapper partnerApplyMapper;
    private final UserMapper userMapper;

    public PartnerServiceImpl(PartnerMapper partnerMapper, PartnerApplyMapper partnerApplyMapper, UserMapper userMapper) {
        this.partnerMapper = partnerMapper;
        this.partnerApplyMapper = partnerApplyMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Partner createPartner(Long userId, CreatePartnerRequest request) {
        Partner partner = new Partner();
        partner.setUserId(userId);
        partner.setTitle(request.getTitle().trim());
        partner.setContent(request.getContent().trim());
        partner.setPreference(PartnerPreferenceConstants.normalizeAndValidate(request.getPreference()));
        partner.setScope(PartnerScopeConstants.maskFromScopeList(request.getScopes()));
        partner.setType(request.getType());
        partner.setTargetCount(request.getTargetCount() != null ? request.getTargetCount() : 2);
        partner.setCurrentCount(1);
        String loc = request.getLocation();
        if (loc != null && !loc.isBlank()) {
            partner.setLocation(loc.trim());
        } else {
            partner.setLocation(null);
        }
        Double lat = request.getLatitude();
        Double lng = request.getLongitude();
        if (lat != null || lng != null) {
            if (lat == null || lng == null) {
                throw new BusinessException("地图选点纬度与经度需同时填写");
            }
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                throw new BusinessException("地图坐标无效");
            }
            partner.setLatitude(lat);
            partner.setLongitude(lng);
        }
        LocalDateTime planTime = request.getPlanTime();
        LocalDateTime planEndTime = request.getPlanEndTime();
        if (planEndTime != null) {
            if (planTime == null) {
                throw new BusinessException("填写结束时间前请先选择开始时间");
            }
            if (planEndTime.isBefore(planTime)) {
                throw new BusinessException("结束时间不能早于开始时间");
            }
        }
        partner.setPlanTime(planTime);
        partner.setPlanEndTime(planEndTime);
        String coverImage = request.getCoverImage();
        if (coverImage == null || coverImage.isBlank()) {
            coverImage = defaultCoverByPartnerType(request.getType());
        }
        partner.setCoverImage(coverImage);
        partner.setStatus(0);
        partner.setViewCount(0);

        // 将发布者加入应征列表（自动通过）
        partnerMapper.insert(partner);

        PartnerApply apply = new PartnerApply();
        apply.setPartnerId(partner.getId());
        apply.setUserId(userId);
        apply.setMessage("发起者");
        apply.setStatus(1);
        partnerApplyMapper.insert(apply);

        return partner;
    }

    /**
     * 无封面时按类型给默认图（1～15 类型分组映射到现有静态图）
     */
    private static String defaultCoverByPartnerType(Integer type) {
        if (type == null) {
            return "/static/covers/partner-default.png";
        }
        switch (type) {
            case 7:
            case 10:
                return "/static/covers/partner-food.png";
            case 8:
                return "/static/covers/activity-default.png";
            case 5:
            case 15:
                return "/static/covers/partner-sport.png";
            case 14:
                return "/static/covers/partner-study.png";
            case 9:
            case 11:
            case 13:
                return "/static/covers/activity-party.png";
            case 1:
            case 2:
            case 3:
            case 4:
            case 6:
            case 12:
            default:
                return "/static/covers/partner-default.png";
        }
    }

    @Override
    public IPage<PartnerVO> getPartnerList(PageRequest pageRequest, Integer type, Long currentUserId, String scopeType) {
        Page<Partner> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Partner::getStatus, 0);
        if (type != null) {
            wrapper.eq(Partner::getType, type);
        }
        applyVisibilityFilter(wrapper, currentUserId);
        if (currentUserId != null && scopeType != null && !scopeType.isEmpty()) {
            if ("platform".equalsIgnoreCase(scopeType)) {
                wrapper.and(w -> w.isNull(Partner::getScope).or().apply("(scope & 1) <> 0"));
            } else if ("company".equalsIgnoreCase(scopeType)) {
                List<Long> userIds = userIdsByCompany(currentUserId);
                if (userIds.isEmpty()) {
                    wrapper.eq(Partner::getUserId, -1L);
                } else {
                    wrapper.and(w -> w.apply("(scope & 2) <> 0").in(Partner::getUserId, userIds));
                }
            } else if ("school".equalsIgnoreCase(scopeType)) {
                List<Long> userIds = userIdsBySchool(currentUserId);
                if (userIds.isEmpty()) {
                    wrapper.eq(Partner::getUserId, -1L);
                } else {
                    wrapper.and(w -> w.apply("(scope & 4) <> 0").in(Partner::getUserId, userIds));
                }
            }
        }
        wrapper.orderByDesc(Partner::getCreateTime);

        IPage<Partner> result = partnerMapper.selectPage(page, wrapper);

        IPage<PartnerVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toPartnerVOList(result.getRecords()));
        return voPage;
    }

    /**
     * 可见范围（位掩码）：未登录仅公开位；已登录可见本人、含公开位、或同事位且同公司、或校友位且同校
     */
    private void applyVisibilityFilter(LambdaQueryWrapper<Partner> wrapper, Long viewerUserId) {
        if (viewerUserId == null) {
            wrapper.and(w -> w.isNull(Partner::getScope).or().apply("(scope & 1) <> 0"));
            return;
        }
        List<Long> companyIds = userIdsByCompany(viewerUserId);
        List<Long> schoolIds = userIdsBySchool(viewerUserId);
        wrapper.and(w -> {
            w.isNull(Partner::getScope)
                    .or()
                    .apply("(scope & 1) <> 0")
                    .or()
                    .eq(Partner::getUserId, viewerUserId);
            if (!companyIds.isEmpty()) {
                w.or(sub -> sub.apply("(scope & 2) <> 0").in(Partner::getUserId, companyIds));
            }
            if (!schoolIds.isEmpty()) {
                w.or(sub -> sub.apply("(scope & 4) <> 0").in(Partner::getUserId, schoolIds));
            }
        });
    }

    private List<PartnerVO> toPartnerVOList(List<Partner> partners) {
        if (partners == null || partners.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> userIds = partners.stream()
                .map(Partner::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }
        List<PartnerVO> list = new ArrayList<>();
        for (Partner p : partners) {
            list.add(toPartnerVO(p, userMap.get(p.getUserId())));
        }
        return list;
    }

    private PartnerVO toPartnerVO(Partner partner, User author) {
        PartnerVO vo = new PartnerVO();
        vo.setId(partner.getId());
        vo.setUserId(partner.getUserId());
        vo.setTitle(partner.getTitle());
        vo.setDescription(partner.getContent());
        vo.setPreference(partner.getPreference());
        vo.setCoverImage(MediaPathUtil.normalizeForResponse(partner.getCoverImage()));
        vo.setMaxParticipants(partner.getTargetCount());
        vo.setCurrentParticipants(partner.getCurrentCount());
        vo.setScope(partner.getScope());
        vo.setScopeName(PartnerScopeConstants.labelOf(partner.getScope()));
        vo.setTypeCode(partner.getType());
        vo.setTypeName(PartnerTypeConstants.labelOf(partner.getType()));
        if (partner.getType() != null) {
            vo.setType(String.valueOf(partner.getType()));
        }
        vo.setAddress(partner.getLocation());
        vo.setLatitude(partner.getLatitude());
        vo.setLongitude(partner.getLongitude());
        vo.setPlanTime(partner.getPlanTime());
        vo.setPlanEndTime(partner.getPlanEndTime());
        vo.setStatus(partner.getStatus());
        vo.setCreateTime(partner.getCreateTime());
        if (author != null) {
            vo.setNickname(author.getNickname());
            vo.setAvatar(AvatarUtil.sanitizeForResponse(author.getAvatar()));
            vo.setGender(author.getGender());
        }
        return vo;
    }

    @Override
    public PartnerVO getPartnerDetailVO(Long partnerId) {
        Partner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new BusinessException("搭子不存在");
        }

        partner.setViewCount(partner.getViewCount() == null ? 1 : partner.getViewCount() + 1);
        partnerMapper.updateById(partner);

        User author = partner.getUserId() != null ? userMapper.selectById(partner.getUserId()) : null;
        return toPartnerVO(partner, author);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PartnerApply applyPartner(Long userId, Long partnerId, ApplyPartnerRequest request) {
        Partner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new BusinessException("搭子不存在");
        }

        // 检查是否已应征
        LambdaQueryWrapper<PartnerApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartnerApply::getPartnerId, partnerId)
                .eq(PartnerApply::getUserId, userId);
        long count = partnerApplyMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("您已应征过此搭子");
        }

        // 检查是否已满
        if (partner.getCurrentCount() >= partner.getTargetCount()) {
            throw new BusinessException("该搭子已满");
        }

        // 创建应征记录
        PartnerApply apply = new PartnerApply();
        apply.setPartnerId(partnerId);
        apply.setUserId(userId);
        if (request != null) {
            apply.setMessage(request.getMessage());
        }
        apply.setStatus(0);
        partnerApplyMapper.insert(apply);

        // 更新当前人数
        partner.setCurrentCount(partner.getCurrentCount() + 1);
        if (partner.getCurrentCount() >= partner.getTargetCount()) {
            partner.setStatus(1);
        }
        partnerMapper.updateById(partner);

        return apply;
    }

    @Override
    public List<PartnerApply> getApplyList(Long partnerId) {
        LambdaQueryWrapper<PartnerApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartnerApply::getPartnerId, partnerId)
                .orderByDesc(PartnerApply::getCreateTime);
        return partnerApplyMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptApplicant(Long userId, Long partnerId, Long applicantId) {
        Partner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new BusinessException("搭子不存在");
        }
        // 验证是否是发布者
        if (!partner.getUserId().equals(userId)) {
            throw new BusinessException("只有发布者才能接受应征");
        }

        // 查找应征记录
        PartnerApply apply = partnerApplyMapper.selectById(applicantId);
        if (apply == null || !apply.getPartnerId().equals(partnerId)) {
            throw new BusinessException("应征记录不存在");
        }
        if (apply.getStatus() != 0) {
            throw new BusinessException("该应征已被处理");
        }

        // 更新应征状态为已通过
        apply.setStatus(1);
        partnerApplyMapper.updateById(apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectApplicant(Long userId, Long partnerId, Long applicantId) {
        Partner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new BusinessException("搭子不存在");
        }
        // 验证是否是发布者
        if (!partner.getUserId().equals(userId)) {
            throw new BusinessException("只有发布者才能拒绝应征");
        }

        // 查找应征记录
        PartnerApply apply = partnerApplyMapper.selectById(applicantId);
        if (apply == null || !apply.getPartnerId().equals(partnerId)) {
            throw new BusinessException("应征记录不存在");
        }
        if (apply.getStatus() != 0) {
            throw new BusinessException("该应征已被处理");
        }

        // 更新应征状态为已拒绝
        apply.setStatus(2);
        partnerApplyMapper.updateById(apply);

        // 减少当前人数
        partner.setCurrentCount(partner.getCurrentCount() - 1);
        if (partner.getCurrentCount() < partner.getTargetCount()) {
            partner.setStatus(0);
        }
        partnerMapper.updateById(partner);
    }

    @Override
    public List<PartnerVO> getMyPartners(Long userId, String type) {
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<>();

        if ("joined".equals(type)) {
            LambdaQueryWrapper<PartnerApply> applyWrapper = new LambdaQueryWrapper<>();
            applyWrapper.eq(PartnerApply::getUserId, userId)
                    .eq(PartnerApply::getStatus, 1);
            List<PartnerApply> applies = partnerApplyMapper.selectList(applyWrapper);
            if (applies.isEmpty()) {
                return List.of();
            }
            List<Long> partnerIds = applies.stream().map(PartnerApply::getPartnerId).toList();
            wrapper.in(Partner::getId, partnerIds);
        } else {
            // created、published、null：仅我发布的搭子
            wrapper.eq(Partner::getUserId, userId);
        }

        wrapper.orderByDesc(Partner::getCreateTime);
        List<Partner> list = partnerMapper.selectList(wrapper);
        return toPartnerVOList(list);
    }

    @Override
    public IPage<Partner> filterPartners(PageRequest pageRequest, Integer type, String location,
                                        LocalDateTime planTimeStart, LocalDateTime planTimeEnd, Integer gender) {
        Page<Partner> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        IPage<Partner> result = partnerMapper.selectPageByFilter(page, type, location, planTimeStart, planTimeEnd, gender);
        
        // 批量查询发布者信息，避免 N+1 问题
        List<Long> userIds = result.getRecords().stream()
                .map(Partner::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }
        
        // 补充发布者信息
        for (Partner partner : result.getRecords()) {
            User user = userMap.get(partner.getUserId());
            if (user != null) {
                partner.setCreateBy(user.getId());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePartner(Long userId, Long partnerId) {
        Partner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new BusinessException("搭子不存在");
        }
        // 验证是否是发布者
        if (!partner.getUserId().equals(userId)) {
            throw new BusinessException("只有发布者才能删除搭子");
        }
        
        // 删除应征记录
        LambdaQueryWrapper<PartnerApply> applyWrapper = new LambdaQueryWrapper<>();
        applyWrapper.eq(PartnerApply::getPartnerId, partnerId);
        partnerApplyMapper.delete(applyWrapper);
        
        // 删除搭子
        partnerMapper.deleteById(partnerId);
    }

    /** 与当前用户同公司的用户ID列表（用于同事搭） */
    private List<Long> userIdsByCompany(Long currentUserId) {
        User user = userMapper.selectById(currentUserId);
        if (user == null || user.getCompanyName() == null || user.getCompanyName().isBlank()) {
            return Collections.emptyList();
        }
        String company = user.getCompanyName().trim();
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getCompanyName, company).select(User::getId);
        return userMapper.selectList(w).stream().map(User::getId).collect(Collectors.toList());
    }

    /** 与当前用户同学校的用户ID列表（用于校友搭） */
    private List<Long> userIdsBySchool(Long currentUserId) {
        User user = userMapper.selectById(currentUserId);
        if (user == null || user.getSchoolName() == null || user.getSchoolName().isBlank()) {
            return Collections.emptyList();
        }
        String school = user.getSchoolName().trim();
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getSchoolName, school).select(User::getId);
        return userMapper.selectList(w).stream().map(User::getId).collect(Collectors.toList());
    }

    @Override
    public List<UserPartnerResponse> getUserVisiblePartners(Long targetUserId, User currentUser, PageRequest pageRequest) {
        // 查询目标用户发布的所有搭子
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Partner::getUserId, targetUserId);
        wrapper.orderByDesc(Partner::getCreateTime);
        
        // 分页查询
        Page<Partner> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        IPage<Partner> result = partnerMapper.selectPage(page, wrapper);
        
        if (result.getRecords().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取目标用户信息（用于权限判断）
        User targetUser = userMapper.selectById(targetUserId);
        
        // 过滤可见搭子
        List<UserPartnerResponse> visiblePartners = new ArrayList<>();
        for (Partner partner : result.getRecords()) {
            if (isPartnerVisible(partner, targetUser, currentUser)) {
                visiblePartners.add(toUserPartnerResponse(partner));
            }
        }
        
        return visiblePartners;
    }
    
    /**
     * 判断搭子是否对当前用户可见
     */
    private boolean isPartnerVisible(Partner partner, User targetUser, User currentUser) {
        Integer scope = partner.getScope();
        if (scope == null) {
            scope = 1; // 默认为公开
        }
        
        // 公开搭子（scope & 1）：所有用户可见
        if ((scope & 1) != 0) {
            return true;
        }
        
        // 如果当前用户未登录，只能看到公开搭子
        if (currentUser == null) {
            return false;
        }
        
        // 自己看自己发布的搭子，全部可见
        if (currentUser.getId().equals(partner.getUserId())) {
            return true;
        }
        
        // 公司限定（scope & 2）：仅同公司用户可见
        if ((scope & 2) != 0) {
            String targetCompany = targetUser != null ? targetUser.getCompanyName() : null;
            String currentCompany = currentUser.getCompanyName();
            if (targetCompany != null && !targetCompany.isBlank() 
                    && currentCompany != null && !currentCompany.isBlank()
                    && targetCompany.equals(currentCompany)) {
                return true;
            }
        }
        
        // 校友限定（scope & 4）：仅同校用户可见
        if ((scope & 4) != 0) {
            String targetSchool = targetUser != null ? targetUser.getSchoolName() : null;
            String currentSchool = currentUser.getSchoolName();
            if (targetSchool != null && !targetSchool.isBlank() 
                    && currentSchool != null && !currentSchool.isBlank()
                    && targetSchool.equals(currentSchool)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 将 Partner 转换为 UserPartnerResponse
     */
    private UserPartnerResponse toUserPartnerResponse(Partner partner) {
        UserPartnerResponse response = new UserPartnerResponse();
        response.setId(partner.getId());
        response.setTitle(partner.getTitle());
        response.setTypeCode(partner.getType());
        response.setTypeName(PartnerTypeConstants.labelOf(partner.getType()));
        response.setCoverImage(MediaPathUtil.normalizeForResponse(partner.getCoverImage()));
        response.setStatus(partner.getStatus());
        response.setStatusName(getStatusName(partner.getStatus()));
        response.setPreference(partner.getPreference());
        
        // 解析标签（从 preference 或其他字段）
        response.setTags(parseTags(partner.getPreference()));
        
        response.setCurrentCount(partner.getCurrentCount());
        response.setMaxCount(partner.getTargetCount());
        response.setScope(partner.getScope());
        response.setScopeName(PartnerScopeConstants.labelOf(partner.getScope()));
        response.setCreateTime(partner.getCreateTime());
        
        return response;
    }
    
    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "招募中";
            case 1: return "已满";
            case 2: return "已结束";
            default: return "未知";
        }
    }
    
    /**
     * 解析标签
     */
    private List<String> parseTags(String preference) {
        if (preference == null || preference.isBlank()) {
            return new ArrayList<>();
        }
        // 简单按逗号或空格分割，可根据实际需求调整
        String[] tags = preference.split("[,，、\\s]+");
        List<String> result = new ArrayList<>();
        for (String tag : tags) {
            if (!tag.trim().isEmpty()) {
                result.add(tag.trim());
            }
        }
        return result;
    }
}