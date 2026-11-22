package com.upendra.dispatch.controller;

import com.upendra.dispatch.dto.DispatchPlanResponse;
import com.upendra.dispatch.dto.DispatchSummaryResponse;
import com.upendra.dispatch.dto.OrderListResponse;
import com.upendra.dispatch.dto.OrderResponse;
import com.upendra.dispatch.dto.OrdersRequest;
import com.upendra.dispatch.dto.StatusResponse;
import com.upendra.dispatch.dto.VehicleListResponse;
import com.upendra.dispatch.dto.VehicleResponse;
import com.upendra.dispatch.dto.VehiclesRequest;
import com.upendra.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Thin HTTP layer: validates input, delegates to the service, returns DTOs.
 */
@RestController
@RequestMapping(value = "/api/dispatch", produces = MediaType.APPLICATION_JSON_VALUE)
public class DispatchController {

    /** Swagger groups: the core dispatch endpoints, and the extra data-management helpers. */
    public static final String TAG_CORE = "1. Dispatch (core API)";
    public static final String TAG_EXTRAS = "2. Manage data (extras)";

    static final String ORDERS_ACCEPTED = "Delivery orders accepted.";
    static final String VEHICLES_ACCEPTED = "Vehicle details accepted.";
    static final String RESET_DONE = "All orders and vehicles cleared.";

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Tag(name = TAG_CORE)
    @Operation(summary = "Add or update delivery orders",
            description = "Upsert by orderId: an existing orderId is updated, a new one is created.")
    public StatusResponse addOrders(@Valid @RequestBody OrdersRequest request) {
        dispatchService.saveOrders(request.orders());
        return StatusResponse.success(ORDERS_ACCEPTED);
    }

    @PostMapping(value = "/vehicles", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Tag(name = TAG_CORE)
    @Operation(summary = "Add or update vehicles",
            description = "Upsert by vehicleId: an existing vehicleId is updated, a new one is created.")
    public StatusResponse addVehicles(@Valid @RequestBody VehiclesRequest request) {
        dispatchService.saveVehicles(request.vehicles());
        return StatusResponse.success(VEHICLES_ACCEPTED);
    }

    // ---------- orders: read and delete ----------

    @GetMapping("/orders")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "List all stored orders", description = "Sorted by orderId.")
    public OrderListResponse getOrders() {
        return dispatchService.getOrders();
    }

    @GetMapping("/orders/{orderId}")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "Get one order by id")
    public OrderResponse getOrder(@PathVariable("orderId") String orderId) {
        return dispatchService.getOrder(orderId);
    }

    @DeleteMapping("/orders/{orderId}")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "Delete one order by id")
    public StatusResponse deleteOrder(@PathVariable("orderId") String orderId) {
        dispatchService.deleteOrders(List.of(orderId));
        return StatusResponse.success("Order " + orderId + " deleted.");
    }

    @DeleteMapping("/orders")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "Delete several orders",
            description = "Example: DELETE /api/dispatch/orders?ids=ORD001,ORD002. "
                    + "All-or-nothing: if any id is unknown, nothing is deleted.")
    public StatusResponse deleteOrders(
            @Parameter(description = "Comma-separated order ids", example = "ORD001,ORD002")
            @RequestParam("ids") List<String> ids) {
        int deleted = dispatchService.deleteOrders(ids);
        return StatusResponse.success(deleted + (deleted == 1 ? " order" : " orders") + " deleted.");
    }

    // ---------- vehicles: read and delete ----------

    @GetMapping("/vehicles")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "List all stored vehicles", description = "Sorted by vehicleId.")
    public VehicleListResponse getVehicles() {
        return dispatchService.getVehicles();
    }

    @GetMapping("/vehicles/{vehicleId}")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "Get one vehicle by id")
    public VehicleResponse getVehicle(@PathVariable("vehicleId") String vehicleId) {
        return dispatchService.getVehicle(vehicleId);
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "Delete one vehicle by id")
    public StatusResponse deleteVehicle(@PathVariable("vehicleId") String vehicleId) {
        dispatchService.deleteVehicles(List.of(vehicleId));
        return StatusResponse.success("Vehicle " + vehicleId + " deleted.");
    }

    @DeleteMapping("/vehicles")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "Delete several vehicles",
            description = "Example: DELETE /api/dispatch/vehicles?ids=VEH001,VEH002. "
                    + "All-or-nothing: if any id is unknown, nothing is deleted.")
    public StatusResponse deleteVehicles(
            @Parameter(description = "Comma-separated vehicle ids", example = "VEH001,VEH002")
            @RequestParam("ids") List<String> ids) {
        int deleted = dispatchService.deleteVehicles(ids);
        return StatusResponse.success(deleted + (deleted == 1 ? " vehicle" : " vehicles") + " deleted.");
    }

    // ---------- planning ----------

    @GetMapping("/plan")
    @Tag(name = TAG_CORE)
    @Operation(summary = "Get the dispatch plan",
            description = "Assigns HIGH priority first, never exceeds capacity, picks the nearest "
                    + "vehicle (Haversine). Orders that cannot fit are listed in unassignedOrders.")
    public DispatchPlanResponse getPlan() {
        return dispatchService.getDispatchPlan();
    }

    @GetMapping("/summary")
    @Tag(name = TAG_CORE)
    @Operation(summary = "Get a demand vs capacity summary",
            description = "Order count and weight per priority plus fleet capacity (SQL CTE query).")
    public DispatchSummaryResponse getSummary() {
        return dispatchService.getSummary();
    }

    @DeleteMapping("/reset")
    @Tag(name = TAG_EXTRAS)
    @Operation(summary = "Clear all orders and vehicles (testing helper)")
    public StatusResponse reset() {
        dispatchService.reset();
        return StatusResponse.success(RESET_DONE);
    }
}
