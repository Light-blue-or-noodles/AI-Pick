package com.aipick.storage;

import com.aipick.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 对象存储占位实现：接入阿里云 OSS / 腾讯云 COS 等时在此实现 {@link ImageStorageService}。
 *
 * @author AI-Pick
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "oss")
public class OssImageStorageService implements ImageStorageService {

    @Override
    public Map<String, String> storeAvatar(MultipartFile file, Long userId) {
        throw new BusinessException("OSS 存储尚未接入，请使用 storage.type=local");
    }

    @Override
    public Map<String, String> storeActivityImage(MultipartFile file, Long userId) {
        throw new BusinessException("OSS 存储尚未接入，请使用 storage.type=local");
    }
}
