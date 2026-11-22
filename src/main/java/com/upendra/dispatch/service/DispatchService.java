package com.upendra.dispatch.service;

import com.upendra.dispatch.domain.Order;
import com.upendra.dispatch.domain.Vehicle;
import com.upendra.dispatch.dto.DispatchPlanResponse;
import com.upendra.dispatch.dto.DispatchSummaryResponse;
import com.upendra.dispatch.dto.OrderListResponse;
import com.upendra.dispatch.dto.OrderRequest;
import com.upendra.dispatch.dto.OrderResponse;
import com.upendra.dispatch.dto.PrioritySummaryResponse;
import com.upendra.dispatch.dto.VehicleListResponse;
import com.upendra.dispatch.dto.VehicleRequest;
import com.upendra.dispatch.dto.VehicleResponse;
import com.upendra.dispatch.exception.BadRequestException;
import com.upendra.dispatch.exception.DispatchDataMissingException;
import com.upendra.dispatch.exception.DuplicateIdException;
import com.upendra.dispatch.exception.ResourceNotFoundException;
import com.upendra.dispatch.mapper.DispatchMapper;
import com.upendra.dispatch.repository.OrderRepository;
import com.upendra.dispatch.repository.VehicleRepository;
import com.upendra.dispatch.strategy.DispatchResult;
import com.upendra.dispatch.strategy.DispatchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the use cases: store orders/vehicles (upsert),
 * build the dispatch plan via the configured strategy, and report a summary.
 */
@Service
public class DispatchService {

    static final String NO_ORDERS_MESSAGE =
            "No delivery orders found. Add orders with POST /api/dispatch/orders first.";
    static final String NO_VEHICLES_MESSAGE =
            "No vehicles found. Add vehicles with POST /api/dispatch/vehicles first.";

    /** Keeps IN (...) lists well below database bind-parameter limits for huge requests. */
    static final int ID_LOOKUP_CHUNK_SIZE = 1000;

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchStrategy dispatchStrategy;

    public DispatchService(OrderRepository orderRepository,
                           VehicleRepository vehicleRepository,
                           DispatchStrategy dispatchStrategy) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.dispatchStrategy = dispatchStrategy;
    }

    /**
     * Upsert: existing orderIds are updated in place (JPA dirty checking),
     * new ones are inserted in JDBC batches. Existing rows are looked up with
     * one SELECT per 1000 ids instead of one SELECT per row.
     */
    @Transactional
    public int saveOrders(List<OrderRequest> requests) {
        List<String> ids = requests.stream().map(OrderRequest::orderId).toList();
        ensureUnique("orderId", ids);

        Map<String, Order> existing = findAllInChunks(ids, orderRepository::findAllById).stream()
                .collect(Collectors.toMap(Order::getOrderId, Function.identity()));

        List<Order> newOrders = new ArrayList<>();
        for (OrderRequest request : requests) {
            Order current = existing.get(request.orderId());
            if (current != null) {
                DispatchMapper.updateOrder(current, request);
            } else {
                newOrders.add(DispatchMapper.toOrder(request));
            }
        }
        orderRepository.saveAll(newOrders);

        log.info("Orders saved: {} new, {} updated", newOrders.size(), existing.size());
        return requests.size();
    }

    @Transactional
    public int saveVehicles(List<VehicleRequest> requests) {
        List<String> ids = requests.stream().map(VehicleRequest::vehicleId).toList();
        ensureUnique("vehicleId", ids);

        Map<String, Vehicle> existing = findAllInChunks(ids, vehicleRepository::findAllById).stream()
                .collect(Collectors.toMap(Vehicle::getVehicleId, Function.identity()));

        List<Vehicle> newVehicles = new ArrayList<>();
        for (VehicleRequest request : requests) {
            Vehicle current = existing.get(request.vehicleId());
            if (current != null) {
                DispatchMapper.updateVehicle(current, request);
            } else {
                newVehicles.add(DispatchMapper.toVehicle(request));
            }
        }
        vehicleRepository.saveAll(newVehicles);

        log.info("Vehicles saved: {} new, {} updated", newVehicles.size(), existing.size());
        return requests.size();
    }

    // ---------- read ----------

    @Transactional(readOnly = true)
    public OrderListResponse getOrders() {
        List<OrderResponse> orders = orderRepository.findAll(Sort.by("orderId")).stream()
                .map(DispatchMapper::toOrderResponse)
                .toList();
        return new OrderListResponse(orders.size(), orders);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(DispatchMapper::toOrderResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
    }

    @Transactional(readOnly = true)
    public VehicleListResponse getVehicles() {
        List<VehicleResponse> vehicles = vehicleRepository.findAll(Sort.by("vehicleId")).stream()
                .map(DispatchMapper::toVehicleResponse)
                .toList();
        return new VehicleListResponse(vehicles.size(), vehicles);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(String vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .map(DispatchMapper::toVehicleResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle " + vehicleId + " not found"));
    }

    // ---------- delete ----------

    /**
     * Deletes the given orders. All-or-nothing: if any id does not exist,
     * nothing is deleted and a 404 lists the missing ids.
     */
    @Transactional
    public int deleteOrders(List<String> orderIds) {
        return deleteByIds("Order", orderIds, ids -> findAllInChunks(ids, orderRepository::findAllById),
                Order::getOrderId, orderRepository::deleteAllInBatch);
    }

    @Transactional
    public int deleteVehicles(List<String> vehicleIds) {
        return deleteByIds("Vehicle", vehicleIds, ids -> findAllInChunks(ids, vehicleRepository::findAllById),
                Vehicle::getVehicleId, vehicleRepository::deleteAllInBatch);
    }

    /** The plan is computed fresh from the current database state on every call. */
    @Transactional(readOnly = true)
    public DispatchPlanResponse getDispatchPlan() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            throw new DispatchDataMissingException(NO_ORDERS_MESSAGE);
        }
        List<Vehicle> vehicles = vehicleRepository.findAll();
        if (vehicles.isEmpty()) {
            throw new DispatchDataMissingException(NO_VEHICLES_MESSAGE);
        }

        long start = System.nanoTime();
        DispatchResult result = dispatchStrategy.assign(orders, vehicles);
        long millis = (System.nanoTime() - start) / 1_000_000;

        log.info("Dispatch plan: {} orders, {} vehicles, {} unassigned, computed in {} ms",
                orders.size(), vehicles.size(), result.unassignedOrders().size(), millis);
        return DispatchMapper.toPlanResponse(result);
    }

    @Transactional(readOnly = true)
    public DispatchSummaryResponse getSummary() {
        List<Object[]> rows = orderRepository.fetchDispatchSummaryRows();

        List<PrioritySummaryResponse> byPriority = rows.stream()
                .map(row -> new PrioritySummaryResponse(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue(),
                        DispatchMapper.round2(((Number) row[2]).doubleValue())))
                .toList();

        long totalOrders = byPriority.stream().mapToLong(PrioritySummaryResponse::orderCount).sum();
        double totalWeight = byPriority.stream().mapToDouble(PrioritySummaryResponse::totalWeight).sum();
        long totalVehicles = rows.isEmpty() ? 0 : ((Number) rows.get(0)[3]).longValue();
        double totalCapacity = rows.isEmpty() ? 0 : ((Number) rows.get(0)[4]).doubleValue();
        double demandPercent = totalCapacity == 0 ? 0 : totalWeight * 100.0 / totalCapacity;

        return new DispatchSummaryResponse(totalOrders, DispatchMapper.round2(totalWeight),
                totalVehicles, DispatchMapper.round2(totalCapacity),
                DispatchMapper.round2(demandPercent), byPriority);
    }

    /** Testing helper: removes all orders and vehicles. */
    @Transactional
    public void reset() {
        orderRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
        log.info("All orders and vehicles cleared");
    }

    private static <T> int deleteByIds(String label, List<String> rawIds,
                                       Function<List<String>, List<T>> finder,
                                       Function<T, String> idOf,
                                       Consumer<List<T>> deleter) {
        List<String> ids = rawIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw new BadRequestException("Provide at least one " + label.toLowerCase() + " id to delete");
        }

        List<T> found = finder.apply(ids);
        Set<String> foundIds = found.stream().map(idOf).collect(Collectors.toSet());
        List<String> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException(label + (missing.size() == 1 ? " " : "s ")
                    + String.join(", ", missing) + " not found. Nothing was deleted.");
        }

        deleter.accept(found);
        log.info("{} deleted: {}", label, ids);
        return found.size();
    }

    private static <T> List<T> findAllInChunks(List<String> ids, Function<List<String>, List<T>> finder) {
        List<T> found = new ArrayList<>();
        for (int from = 0; from < ids.size(); from += ID_LOOKUP_CHUNK_SIZE) {
            int to = Math.min(from + ID_LOOKUP_CHUNK_SIZE, ids.size());
            found.addAll(finder.apply(ids.subList(from, to)));
        }
        return found;
    }

    private static void ensureUnique(String field, List<String> ids) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                duplicates.add(id);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new DuplicateIdException(field, List.copyOf(duplicates));
        }
    }
}
