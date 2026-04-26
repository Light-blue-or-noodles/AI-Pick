package com.sparklink.util;

/**
 * 地理距离工具（球面大圆距离）
 *
 * @author AI-Pick
 */
public final class DistanceUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceUtil() {
    }

    /**
     * Haversine 公式计算两点间距离（千米）
     *
     * @param lat1 点1 纬度（度）
     * @param lon1 点1 经度（度）
     * @param lat2 点2 纬度（度）
     * @param lon2 点2 经度（度）
     * @return 距离（km），任一点无效时返回 {@link Double#POSITIVE_INFINITY}
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        if (!isValidLatLon(lat1, lon1) || !isValidLatLon(lat2, lon2)) {
            return Double.POSITIVE_INFINITY;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private static boolean isValidLatLon(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isNaN(lon)
                && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
}
