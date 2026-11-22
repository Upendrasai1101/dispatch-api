package com.upendra.dispatch.dto;

import java.util.List;

public record VehicleListResponse(int count, List<VehicleResponse> vehicles) {
}
