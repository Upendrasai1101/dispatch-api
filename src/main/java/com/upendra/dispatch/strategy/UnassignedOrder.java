package com.upendra.dispatch.strategy;

import com.upendra.dispatch.domain.Order;

public record UnassignedOrder(Order order, String reason) {
}
