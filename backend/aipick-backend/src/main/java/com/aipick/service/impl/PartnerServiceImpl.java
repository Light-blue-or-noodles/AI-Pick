package com.aipick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aipick.common.BusinessException;
import com.aipick.dto.ApplyPartnerRequest;
import com.aipick.dto.CreatePartnerRequest;
import com.aipick.dto.PageRequest;
import com.aipick.entity.Partner;
import com.aipick.entity.PartnerApply;
import com.aipick.entity.User;
import com.aipick.mapper.PartnerApplyMapper;
import com.aipick.mapper.PartnerMapper;
import com.aipick.mapper.UserMapper;
import com.aipick.service.PartnerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        partner.setTitle(request.getTitle());
        partner.setContent(request.getContent());
        partner.setType(request.getType());
        partner.setTargetCount(request.getTargetCount() != null ? request.getTargetCount() : 2);
        partner.setCurrentCount(1);
        partner.setLocation(request.getLocation());
        partner.setPlanTime(request.getPlanTime());
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

    /** 按搭子类型返回默认封面：1-吃饭 2-旅游 3-运动 4-学习 5-游戏 6-其他 */
    private static String defaultCoverByPartnerType(Integer type) {
        if (type == null) {
            return "/static/covers/partner-default.png";
        }
        switch (type) {
            case 1: return "/static/covers/partner-food.png";
            case 2: return "/static/covers/activity-default.png";
            case 3: return "/static/covers/partner-sport.png";
            case 4: return "/static/covers/partner-study.png";
            case 5: return "/static/covers/activity-party.png";
            case 6: return "/static/covers/partner-default.png";
            default: return "/static/covers/partner-default.png";
        }
    }

    @Override
    public IPage<Partner> getPartnerList(PageRequest pageRequest, Integer type) {
        Page<Partner> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Partner::getStatus, 0);
        if (type != null) {
            wrapper.eq(Partner::getType, type);
        }
        wrapper.orderByDesc(Partner::getCreateTime);

        IPage<Partner> result = partnerMapper.selectPage(page, wrapper);

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
    public Partner getPartnerDetail(Long partnerId) {
        Partner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new BusinessException("搭子不存在");
        }

        // 增加浏览量
        partner.setViewCount(partner.getViewCount() + 1);
        partnerMapper.updateById(partner);

        return partner;
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
    public List<Partner> getMyPartners(Long userId, String type) {
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<>();
        
        if ("created".equals(type)) {
            // 我发布的
            wrapper.eq(Partner::getUserId, userId);
        } else if ("joined".equals(type)) {
            // 我参加的（通过应征记录查询）
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
            // 默认返回所有
            wrapper.eq(Partner::getUserId, userId);
        }
        
        wrapper.orderByDesc(Partner::getCreateTime);
        return partnerMapper.selectList(wrapper);
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
}