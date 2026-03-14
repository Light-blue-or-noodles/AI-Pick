package com.aipick.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.aipick.dto.ApplyPartnerRequest;
import com.aipick.dto.CreatePartnerRequest;
import com.aipick.dto.PageRequest;
import com.aipick.entity.Partner;
import com.aipick.entity.PartnerApply;

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
     * @param pageRequest 分页请求
     * @param type        搭子类型
     * @return 搭子分页列表
     */
    IPage<Partner> getPartnerList(PageRequest pageRequest, Integer type);

    /**
     * 获取搭子详情
     *
     * @param partnerId 搭子ID
     * @return 搭子详情
     */
    Partner getPartnerDetail(Long partnerId);

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
     * 获取我的搭子
     *
     * @param userId 用户ID
     * @param type   类型：joined-我参加的，created-我发布的
     * @return 搭子列表
     */
    List<Partner> getMyPartners(Long userId, String type);

    /**
     * 搭子筛选：按兴趣类型、位置、计划时间、发布者性别
     *
     * @param pageRequest   分页参数
     * @param type          搭子类型 1-吃饭 2-旅游 3-运动 4-学习 5-游戏 6-其他
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
}