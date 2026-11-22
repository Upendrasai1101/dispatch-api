package com.upendra.dispatch.dto;

import java.util.List;

/**
 * Demand vs supply overview.
 * demandToCapacityPercent above 100 means some orders cannot be assigned.
 */
public record DispatchSummaryResponse(
        long totalOrders,
        double totalOrderWeight,
        long totalVehicles,
        double totalFleetCapacity,
        double demandToCapacityPercent,
        List<PrioritySummaryResponse> ordersByPriority
) {
}
