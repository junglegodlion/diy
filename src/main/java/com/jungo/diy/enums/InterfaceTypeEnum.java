package com.jungo.diy.enums;

import lombok.Getter;

/**
 * @author lichuang3
 */

@Getter
public enum InterfaceTypeEnum {
    // 关键路径
    CRITICAL_LINK(1, "默认类型 关键路径"),
    // 五大金刚
    FIVE_GANG_JING(2, "五大金刚"),
    // 首屏tab
    FIRST_SCREEN_TAB(3, "首屏tab"),
    // 麒麟组件接口
    QILIN_COMPONENT_INTERFACE(4, "麒麟组件接口"),
    // 其他核心业务接口
    OTHER_CORE_BUSINESS_INTERFACE(5, "其他核心业务接口"),

    ACCESS_VOLUME_TOP30(6, "访问量前30");

    private final Integer code;
    private final String description;

    InterfaceTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InterfaceTypeEnum fromCode(Integer code) {
        for (InterfaceTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的接口类型编码: " + code);
    }
}
