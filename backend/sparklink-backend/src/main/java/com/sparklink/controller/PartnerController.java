package com.sparklink.controller;

import com.sparklink.common.BusinessException;
import com.sparklink.common.Result;
import com.sparklink.dto.ApplyPartnerRequest;
import com.sparklink.dto.CreatePartnerRequest;
import com.sparklink.dto.PageRequest;
import com.sparklink.entity.Partner;
import com.sparklink.entity.PartnerApply;
import com.sparklink.service.PartnerService;
import com.sparklink.storage.ImageStorageService;
import com.sparklink.vo.PartnerVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 搭子控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/partner")
public class PartnerController {

    private static final long IMAGE_MAX_SIZE = 5 * 1024 * 1024;
    private static final String[] IMAGE_ALLOWED = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final PartnerService partnerService;
    private final ImageStorageService imageStorageService;

    public PartnerController(PartnerService partnerService, ImageStorageService imageStorageService) {
        this.partnerService = partnerService;
        this.imageStorageService = imageStorageService;
    }

    /**
     * 搭子封面等配图上传（存储逻辑与活动图一致，落盘 activity-images 子目录）
     */
    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadPartnerImage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "file", required = false) MultipartFile filePart,
            @RequestParam(value = "image", required = false) MultipartFile imagePart) {
        MultipartFile file = (filePart != null && !filePart.isEmpty()) ? filePart
                : (imagePart != null && !imagePart.isEmpty() ? imagePart : null);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(IMAGE_ALLOWED).contains(contentType)) {
            throw new BusinessException("仅支持 JPG/PNG/GIF/WEBP");
        }
        if (file.getSize() > IMAGE_MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        Map<String, String> stored = imageStorageService.storeActivityImage(file, userId);
        String urlPath = stored.get("url");
        return Result.success("上传成功", Map.of("url", urlPath));
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
     * @param scopeType 列表主题：platform-仅含勾选「公开」的帖；company-含「同事」且发布者同公司；school-含「校友」且发布者同校
     */
    @GetMapping
    public Result<IPage<PartnerVO>> getPartnerList(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) Integer type,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String scopeType) {
        IPage<PartnerVO> list = partnerService.getPartnerList(pageRequest, type, userId, scopeType);
        return Result.success(list);
    }

    /**
     * 搭子详情
     */
    @GetMapping("/{id}")
    public Result<PartnerVO> getPartnerDetail(@PathVariable Long id) {
        PartnerVO vo = partnerService.getPartnerDetailVO(id);
        return Result.success(vo);
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
     * 我的搭子：默认仅返回当前用户发布的搭子（type=created 或 published）；type=joined 返回应征通过的
     */
    @GetMapping("/my")
    public Result<List<PartnerVO>> getMyPartners(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type) {
        List<PartnerVO> list = partnerService.getMyPartners(userId, type);
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