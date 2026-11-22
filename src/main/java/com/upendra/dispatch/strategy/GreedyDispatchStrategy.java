package com.upendra.dispatch.strategy;

import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Vehicle;
import com.upendra.dispatch.util.DistanceCalculator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy, priority-first nearest-vehicle assignment.
 *
 * 1. Sort orders: HIGH before MEDIUM before LOW; heavier first within a priority
 *    (big packages are hardest to fit, so place them while space is free);
 *    then by orderId so the result is always the same for the same input.
 * 2. For each order, among vehicles that still have room, pick the one whose
 *    CURRENT position (start point or last drop) is nearest. Ties go to the
 *    smaller vehicleId.
 * 3. Move that vehicle to the drop point and add the leg distance and weight.
 * 4. If no vehicle has room, the order is reported as unassigned with a reason.
 *
 * Time complexity: O(n log n + n * m) for n orders and m vehicles.
 */
@Component
public class GreedyDispatchStrategy implements DispatchStrategy {

    public static final String REASON_EXCEEDS_MAX_CAPACITY =
            "Package weight exceeds the capacity of every vehicle in the fleet";
    public static final String REASON_NO_REMAINING_CAPACITY =
            "No vehicle has enough remaining capacity for this package";

    static final Comparator<Order> DISPATCH_ORDER =
            Comparator.comparingInt((Order order) -> order.getPriority().getRank())
                    .thenComparing(Comparator.comparingDouble(Order::getPackageWeight).reversed())
                    .thenComparing(Order::getOrderId);

    private final DistanceCalculator distanceCalculator;

    public GreedyDispatchStrategy(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public DispatchResult assign(List<Order> orders, List<Vehicle> vehicles) {
        List<VehicleRoute> routes = vehicles.stream()
                .sorted(Comparator.comparing(Vehicle::getVehicleId))
                .map(VehicleRoute::new)
                .toList();

        double largestCapacity = vehicles.stream()
                .mapToDouble(Vehicle::getCapacity)
                .max()
                .orElse(0);

        List<UnassignedOrder> unassigned = new ArrayList<>();

        for (Order order : orders.stream().sorted(DISPATCH_ORDER).toList()) {
            VehicleRoute nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (VehicleRoute route : routes) {
                if (!route.canCarry(order.getPackageWeight())) {
                    continue;
                }
                double distance = distanceCalculator.distanceKm(
                        route.getCurrentLatitude(), route.getCurrentLongitude(),
                        order.getLatitude(), order.getLongitude());
                // strict '<' keeps the earlier (smaller vehicleId) route on a tie
                if (distance < nearestDistance) {
                    nearest = route;
                    nearestDistance = distance;
                }
            }

            if (nearest == null) {
                String reason = order.getPackageWeight() > largestCapacity
                        ? REASON_EXCEEDS_MAX_CAPACITY
                        : REASON_NO_REMAINING_CAPACITY;
                unassigned.add(new UnassignedOrder(order, reason));
            } else {
                nearest.assign(order, nearestDistance);
            }
        }

        return new DispatchResult(routes, unassigned);
    }
}
