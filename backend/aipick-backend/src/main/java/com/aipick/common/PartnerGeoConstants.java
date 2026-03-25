package com.aipick.common;

/**
 * 搭子地理位置约定（便于匹配与推荐）
 * <p>
 * 坐标系：<b>GCJ-02</b>（国测局加密坐标），与微信小程序 {@code wx.chooseLocation}、高德/腾讯地图一致。
 * 若后续用百度地图展示，需 BD-09 转换；距离计算、附近检索可直接用本表经纬度。
 * </p>
 */
public final class PartnerGeoConstants {

    private PartnerGeoConstants() {
    }
}
