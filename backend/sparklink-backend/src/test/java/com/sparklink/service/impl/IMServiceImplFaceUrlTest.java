package com.sparklink.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IMServiceImplFaceUrlTest {

    @Test
    void toImFacePath_stripsHttpIpToStatic() {
        assertEquals(
                "/static/avatars/28_x.jpg",
                IMServiceImpl.toImFacePathOrExternalHttps(
                        "http://59.110.0.107:8080/api/static/avatars/28_x.jpg"));
    }

    @Test
    void toImFacePath_keepsHttpsThirdParty() {
        assertEquals(
                "https://third.example/mmopen/abc",
                IMServiceImpl.toImFacePathOrExternalHttps("https://third.example/mmopen/abc"));
    }

    @Test
    void pickBase_ipv4PrimaryUsesHttpsFallback() {
        assertEquals(
                "https://www.aipick.cloud/api",
                IMServiceImpl.pickImAvatarPublicBase(
                        "http://59.110.0.107:8080/api", "https://www.aipick.cloud/api"));
    }

    @Test
    void pickBase_domainPrimaryUnchanged() {
        assertEquals(
                "https://www.aipick.cloud/api",
                IMServiceImpl.pickImAvatarPublicBase(
                        "https://www.aipick.cloud/api", "https://other.example/api"));
    }

    @Test
    void pickBase_ipv4AndEmptyFallback() {
        assertNull(IMServiceImpl.pickImAvatarPublicBase("http://59.110.0.107:8080/api", ""));
    }
}
