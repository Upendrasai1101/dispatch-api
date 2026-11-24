package com.upendra.dispatch.strategy;

import com.upendra.dispatch.SampleData;
import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Priority;
import com.upendra.dispatch.domain.Vehicle;
import com.upendra.dispatch.util.DistanceCalculator;
import com.upendra.dispatch.util.HaversineDistanceCalculator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class GreedyDispatchStrategyTest {

    // Delhi, Mumbai and Bengaluru: far apart so "nearest" is obvious
    private static final double DEL_LAT = 28.6139, DEL_LON = 77.2090;
    private static final double MUM_LAT = 19.0760, MUM_LON = 72.8777;
    private static final double BLR_LAT = 12.9716, BLR_LON = 77.5946;

    private final DistanceCalculator distance = new HaversineDistanceCalculator();
    private final GreedyDispatchStrategy strategy = new GreedyDispatchStrategy(distance);

    @Test
    void highPriorityIsAssignedBeforeLowWhenCapacityIsTight() {
        Vehicle van = vehicle("VEH001", 10, DEL_LAT, DEL_LON);
        Order lowNearby = order("ORD001", 10, Priority.LOW, DEL_LAT, DEL_LON);
        Order highFarAway = order("ORD002", 10, Priority.HIGH, MUM_LAT, MUM_LON);

        DispatchResult result = strategy.assign(List.of(lowNearby, highFarAway), List.of(van));

        assertThat(result.routes().get(0).getAssignedOrders()).containsExactly(highFarAway);
        assertThat(result.unassignedOrders()).hasSize(1);
        assertThat(result.unassignedOrders().get(0).order()).isEqualTo(lowNearby);
        assertThat(result.unassignedOrders().get(0).reason())
                .isEqualTo(GreedyDispatchStrategy.REASON_NO_REMAINING_CAPACITY);
    }

    @Test
    void nearestVehicleWithRoomIsChosen() {
        Vehicle delhiVan = vehicle("VEH001", 100, DEL_LAT, DEL_LON);
        Vehicle mumbaiVan = vehicle("VEH002", 100, MUM_LAT, MUM_LON);
        Order mumbaiOrder = order("ORD001", 10, Priority.HIGH, MUM_LAT + 0.01, MUM_LON);

        DispatchResult result = strategy.assign(List.of(mumbaiOrder), List.of(delhiVan, mumbaiVan));

        assertThat(route(result, "VEH001").getAssignedOrders()).isEmpty();
        assertThat(route(result, "VEH002").getAssignedOrders()).containsExactly(mumbaiOrder);
    }

    @Test
    void fullVehicleIsSkippedEvenIfNearest() {
        Vehicle nearbySmall = vehicle("VEH001", 5, DEL_LAT, DEL_LON);
        Vehicle farBig = vehicle("VEH002", 100, MUM_LAT, MUM_LON);
        Order heavyDelhiOrder = order("ORD001", 20, Priority.HIGH, DEL_LAT, DEL_LON);

        DispatchResult result = strategy.assign(List.of(heavyDelhiOrder), List.of(nearbySmall, farBig));

        assertThat(route(result, "VEH002").getAssignedOrders()).containsExactly(heavyDelhiOrder);
    }

    @Test
    void vehicleMovesToEachDropAndDistanceAccumulates() {
        Vehicle van = vehicle("VEH001", 100, DEL_LAT, DEL_LON);
        Order first = order("ORD001", 20, Priority.HIGH, MUM_LAT, MUM_LON);
        Order second = order("ORD002", 10, Priority.HIGH, BLR_LAT, BLR_LON);

        DispatchResult result = strategy.assign(List.of(second, first), List.of(van));

        VehicleRoute route = result.routes().get(0);
        double expected = distance.distanceKm(DEL_LAT, DEL_LON, MUM_LAT, MUM_LON)
                + distance.distanceKm(MUM_LAT, MUM_LON, BLR_LAT, BLR_LON);
        assertThat(route.getAssignedOrders()).containsExactly(first, second);
        assertThat(route.getTotalDistanceKm()).isCloseTo(expected, within(1e-6));
        assertThat(route.getTotalLoad()).isEqualTo(30.0);
        assertThat(route.getCurrentLatitude()).isEqualTo(BLR_LAT);
    }

    @Test
    void orderHeavierThanEveryVehicleGetsMaxCapacityReason() {
        Vehicle van = vehicle("VEH001", 50, DEL_LAT, DEL_LON);
        Order tooHeavy = order("ORD001", 51, Priority.HIGH, DEL_LAT, DEL_LON);

        DispatchResult result = strategy.assign(List.of(tooHeavy), List.of(van));

        assertThat(result.unassignedOrders()).singleElement()
                .extracting(UnassignedOrder::reason)
                .isEqualTo(GreedyDispatchStrategy.REASON_EXCEEDS_MAX_CAPACITY);
    }

    @Test
    void tieBetweenEquallyNearVehiclesGoesToSmallerVehicleId() {
        Vehicle b = vehicle("VEH002", 100, DEL_LAT, DEL_LON);
        Vehicle a = vehicle("VEH001", 100, DEL_LAT, DEL_LON);
        Order order = order("ORD001", 10, Priority.HIGH, MUM_LAT, MUM_LON);

        DispatchResult result = strategy.assign(List.of(order), List.of(b, a));

        assertThat(route(result, "VEH001").getAssignedOrders()).containsExactly(order);
        assertThat(result.routes()).extracting(r -> r.getVehicle().getVehicleId())
                .containsExactly("VEH001", "VEH002");
    }

    @Test
    void ordersAreSortedByPriorityThenHeavierFirstThenId() {
        Order lowHeavy = order("ORD001", 90, Priority.LOW, DEL_LAT, DEL_LON);
        Order highLight = order("ORD002", 5, Priority.HIGH, DEL_LAT, DEL_LON);
        Order highHeavy = order("ORD003", 30, Priority.HIGH, DEL_LAT, DEL_LON);
        Order mediumB = order("ORD005", 10, Priority.MEDIUM, DEL_LAT, DEL_LON);
        Order mediumA = order("ORD004", 10, Priority.MEDIUM, DEL_LAT, DEL_LON);

        List<Order> sorted = List.of(lowHeavy, highLight, highHeavy, mediumB, mediumA).stream()
                .sorted(GreedyDispatchStrategy.DISPATCH_ORDER)
                .toList();

        assertThat(sorted).containsExactly(highHeavy, highLight, mediumA, mediumB, lowHeavy);
    }

    @Test
    void exactFitWithDecimalWeightsIsAllowed() {
        Vehicle van = vehicle("VEH001", 0.3, DEL_LAT, DEL_LON);

        DispatchResult result = strategy.assign(List.of(
                order("ORD001", 0.1, Priority.HIGH, DEL_LAT, DEL_LON),
                order("ORD002", 0.2, Priority.HIGH, DEL_LAT, DEL_LON)), List.of(van));

        assertThat(result.unassignedOrders()).isEmpty();
    }

    @Test
    void sameInputAlwaysGivesSamePlan() {
        DispatchResult first = strategy.assign(SampleData.orders(), SampleData.vehicles());
        DispatchResult second = strategy.assign(SampleData.orders(), SampleData.vehicles());

        assertThat(planSignature(first)).isEqualTo(planSignature(second));
    }

    @Test
    void sampleDataRespectsAllConstraints() {
        DispatchResult result = strategy.assign(SampleData.orders(), SampleData.vehicles());

        for (VehicleRoute route : result.routes()) {
            assertThat(route.getTotalLoad()).isLessThanOrEqualTo(route.getVehicle().getCapacity());
        }
        long assignedHigh = result.routes().stream()
                .flatMap(r -> r.getAssignedOrders().stream())
                .filter(o -> o.getPriority() == Priority.HIGH)
                .count();
        assertThat(assignedHigh).isEqualTo(12);
        // 645 total weight vs 500 total capacity: only LOW orders are left over
        assertThat(result.unassignedOrders()).hasSize(8)
                .allSatisfy(u -> assertThat(u.order().getPriority()).isEqualTo(Priority.LOW));
        int assignedCount = result.routes().stream().mapToInt(r -> r.getAssignedOrders().size()).sum();
        assertThat(assignedCount + result.unassignedOrders().size()).isEqualTo(30);
    }

    @Test
    void handlesTenThousandOrdersAndHundredVehiclesQuickly() {
        Random random = new Random(42);
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            orders.add(order("ORD" + i, 1 + random.nextInt(50), Priority.values()[random.nextInt(3)],
                    12.8 + random.nextDouble(), 77.4 + random.nextDouble()));
        }
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            vehicles.add(vehicle("VEH" + i, 1000, 12.8 + random.nextDouble(), 77.4 + random.nextDouble()));
        }

        DispatchResult result = assertTimeout(Duration.ofSeconds(3),
                () -> strategy.assign(orders, vehicles));

        int assigned = result.routes().stream().mapToInt(r -> r.getAssignedOrders().size()).sum();
        assertThat(assigned + result.unassignedOrders().size()).isEqualTo(10_000);
        assertThat(result.routes()).allSatisfy(r ->
                assertThat(r.getTotalLoad()).isLessThanOrEqualTo(r.getVehicle().getCapacity()));
    }

    private static List<String> planSignature(DispatchResult result) {
        List<String> signature = new ArrayList<>();
        result.routes().forEach(r -> r.getAssignedOrders()
                .forEach(o -> signature.add(r.getVehicle().getVehicleId() + ":" + o.getOrderId())));
        result.unassignedOrders().forEach(u -> signature.add("NONE:" + u.order().getOrderId()));
        return signature;
    }

    private static VehicleRoute route(DispatchResult result, String vehicleId) {
        return result.routes().stream()
                .filter(r -> r.getVehicle().getVehicleId().equals(vehicleId))
                .findFirst()
                .orElseThrow();
    }

    private static Order order(String id, double weight, Priority priority, double lat, double lon) {
        return new Order(id, lat, lon, "Test address " + id, weight, priority);
    }

    private static Vehicle vehicle(String id, double capacity, double lat, double lon) {
        return new Vehicle(id, capacity, lat, lon, "Depot " + id);
    }
}
