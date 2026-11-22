package com.upendra.dispatch.dto;

import java.util.List;

public record OrderListResponse(int count, List<OrderResponse> orders) {
}
