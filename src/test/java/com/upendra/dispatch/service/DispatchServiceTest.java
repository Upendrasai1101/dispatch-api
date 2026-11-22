package com.upendra.dispatch.service;

import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Priority;
import com.upendra.dispatch.domain.Vehicle;
import com.upendra.dispatch.dto.DispatchPlanResponse;
import com.upendra.dispatch.dto.DispatchSummaryResponse;
import com.upendra.dispatch.dto.OrderRequest;
import com.upendra.dispatch.dto.VehicleRequest;
import com.upendra.dispatch.exception.DispatchDataMissingException;
import com.upendra.dispatch.exception.BadRequestException;
import com.upendra.dispatch.exception.DuplicateIdException;
import com.upendra.dispatch.exception.ResourceNotFoundException;
import com.upendra.dispatch.repository.OrderRepository;
import com.upendra.dispatch.repository.VehicleRepository;
import com.upendra.dispatch.strategy.DispatchResult;
import com.upendra.dispatch.strategy.DispatchStrategy;
import com.upendra.dispatch.strategy.VehicleRoute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DispatchStrategy dispatchStrategy;

    @InjectMocks
    private DispatchService dispatchService;

    @Captor
    private ArgumentCaptor<List<Order>> ordersCaptor;

    @Captor
    private ArgumentCaptor<List<Vehicle>> vehiclesCaptor;

    @Test
    void saveOrdersInsertsNewAndUpdatesExistingInPlace() {
        Order existing = new Order("ORD001", 1.0, 1.0, "Old address", 5, Priority.LOW);
        when(orderRepository.findAllById(List.of("ORD001", "ORD002"))).thenReturn(List.of(existing));

        int saved = dispatchService.saveOrders(List.of(
                orderRequest("ORD001", 50, "HIGH"),
                orderRequest("ORD002", 10, "low")));

        assertThat(saved).isEqualTo(2);
        verify(orderRepository).saveAll(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue()).extracting(Order::getOrderId).containsExactly("ORD002");
        assertThat(ordersCaptor.getValue().get(0).getPriority()).isEqualTo(Priority.LOW);
        // existing row updated through JPA dirty checking, not re-inserted
        assertThat(existing.getPackageWeight()).isEqualTo(50.0);
        assertThat(existing.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(existing.getAddress()).isEqualTo("Address ORD001");
    }

    @Test
    void saveOrdersRejectsDuplicateIdsInsideOneRequest() {
        List<OrderRequest> requests = List.of(
                orderRequest("ORD001", 10, "HIGH"),
                orderRequest("ORD001", 20, "LOW"));

        assertThatThrownBy(() -> dispatchService.saveOrders(requests))
                .isInstanceOf(DuplicateIdException.class)
                .hasMessageContaining("ORD001");
        verifyNoInteractions(orderRepository);
    }

    @Test
    void saveOrdersLooksUpExistingIdsInChunks() {
        List<OrderRequest> requests = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            requests.add(orderRequest("ORD" + i, 1, "LOW"));
        }
        when(orderRepository.findAllById(any())).thenReturn(List.of());

        dispatchService.saveOrders(requests);

        verify(orderRepository, times(3)).findAllById(any());
        verify(orderRepository).saveAll(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue()).hasSize(2500);
    }

    @Test
    void saveVehiclesInsertsNewAndUpdatesExisting() {
        Vehicle existing = new Vehicle("VEH001", 50, 1.0, 1.0, "Old depot");
        when(vehicleRepository.findAllById(List.of("VEH001", "VEH002"))).thenReturn(List.of(existing));

        dispatchService.saveVehicles(List.of(vehicleRequest("VEH001", 120), vehicleRequest("VEH002", 80)));

        verify(vehicleRepository).saveAll(vehiclesCaptor.capture());
        assertThat(vehiclesCaptor.getValue()).extracting(Vehicle::getVehicleId).containsExactly("VEH002");
        assertThat(existing.getCapacity()).isEqualTo(120.0);
    }

    @Test
    void saveVehiclesRejectsDuplicateIds() {
        List<VehicleRequest> requests = List.of(vehicleRequest("VEH001", 10), vehicleRequest("VEH001", 20));

        assertThatThrownBy(() -> dispatchService.saveVehicles(requests))
                .isInstanceOf(DuplicateIdException.class)
                .hasMessageContaining("vehicleId");
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void planFailsClearlyWhenThereAreNoOrders() {
        when(orderRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> dispatchService.getDispatchPlan())
                .isInstanceOf(DispatchDataMissingException.class)
                .hasMessage(DispatchService.NO_ORDERS_MESSAGE);
        verify(dispatchStrategy, never()).assign(any(), any());
    }

    @Test
    void planFailsClearlyWhenThereAreNoVehicles() {
        when(orderRepository.findAll()).thenReturn(List.of(
                new Order("ORD001", 1.0, 1.0, "A", 5, Priority.HIGH)));
        when(vehicleRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> dispatchService.getDispatchPlan())
                .isInstanceOf(DispatchDataMissingException.class)
                .hasMessage(DispatchService.NO_VEHICLES_MESSAGE);
    }

    @Test
    void planDelegatesToStrategyAndMapsResult() {
        List<Order> orders = List.of(new Order("ORD001", 1.0, 1.0, "A", 5, Priority.HIGH));
        Vehicle vehicle = new Vehicle("VEH001", 50, 1.0, 1.0, "Depot");
        when(orderRepository.findAll()).thenReturn(orders);
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));
        when(dispatchStrategy.assign(orders, List.of(vehicle)))
                .thenReturn(new DispatchResult(List.of(new VehicleRoute(vehicle)), List.of()));

        DispatchPlanResponse plan = dispatchService.getDispatchPlan();

        assertThat(plan.dispatchPlan()).singleElement().satisfies(p -> {
            assertThat(p.vehicleId()).isEqualTo("VEH001");
            assertThat(p.totalLoad()).isEqualTo(0.0);
            assertThat(p.totalDistance()).isEqualTo("0.00 km");
        });
        assertThat(plan.unassignedOrders()).isEmpty();
    }

    @Test
    void summaryAddsUpCteRows() {
        when(orderRepository.fetchDispatchSummaryRows()).thenReturn(List.of(
                new Object[]{"HIGH", 2L, 40.0, 2L, 200.0},
                new Object[]{"MEDIUM", 1L, 10.0, 2L, 200.0},
                new Object[]{"LOW", 0L, 0, 2L, 200.0}));

        DispatchSummaryResponse summary = dispatchService.getSummary();

        assertThat(summary.totalOrders()).isEqualTo(3);
        assertThat(summary.totalOrderWeight()).isEqualTo(50.0);
        assertThat(summary.totalVehicles()).isEqualTo(2);
        assertThat(summary.totalFleetCapacity()).isEqualTo(200.0);
        assertThat(summary.demandToCapacityPercent()).isEqualTo(25.0);
        assertThat(summary.ordersByPriority()).extracting(p -> p.priority())
                .containsExactly("HIGH", "MEDIUM", "LOW");
    }

    @Test
    void deleteOrdersRemovesAllWhenEveryIdExists() {
        Order a = new Order("ORD001", 1.0, 1.0, "A", 5, Priority.HIGH);
        Order b = new Order("ORD002", 1.0, 1.0, "B", 5, Priority.LOW);
        when(orderRepository.findAllById(List.of("ORD001", "ORD002"))).thenReturn(List.of(a, b));

        int deleted = dispatchService.deleteOrders(List.of("ORD001", " ORD002 ", "ORD001"));

        assertThat(deleted).isEqualTo(2);
        verify(orderRepository).deleteAllInBatch(List.of(a, b));
    }

    @Test
    void deleteOrdersIsAllOrNothing() {
        Order a = new Order("ORD001", 1.0, 1.0, "A", 5, Priority.HIGH);
        when(orderRepository.findAllById(List.of("ORD001", "ORD999"))).thenReturn(List.of(a));

        assertThatThrownBy(() -> dispatchService.deleteOrders(List.of("ORD001", "ORD999")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order ORD999 not found. Nothing was deleted.");
        verify(orderRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void deleteWithOnlyBlankIdsIsRejected() {
        assertThatThrownBy(() -> dispatchService.deleteVehicles(List.of(" ", "")))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void getUnknownVehicleThrowsNotFound() {
        when(vehicleRepository.findById("VEH9")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> dispatchService.getVehicle("VEH9"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vehicle VEH9 not found");
    }

    @Test
    void resetDeletesOrdersAndVehicles() {
        dispatchService.reset();

        verify(orderRepository).deleteAllInBatch();
        verify(vehicleRepository).deleteAllInBatch();
    }

    private static OrderRequest orderRequest(String id, double weight, String priority) {
        return new OrderRequest(id, 28.6, 77.2, "Address " + id, weight, priority);
    }

    private static VehicleRequest vehicleRequest(String id, double capacity) {
        return new VehicleRequest(id, capacity, 28.7, 77.1, "Depot " + id);
    }
}
