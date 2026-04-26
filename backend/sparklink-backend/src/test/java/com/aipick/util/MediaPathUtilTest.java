package com.sparklink.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * MediaPathUtil 自测
 */
class MediaPathUtilTest {

    @Test
    void normalizeForPersistence_stripsApiPrefix() {
        assertEquals("/static/avatars/a.jpg", MediaPathUtil.normalizeForPersistence("/api/static/avatars/a.jpg"));
        assertEquals("/static/avatars/a.jpg",
                MediaPathUtil.normalizeForPersistence("http://127.0.0.1:8080/api/static/avatars/a.jpg"));
    }

    @Test
    void normalizeForPersistence_keepsThirdPartyHttps() {
        String wx = "https://thirdwx.qlogo.cn/mmopen/v_xxx/0";
        assertEquals(wx, MediaPathUtil.normalizeForPersistence(wx));
    }

    @Test
    void normalizeForResponse_stripsHostForStatic() {
        assertEquals("/static/covers/x.png",
                MediaPathUtil.normalizeForResponse("http://192.168.1.5:8080/api/static/covers/x.png"));
    }

    @Test
    void normalizeForResponse_null() {
        assertNull(MediaPathUtil.normalizeForResponse(null));
        assertNull(MediaPathUtil.normalizeForResponse("  "));
    }
}
