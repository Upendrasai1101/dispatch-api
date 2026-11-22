package com.upendra.dispatch.strategy;

import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Working state of one vehicle while the plan is being built:
 * where it is now, how much it carries and how far it has travelled.
 */
public class VehicleRoute {

    /** Tolerance so floating-point sums like 0.1 + 0.2 never block an exact fit. */
    private static final double EPSILON = 1e-9;

    private final Vehicle vehicle;
    private final List<Order> assignedOrders = new ArrayList<>();
    private double currentLatitude;
    private double currentLongitude;
    private double totalLoad;
    private double totalDistanceKm;

    public VehicleRoute(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.currentLatitude = vehicle.getCurrentLatitude();
        this.currentLongitude = vehicle.getCurrentLongitude();
    }

    public boolean canCarry(double weight) {
        return weight <= remainingCapacity() + EPSILON;
    }

    public double remainingCapacity() {
        return vehicle.getCapacity() - totalLoad;
    }

    /**
     * Adds the order to this route: the vehicle drives legKm to the drop point,
     * picks up the load, and its position becomes the drop location.
     */
    void assign(Order order, double legKm) {
        assignedOrders.add(order);
        totalLoad += order.getPackageWeight();
        totalDistanceKm += legKm;
        currentLatitude = order.getLatitude();
        currentLongitude = order.getLongitude();
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public List<Order> getAssignedOrders() {
        return Collections.unmodifiableList(assignedOrders);
    }

    public double getCurrentLatitude() {
        return currentLatitude;
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }

    public double getTotalLoad() {
        return totalLoad;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }
}
