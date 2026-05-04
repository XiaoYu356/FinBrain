package com.finbrain.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    CANCELLED(2, "已取消"),
    REDEEMED(3, "已赎回"),
    MATURED(4, "已到期"),
    SETTLED(5, "已结算");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static boolean canTransit(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        switch (from) {
            case PENDING:
                return to == CONFIRMED || to == CANCELLED;
            case CONFIRMED:
                return to == REDEEMED || to == MATURED || to == CANCELLED;
            case MATURED:
                return to == SETTLED;
            case CANCELLED:
            case REDEEMED:
            case SETTLED:
                return false;
            default:
                return false;
        }
    }

    public static boolean canCancel(OrderStatus status) {
        return status == PENDING || status == CONFIRMED;
    }

    public static boolean canRedeem(OrderStatus status) {
        return status == CONFIRMED;
    }
}
