package com.upendra.dispatch.dto;

public record UnassignedOrderResponse(
        String orderId,
        double packageWeight,
        String priority,
        String reason
) {
}
