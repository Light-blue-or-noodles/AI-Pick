package com.aipick.storage;

import com.aipick.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 本地磁盘图片存储：文件落在 {@link StorageProperties.Local#getRootDir()} 下，对外路径统一为 /static/...
 *
 * @author AI-Pick
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    private static final String ACTIVITY_IMAGES_SUBDIR = "activity-images";

    private final StorageProperties storageProperties;
    private final String avatarSubdir;

    public LocalImageStorageService(StorageProperties storageProperties,
                                    @Value("${upload.avatar-subdir:avatars}") String avatarSubdir) {
        this.storageProperties = storageProperties;
        this.avatarSubdir = avatarSubdir;
    }

    @Override
    public Map<String, String> storeAvatar(MultipartFile file, Long userId) {
        String filename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                + extensionFromContentType(file.getContentType(), "jpg");
        Path target = resolveAndPrepareDir(avatarSubdir).resolve(filename);
        transferTo(file, target);
        String publicPath = "/static/" + avatarSubdir + "/" + filename;
        return Map.of("url", publicPath, "path", publicPath);
    }

    @Override
    public Map<String, String> storeActivityImage(MultipartFile file, Long userId) {
        String filename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                + extensionFromContentType(file.getContentType(), "jpg");
        Path target = resolveAndPrepareDir(ACTIVITY_IMAGES_SUBDIR).resolve(filename);
        transferTo(file, target);
        String publicPath = "/static/" + ACTIVITY_IMAGES_SUBDIR + "/" + filename;
        return Map.of("url", publicPath, "path", publicPath);
    }

    private Path resolveAndPrepareDir(String subdir) {
        String root = storageProperties.getLocal().getRootDir();
        Path basePath = Paths.get(root).toAbsolutePath().normalize();
        Path dir = basePath.resolve(subdir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("创建目录失败：" + e.getMessage());
        }
        return dir;
    }

    private static void transferTo(MultipartFile file, Path target) {
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException("保存失败：" + e.getMessage());
        }
    }

    private static String extensionFromContentType(String contentType, String defaultExt) {
        if (contentType == null) {
            return "." + defaultExt;
        }
        if (contentType.contains("png")) {
            return ".png";
        }
        if (contentType.contains("gif")) {
            return ".gif";
        }
        if (contentType.contains("webp")) {
            return ".webp";
        }
        return "." + defaultExt;
    }
}
