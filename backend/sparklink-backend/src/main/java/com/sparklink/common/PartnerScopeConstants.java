package com.sparklink.common;

import com.sparklink.common.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 搭子可见范围：按位掩码存储，可同时选公开 / 同事 / 校友。
 * <p>
 * 位定义：1=公开，2=同事，4=校友；多选时按位或，例如 3=1+2=公开+同事、5=1+4=公开+校友。
 * <p>
 * 注意：整数 <b>3</b> 在掩码下表示「公开+同事」，<b>不可</b>与「仅校友」混淆；仅校友的掩码是 <b>4</b>（或 5=公开+校友 等）。
 */
public final class PartnerScopeConstants {

    public static final int BIT_PUBLIC = 1;
    public static final int BIT_COLLEAGUE = 2;
    public static final int BIT_ALUMNI = 4;

    private PartnerScopeConstants() {
    }

    /**
     * 将库中取值规范为位掩码：null→仅展示作公开。
     * <p>
     * 不再将 3 映射为 4：在位掩码语义下 3=1|2=「公开+同事」。旧版曾用单值 3 表示「校友」的脏数据
     * 应通过一次性 SQL 修正为 4，而不能在每次展示时把 3 当成校友（否则会误标「公司可见」为校友）。
     */
    public static int normalizeMask(Integer scope) {
        if (scope == null) {
            return BIT_PUBLIC;
        }
        return scope;
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
