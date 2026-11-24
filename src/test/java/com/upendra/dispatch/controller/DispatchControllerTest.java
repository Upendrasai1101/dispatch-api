package com.upendra.dispatch.controller;

import com.upendra.dispatch.repository.OrderRepository;
import com.upendra.dispatch.service.DispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests: real controller, service, strategy and H2 database.
 */
@SpringBootTest
class DispatchControllerTest {

    private static final String ONE_VEHICLE = """
            {"vehicles":[{"vehicleId":"VEH001","capacity":100,"currentLatitude":12.9716,
            "currentLongitude":77.6413,"currentAddress":"Indiranagar, Bangalore, Karnataka, India"}]}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private OrderRepository orderRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        dispatchService.reset();
    }

    // ---------- happy paths ----------

    @Test
    void postOrdersReturnsSpecMessage() throws Exception {
        postJson("/api/dispatch/orders", order("ORD001", 12.9716, 77.5946, 10, "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delivery orders accepted."))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void postVehiclesReturnsSpecMessage() throws Exception {
        postJson("/api/dispatch/vehicles", ONE_VEHICLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle details accepted."))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void planForSampleDataRespectsPriorityAndCapacity() throws Exception {
        postJson("/api/dispatch/vehicles", resource("/samples/sample-vehicles.json")).andExpect(status().isOk());
        postJson("/api/dispatch/orders", resource("/samples/sample-orders.json")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan", hasSize(5)))
                .andExpect(jsonPath("$.dispatchPlan[0].vehicleId").value("VEH001"))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(100.0))
                .andExpect(jsonPath("$.dispatchPlan[0].totalDistance").value("14.44 km"))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].address").value("Karol Bagh, Delhi, India"))
                .andExpect(jsonPath("$.dispatchPlan[1].totalLoad").value(80.0))
                .andExpect(jsonPath("$.dispatchPlan[2].totalLoad").value(115.0))
                .andExpect(jsonPath("$.dispatchPlan[3].totalLoad").value(90.0))
                .andExpect(jsonPath("$.dispatchPlan[4].totalLoad").value(105.0))
                .andExpect(jsonPath("$.unassignedOrders", hasSize(8)))
                .andExpect(jsonPath("$.unassignedOrders[*].priority", everyItem(is("LOW"))))
                .andExpect(jsonPath("$.unassignedOrders[0].reason").isNotEmpty());
    }

    @Test
    void summaryUsesCteQuery() throws Exception {
        postJson("/api/dispatch/vehicles", resource("/samples/sample-vehicles.json")).andExpect(status().isOk());
        postJson("/api/dispatch/orders", resource("/samples/sample-orders.json")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(30))
                .andExpect(jsonPath("$.totalOrderWeight").value(645.0))
                .andExpect(jsonPath("$.totalVehicles").value(5))
                .andExpect(jsonPath("$.totalFleetCapacity").value(500.0))
                .andExpect(jsonPath("$.demandToCapacityPercent").value(129.0))
                .andExpect(jsonPath("$.ordersByPriority[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.ordersByPriority[0].orderCount").value(12))
                .andExpect(jsonPath("$.ordersByPriority[0].totalWeight").value(275.0))
                .andExpect(jsonPath("$.ordersByPriority[2].priority").value("LOW"));
    }

    @Test
    void summaryOnEmptyDatabaseReturnsZeros() throws Exception {
        mockMvc.perform(get("/api/dispatch/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(0))
                .andExpect(jsonPath("$.ordersByPriority", hasSize(3)))
                .andExpect(jsonPath("$.demandToCapacityPercent").value(0.0));
    }

    @Test
    void postingSameOrderIdAgainUpdatesInsteadOfDuplicating() throws Exception {
        postJson("/api/dispatch/orders", order("ORD001", 12.9716, 77.5946, 10, "HIGH")).andExpect(status().isOk());
        postJson("/api/dispatch/orders", order("ORD001", 12.9716, 77.5946, 50, "LOW")).andExpect(status().isOk());

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderRepository.findById("ORD001")).hasValueSatisfying(o -> {
            assertThat(o.getPackageWeight()).isEqualTo(50.0);
            assertThat(o.getPriority().name()).isEqualTo("LOW");
        });
    }

    @Test
    void resetClearsEverything() throws Exception {
        postJson("/api/dispatch/orders", order("ORD001", 12.9716, 77.5946, 10, "HIGH")).andExpect(status().isOk());

        mockMvc.perform(delete("/api/dispatch/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void rootRedirectsToSwagger() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/swagger-ui.html"));
    }

    @Test
    void openApiDocsAreGenerated() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/dispatch/plan")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(DispatchController.TAG_CORE)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(DispatchController.TAG_EXTRAS)));
    }

    // ---------- read and delete by id ----------

    @Test
    void listOrdersReturnsAllSortedById() throws Exception {
        postJson("/api/dispatch/orders", twoOrders()).andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.orders[0].orderId").value("ORD001"))
                .andExpect(jsonPath("$.orders[1].orderId").value("ORD002"))
                .andExpect(jsonPath("$.orders[1].priority").value("LOW"));
    }

    @Test
    void getSingleOrderAndUnknownOrder() throws Exception {
        postJson("/api/dispatch/orders", twoOrders()).andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/orders/ORD002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD002"))
                .andExpect(jsonPath("$.packageWeight").value(20.0));
        mockMvc.perform(get("/api/dispatch/orders/ORD999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order ORD999 not found"));
    }

    @Test
    void deleteSingleOrder() throws Exception {
        postJson("/api/dispatch/orders", twoOrders()).andExpect(status().isOk());

        mockMvc.perform(delete("/api/dispatch/orders/ORD001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order ORD001 deleted."));
        assertThat(orderRepository.findAll()).extracting(o -> o.getOrderId()).containsExactly("ORD002");
    }

    @Test
    void deleteUnknownOrderReturns404() throws Exception {
        mockMvc.perform(delete("/api/dispatch/orders/ORD999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order ORD999 not found. Nothing was deleted."));
    }

    @Test
    void bulkDeleteOrders() throws Exception {
        postJson("/api/dispatch/orders", twoOrders()).andExpect(status().isOk());

        mockMvc.perform(delete("/api/dispatch/orders").param("ids", "ORD001,ORD002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("2 orders deleted."));
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void bulkDeleteWithAnUnknownIdDeletesNothing() throws Exception {
        postJson("/api/dispatch/orders", twoOrders()).andExpect(status().isOk());

        mockMvc.perform(delete("/api/dispatch/orders").param("ids", "ORD001,ORD999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order ORD999 not found. Nothing was deleted."));
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void bulkDeleteWithoutIdsIsRejected() throws Exception {
        mockMvc.perform(delete("/api/dispatch/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required query parameter 'ids' is missing"));
    }

    @Test
    void vehiclesCanBeListedFetchedAndDeleted() throws Exception {
        postJson("/api/dispatch/vehicles", resource("/samples/sample-vehicles.json")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.vehicles[0].vehicleId").value("VEH001"));
        mockMvc.perform(get("/api/dispatch/vehicles/VEH003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(120.0));
        mockMvc.perform(delete("/api/dispatch/vehicles/VEH005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle VEH005 deleted."));
        mockMvc.perform(delete("/api/dispatch/vehicles").param("ids", "VEH001,VEH002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("2 vehicles deleted."));
        mockMvc.perform(get("/api/dispatch/vehicles"))
                .andExpect(jsonPath("$.count").value(2));
        mockMvc.perform(get("/api/dispatch/vehicles/VEH001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void planReflectsDeletedOrders() throws Exception {
        postJson("/api/dispatch/vehicles", ONE_VEHICLE).andExpect(status().isOk());
        postJson("/api/dispatch/orders", twoOrders()).andExpect(status().isOk());
        mockMvc.perform(delete("/api/dispatch/orders/ORD001")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders", hasSize(1)))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].orderId").value("ORD002"));
    }

    // ---------- validation and errors ----------

    @Test
    void invalidLatitudeIsRejectedWithFieldError() throws Exception {
        postJson("/api/dispatch/orders", order("ORD001", 95.0, 77.5946, 10, "HIGH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("orders[0].latitude"))
                .andExpect(jsonPath("$.errors[0].message").value("latitude must be between -90 and 90"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void negativeWeightIsRejected() throws Exception {
        postJson("/api/dispatch/orders", order("ORD001", 12.9716, 77.5946, -5, "HIGH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("orders[0].packageWeight"))
                .andExpect(jsonPath("$.errors[0].message").value("packageWeight must be greater than 0"));
    }

    @Test
    void unknownPriorityIsRejected() throws Exception {
        postJson("/api/dispatch/orders", order("ORD001", 12.9716, 77.5946, 10, "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("orders[0].priority"))
                .andExpect(jsonPath("$.errors[0].message").value("priority must be one of HIGH, MEDIUM, LOW"));
    }

    @Test
    void missingFieldsAreAllReported() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\":[{\"orderId\":\"ORD001\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(5)));
    }

    @Test
    void emptyOrderListIsRejected() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("orders"))
                .andExpect(jsonPath("$.errors[0].message").value("orders must contain at least one order"));
    }

    @Test
    void duplicateIdsInOneRequestAreRejected() throws Exception {
        String body = """
                {"orders":[
                  {"orderId":"ORD001","latitude":12.97,"longitude":77.59,"address":"A","packageWeight":10,"priority":"HIGH"},
                  {"orderId":"ORD001","latitude":12.98,"longitude":77.60,"address":"B","packageWeight":20,"priority":"LOW"}
                ]}""";
        postJson("/api/dispatch/orders", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate orderId values in request: [ORD001]"))
                .andExpect(jsonPath("$.errors[0].field").value("orderId"));
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void invalidVehicleCapacityIsRejected() throws Exception {
        String body = """
                {"vehicles":[{"vehicleId":"VEH001","capacity":0,"currentLatitude":12.97,
                "currentLongitude":77.64,"currentAddress":"Indiranagar"}]}""";
        postJson("/api/dispatch/vehicles", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("vehicles[0].capacity"))
                .andExpect(jsonPath("$.errors[0].message").value("capacity must be greater than 0"));
    }

    @Test
    void malformedJsonGivesClearMessage() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": [ {\"orderId\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Malformed JSON request")));
    }

    @Test
    void textInNumericFieldGivesClearMessage() throws Exception {
        String body = """
                {"orders":[{"orderId":"ORD001","latitude":"north","longitude":77.59,
                "address":"A","packageWeight":10,"priority":"HIGH"}]}""";
        postJson("/api/dispatch/orders", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Malformed JSON request")));
    }

    @Test
    void planWithoutOrdersReturns404WithMessage() throws Exception {
        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", startsWith("No delivery orders found")));
    }

    @Test
    void planWithoutVehiclesReturns404WithMessage() throws Exception {
        postJson("/api/dispatch/orders", order("ORD001", 12.9716, 77.5946, 10, "HIGH")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", startsWith("No vehicles found")));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(put("/api/dispatch/plan"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void wrongContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Content-Type must be application/json"));
    }

    // ---------- helpers ----------

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String order(String id, double lat, double lon, double weight, String priority) {
        return """
                {"orders":[{"orderId":"%s","latitude":%s,"longitude":%s,
                "address":"MG Road, Bangalore, Karnataka, India","packageWeight":%s,"priority":"%s"}]}
                """.formatted(id, lat, lon, weight, priority);
    }

    private static String twoOrders() {
        return """
                {"orders":[
                  {"orderId":"ORD002","latitude":12.98,"longitude":77.60,"address":"B","packageWeight":20,"priority":"LOW"},
                  {"orderId":"ORD001","latitude":12.97,"longitude":77.59,"address":"A","packageWeight":10,"priority":"HIGH"}
                ]}""";
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = DispatchControllerTest.class.getResourceAsStream(path)) {
            assertThat(in).as("test resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
