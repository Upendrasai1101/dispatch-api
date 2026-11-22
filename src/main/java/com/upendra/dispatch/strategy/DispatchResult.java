package com.upendra.dispatch.strategy;

import java.util.List;

/**
 * Output of a dispatch strategy: one route per vehicle plus orders that could not be placed.
 */
public record DispatchResult(List<VehicleRoute> routes, List<UnassignedOrder> unassignedOrders) {

    public DispatchResult {
        routes = List.copyOf(routes);
        unassignedOrders = List.copyOf(unassignedOrders);
    }
}
