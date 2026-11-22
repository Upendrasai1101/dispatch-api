package com.upendra.dispatch.mapper;

import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Priority;
import com.upendra.dispatch.domain.Vehicle;
import com.upendra.dispatch.dto.OrderResponse;
import com.upendra.dispatch.dto.DispatchPlanResponse;
import com.upendra.dispatch.dto.OrderRequest;
import com.upendra.dispatch.dto.UnassignedOrderResponse;
import com.upendra.dispatch.dto.VehiclePlanResponse;
import com.upendra.dispatch.dto.VehicleRequest;
import com.upendra.dispatch.dto.VehicleResponse;
import com.upendra.dispatch.strategy.DispatchResult;
import com.upendra.dispatch.strategy.UnassignedOrder;
import com.upendra.dispatch.strategy.VehicleRoute;

import java.util.Locale;

/**
 * Converts between API DTOs and domain objects, so controllers and
 * the dispatch algorithm never depend on each other's data shapes.
 */
public final class DispatchMapper {

    private DispatchMapper() {
    }

    public static Order toOrder(OrderRequest request) {
        return new Order(request.orderId(), request.latitude(), request.longitude(),
                request.address(), request.packageWeight(), toPriority(request.priority()));
    }

    public static void updateOrder(Order order, OrderRequest request) {
        order.updateDetails(request.latitude(), request.longitude(), request.address(),
                request.packageWeight(), toPriority(request.priority()));
    }

    public static Vehicle toVehicle(VehicleRequest request) {
        return new Vehicle(request.vehicleId(), request.capacity(), request.currentLatitude(),
                request.currentLongitude(), request.currentAddress());
    }

    public static void updateVehicle(Vehicle vehicle, VehicleRequest request) {
        vehicle.updateDetails(request.capacity(), request.currentLatitude(),
                request.currentLongitude(), request.currentAddress());
    }

    public static Priority toPriority(String value) {
        return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public static DispatchPlanResponse toPlanResponse(DispatchResult result) {
        return new DispatchPlanResponse(
                result.routes().stream().map(DispatchMapper::toVehiclePlan).toList(),
                result.unassignedOrders().stream().map(DispatchMapper::toUnassigned).toList());
    }

    /** Formats a distance the way the API spec shows it, e.g. "12.34 km". */
    public static String formatDistance(double km) {
        return String.format(Locale.ROOT, "%.2f km", km);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static VehiclePlanResponse toVehiclePlan(VehicleRoute route) {
        return new VehiclePlanResponse(
                route.getVehicle().getVehicleId(),
                round2(route.getTotalLoad()),
                formatDistance(route.getTotalDistanceKm()),
                route.getAssignedOrders().stream().map(DispatchMapper::toOrderResponse).toList());
    }

    public static VehicleResponse toVehicleResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getVehicleId(), vehicle.getCapacity(), vehicle.getCurrentLatitude(),
                vehicle.getCurrentLongitude(), vehicle.getCurrentAddress());
    }

    public static OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(order.getOrderId(), order.getLatitude(), order.getLongitude(),
                order.getAddress(), order.getPackageWeight(), order.getPriority().name());
    }

    private static UnassignedOrderResponse toUnassigned(UnassignedOrder unassigned) {
        Order order = unassigned.order();
        return new UnassignedOrderResponse(order.getOrderId(), order.getPackageWeight(),
                order.getPriority().name(), unassigned.reason());
    }
}
