package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.ApplyPartnerRequest;
import com.aipick.dto.CreatePartnerRequest;
import com.aipick.dto.PageRequest;
import com.aipick.entity.Partner;
import com.aipick.entity.PartnerApply;
import com.aipick.service.PartnerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搭子控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/partner")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    /**
     * 发布搭子
     */
    @PostMapping
    public Result<Partner> createPartner(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreatePartnerRequest request) {
        Partner partner = partnerService.createPartner(userId, request);
        return Result.success("发布成功", partner);
    }

    /**
     * 搭子筛选：按兴趣类型、位置、计划时间、发布者性别
     * 参数均为可选，不传则不过滤该维度
     */
    @GetMapping("/filter")
    public Result<IPage<Partner>> filterPartners(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime planTimeStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime planTimeEnd,
            @RequestParam(required = false) Integer gender) {
        IPage<Partner> list = partnerService.filterPartners(pageRequest, type, location, planTimeStart, planTimeEnd, gender);
        return Result.success(list);
    }

    /**
     * 搭子列表
     */
    @GetMapping
    public Result<IPage<Partner>> getPartnerList(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) Integer type) {
        IPage<Partner> list = partnerService.getPartnerList(pageRequest, type);
        return Result.success(list);
    }

    /**
     * 搭子详情
     */
    @GetMapping("/{id}")
    public Result<Partner> getPartnerDetail(@PathVariable Long id) {
        Partner partner = partnerService.getPartnerDetail(id);
        return Result.success(partner);
    }

    /**
     * 应征搭子
     */
    @PostMapping("/{id}/apply")
    public Result<PartnerApply> applyPartner(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody(required = false) ApplyPartnerRequest request) {
        PartnerApply apply = partnerService.applyPartner(userId, id, request);
        return Result.success("应征成功", apply);
    }

    /**
     * 搭子应征列表
     */
    @GetMapping("/{id}/applicants")
    public Result<List<PartnerApply>> getApplyList(@PathVariable Long id) {
        List<PartnerApply> list = partnerService.getApplyList(id);
        return Result.success(list);
    }

    /**
     * 接受应征
     */
    @PostMapping("/{partnerId}/accept")
    public Result<Void> acceptApplicant(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long partnerId,
            @RequestParam Long applicantId) {
        partnerService.acceptApplicant(userId, partnerId, applicantId);
        return Result.success("接受成功", null);
    }

    /**
     * 拒绝应征
     */
    @PostMapping("/{partnerId}/reject")
    public Result<Void> rejectApplicant(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long partnerId,
            @RequestParam Long applicantId) {
        partnerService.rejectApplicant(userId, partnerId, applicantId);
        return Result.success("拒绝成功", null);
    }

    /**
     * 我的搭子
     */
    @GetMapping("/my")
    public Result<List<Partner>> getMyPartners(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type) {
        List<Partner> list = partnerService.getMyPartners(userId, type);
        return Result.success(list);
    }

    /**
     * 删除搭子
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePartner(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        partnerService.deletePartner(userId, id);
        return Result.success("删除成功", null);
    }
}