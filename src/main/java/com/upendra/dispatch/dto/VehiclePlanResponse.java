package com.upendra.dispatch.dto;

import java.util.List;

public record VehiclePlanResponse(
        String vehicleId,
        double totalLoad,
        String totalDistance,
        List<OrderResponse> assignedOrders
) {
}
