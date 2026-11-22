package com.upendra.dispatch.strategy;

import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Vehicle;

import java.util.List;

/**
 * Strategy pattern: the algorithm that assigns orders to vehicles.
 * The service depends on this interface, so a smarter algorithm
 * (e.g. a VRP solver) can be plugged in without touching the service or controller.
 */
public interface DispatchStrategy {

    DispatchResult assign(List<Order> orders, List<Vehicle> vehicles);
}
