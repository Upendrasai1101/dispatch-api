package com.upendra.dispatch.integration;

import com.upendra.dispatch.dto.DispatchPlanResponse;
import com.upendra.dispatch.dto.OrderRequest;
import com.upendra.dispatch.dto.VehicleRequest;
import com.upendra.dispatch.dto.VehiclePlanResponse;
import com.upendra.dispatch.repository.OrderRepository;
import com.upendra.dispatch.service.DispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * Bulk path through service + database: 10,000 orders, 100 vehicles.
 */
@SpringBootTest
class LargeDatasetTest {

    private static final String[] PRIORITIES = {"HIGH", "MEDIUM", "LOW"};

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void clean() {
        dispatchService.reset();
    }

    @Test
    void tenThousandOrdersAreStoredAndPlannedQuickly() {
        Random random = new Random(7);
        List<OrderRequest> orders = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            orders.add(new OrderRequest("ORD" + i, 12.8 + random.nextDouble(), 77.4 + random.nextDouble(),
                    "Bangalore drop " + i, 1.0 + random.nextInt(40), PRIORITIES[random.nextInt(3)]));
        }
        List<VehicleRequest> vehicles = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            vehicles.add(new VehicleRequest("VEH" + i, 2000.0, 12.8 + random.nextDouble(),
                    77.4 + random.nextDouble(), "Depot " + i));
        }

        DispatchPlanResponse plan = assertTimeout(Duration.ofSeconds(20), () -> {
            dispatchService.saveVehicles(vehicles);
            dispatchService.saveOrders(orders);
            return dispatchService.getDispatchPlan();
        });

        assertThat(orderRepository.count()).isEqualTo(10_000);
        int assigned = plan.dispatchPlan().stream().mapToInt(p -> p.assignedOrders().size()).sum();
        assertThat(assigned + plan.unassignedOrders().size()).isEqualTo(10_000);
        for (VehiclePlanResponse vehiclePlan : plan.dispatchPlan()) {
            assertThat(vehiclePlan.totalLoad()).isLessThanOrEqualTo(2000.0);
        }
    }

    @Test
    void reSubmittingTheSameBatchUpdatesRows() {
        List<OrderRequest> batch = new ArrayList<>();
        for (int i = 0; i < 1_500; i++) {
            batch.add(new OrderRequest("ORD" + i, 12.9, 77.6, "Addr", 5.0, "LOW"));
        }
        dispatchService.saveOrders(batch);
        dispatchService.saveOrders(batch);

        assertThat(orderRepository.count()).isEqualTo(1_500);
    }
}
