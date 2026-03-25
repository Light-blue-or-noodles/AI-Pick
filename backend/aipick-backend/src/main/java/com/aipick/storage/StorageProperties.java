package com.aipick.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 图片与媒体存储配置：本地目录或后续 OSS（storage.type=oss）。
 * <p>
 * 约定：入库与 API 返回的「本站资源」使用相对路径 {@code /static/...}，不含主机与 /api 前缀，
 * 便于切换本机 IP、域名与 CDN；第三方头像（微信 CDN）与对象存储公网 URL 可存完整 https 地址。
 * </p>
 *
 * @author AI-Pick
 */
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 存储类型：local（默认，写入 upload 目录）| oss（后续接入云存储）
     */
    private String type = "local";

    private final Local local = new Local();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Local getLocal() {
        return local;
    }

    /**
     * 本地磁盘根目录（与 {@code upload.dir} 对齐，默认 uploads）
     */
    public static class Local {
        /**
         * 绝对或相对项目根目录的路径，与 WebMvc 中 file: 映射一致
         */
        private String rootDir = "uploads";

        public String getRootDir() {
            return rootDir;
        }

        public void setRootDir(String rootDir) {
            this.rootDir = rootDir;
        }
    }
}
