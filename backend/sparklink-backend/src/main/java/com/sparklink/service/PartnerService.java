package com.sparklink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sparklink.dto.ApplyPartnerRequest;
import com.sparklink.dto.CreatePartnerRequest;
import com.sparklink.dto.PageRequest;
import com.sparklink.dto.UserPartnerResponse;
import com.sparklink.entity.Partner;
import com.sparklink.entity.PartnerApply;
import com.sparklink.entity.User;
import com.sparklink.vo.PartnerVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搭子服务接口
 *
 * @author AI-Pick
 */
public interface PartnerService {

    /**
     * 发布搭子
     *
     * @param userId 用户ID
     * @param request 发布请求
     * @return 搭子信息
     */
    Partner createPartner(Long userId, CreatePartnerRequest request);

    /**
     * 分页查询搭子列表
     *
     * @param pageRequest   分页请求
     * @param type          搭子类型（可选）
     * @param currentUserId 当前用户ID（可选，用于同事搭/校友搭筛选）
     * @param scopeType     展示范围：company-同公司，school-同学校，platform-全平台
     * @return 搭子分页列表
     */
    IPage<PartnerVO> getPartnerList(PageRequest pageRequest, Integer type, Long currentUserId, String scopeType);

    /**
     * 获取搭子详情
     *
     * @param partnerId 搭子ID
     * @return 搭子详情
     */
    PartnerVO getPartnerDetailVO(Long partnerId);

    /**
     * 应征搭子
     *
     * @param userId  用户ID
     * @param partnerId 搭子ID
     * @param request 应征请求
     * @return 应征记录
     */
    PartnerApply applyPartner(Long userId, Long partnerId, ApplyPartnerRequest request);

    /**
     * 获取搭子应征列表
     *
     * @param partnerId 搭子ID
     * @return 应征列表
     */
    java.util.List<PartnerApply> getApplyList(Long partnerId);

    /**
     * 接受应征
     *
     * @param userId      用户ID（搭子发布者）
     * @param partnerId   搭子ID
     * @param applicantId 应征者ID
     */
    void acceptApplicant(Long userId, Long partnerId, Long applicantId);

    /**
     * 拒绝应征
     *
     * @param userId      用户ID（搭子发布者）
     * @param partnerId   搭子ID
     * @param applicantId 应征者ID
     */
    void rejectApplicant(Long userId, Long partnerId, Long applicantId);

    /**
     * 我的搭子列表
     *
     * @param userId 用户 ID
     * @param type   created-仅我发布的；joined-我应征通过的；null 或空则同 created
     */
    List<PartnerVO> getMyPartners(Long userId, String type);

    /**
     * 搭子筛选：按兴趣类型、位置、计划时间、发布者性别
     *
     * @param pageRequest   分页参数
     * @param type          搭子类型 1～15，见 PartnerTypeConstants
     * @param location      位置关键词（模糊匹配）
     * @param planTimeStart 计划时间起
     * @param planTimeEnd   计划时间止
     * @param gender        发布者性别 0-未知 1-男 2-女
     * @return 搭子分页列表
     */
    IPage<Partner> filterPartners(PageRequest pageRequest, Integer type, String location,
                                  LocalDateTime planTimeStart, LocalDateTime planTimeEnd, Integer gender);

    /**
     * 删除搭子
     *
     * @param userId 用户 ID
     * @param partnerId 搭子 ID
     */
    void deletePartner(Long userId, Long partnerId);

    /**
     * 获取用户可见的搭子列表（用于用户详情页）
     * 权限控制：
     * - 公开搭子（scope & 1）：所有用户可见
     * - 公司限定（scope & 2）：仅同公司用户可见
     * - 校友限定（scope & 4）：仅同校用户可见
     *
     * @param targetUserId 目标用户ID（被查看的用户）
     * @param currentUser  当前登录用户（可为null）
     * @param pageRequest  分页参数
     * @return 可见搭子列表
     */
    List<UserPartnerResponse> getUserVisiblePartners(Long targetUserId, User currentUser, PageRequest pageRequest);
}