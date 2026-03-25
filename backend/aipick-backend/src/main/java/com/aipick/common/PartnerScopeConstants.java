package com.aipick.common;

import com.aipick.common.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 搭子可见范围：按位掩码存储，可同时选公开 / 同事 / 校友。
 * <p>
 * 位定义：1=公开，2=同事，4=校友；多选时按位或，例如 3=公开+同事。
 * 历史数据：曾用单列枚举 1/2/3 表示单选，其中 3 表示「校友」；迁移后校友为位 4。
 */
public final class PartnerScopeConstants {

    public static final int BIT_PUBLIC = 1;
    public static final int BIT_COLLEAGUE = 2;
    public static final int BIT_ALUMNI = 4;

    private PartnerScopeConstants() {
    }

    /**
     * 将库中取值规范为位掩码：null→仅公开；历史值 3→校友位。
     */
    public static int normalizeMask(Integer scope) {
        if (scope == null) {
            return BIT_PUBLIC;
        }
        int s = scope;
        if (s == 3) {
            return BIT_ALUMNI;
        }
        return s;
    }

    /**
     * 发布请求：每项为 1、2、4 之一，去重后按位或。
     */
    public static int maskFromScopeList(List<Integer> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new BusinessException("请至少选择一种可见范围");
        }
        Set<Integer> bits = new LinkedHashSet<>();
        for (Integer s : scopes) {
            if (s == null) {
                continue;
            }
            if (s != BIT_PUBLIC && s != BIT_COLLEAGUE && s != BIT_ALUMNI) {
                throw new BusinessException("可见范围参数无效");
            }
            bits.add(s);
        }
        if (bits.isEmpty()) {
            throw new BusinessException("请至少选择一种可见范围");
        }
        int mask = 0;
        for (Integer b : bits) {
            mask |= b;
        }
        return mask;
    }

    /**
     * 展示用：如「公开、同事」。
     */
    public static String labelOf(Integer scope) {
        int m = normalizeMask(scope);
        if (m == 0) {
            return "公开";
        }
        List<String> parts = new ArrayList<>();
        if ((m & BIT_PUBLIC) != 0) {
            parts.add("公开");
        }
        if ((m & BIT_COLLEAGUE) != 0) {
            parts.add("同事");
        }
        if ((m & BIT_ALUMNI) != 0) {
            parts.add("校友");
        }
        if (parts.isEmpty()) {
            return "公开";
        }
        return String.join("、", parts);
    }
}
