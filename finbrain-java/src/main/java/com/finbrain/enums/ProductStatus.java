package com.finbrain.enums;

import lombok.Getter;

@Getter
public enum ProductStatus {
    
    DRAFT(0, "草稿"),
    FUNDRAISING(1, "募集期"),
    OPERATING(2, "存续期"),
    MATURED(3, "已到期"),
    DELISTED(4, "已下架");
    
    private final Integer code;
    private final String desc;
    
    ProductStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static ProductStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProductStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    public static boolean canTransit(ProductStatus from, ProductStatus to) {
        if (from == null || to == null) {
            return false;
        }
        switch (from) {
            case DRAFT:
                return to == FUNDRAISING;
            case FUNDRAISING:
                return to == OPERATING || to == DELISTED;
            case OPERATING:
                return to == MATURED || to == DELISTED;
            case MATURED:
            case DELISTED:
                return false;
            default:
                return false;
        }
    }
}
