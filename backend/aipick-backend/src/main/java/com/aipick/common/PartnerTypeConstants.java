package com.aipick.common;

/**
 * 搭子类型：与 t_partner.type 一致，1～15 为固定文案（仅文字枚举）
 */
public final class PartnerTypeConstants {

    public static final int MIN_CODE = 1;
    public static final int MAX_CODE = 15;

    private PartnerTypeConstants() {
    }

    /**
     * 1 宠物搭子 … 15 户外搭子
     */
    public static String labelOf(Integer type) {
        if (type == null) {
            return "其他";
        }
        return switch (type) {
            case 1 -> "宠物搭子";
            case 2 -> "电影搭子";
            case 3 -> "音乐搭子";
            case 4 -> "逛街搭子";
            case 5 -> "运动搭子";
            case 6 -> "摄影搭子";
            case 7 -> "干饭搭子";
            case 8 -> "旅游搭子";
            case 9 -> "k歌搭子";
            case 10 -> "喝酒搭子";
            case 11 -> "桌游搭子";
            case 12 -> "钓鱼搭子";
            case 13 -> "游戏搭子";
            case 14 -> "聊天搭子";
            case 15 -> "户外搭子";
            default -> "其他";
        };
    }
}
