package com.aipick.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传存储：当前实现为本地磁盘；后续可切换为 OSS 实现本接口。
 *
 * @author AI-Pick
 */
public interface ImageStorageService {

    /**
     * 保存用户头像，返回可供持久化的公开路径（相对路径 /static/...，不含域名）
     *
     * @param file   上传文件
     * @param userId 用户 ID（用于文件名）
     * @return 包含 path 键的 Map，与现有接口兼容
     */
    Map<String, String> storeAvatar(MultipartFile file, Long userId);

    /**
     * 保存活动配图
     *
     * @param file   上传文件
     * @param userId 上传者
     * @return 包含 path 键的 Map
     */
    Map<String, String> storeActivityImage(MultipartFile file, Long userId);
}
