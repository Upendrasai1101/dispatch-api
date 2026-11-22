package com.upendra.dispatch.dto;

public record OrderResponse(
        String orderId,
        double latitude,
        double longitude,
        String address,
        double packageWeight,
        String priority
) {
}
