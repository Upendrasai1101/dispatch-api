package com.upendra.dispatch.dto;

public record VehicleResponse(
        String vehicleId,
        double capacity,
        double currentLatitude,
        double currentLongitude,
        String currentAddress
) {
}
