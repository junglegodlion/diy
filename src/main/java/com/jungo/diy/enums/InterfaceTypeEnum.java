package com.jungo.diy.enums;

import lombok.Getter;

@Getter
public enum InterfaceTypeEnum {
    DEFAULT(1, "默认类型 关键路径");

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
