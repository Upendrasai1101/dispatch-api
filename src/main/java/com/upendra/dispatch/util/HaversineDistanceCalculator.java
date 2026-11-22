package com.upendra.dispatch.util;

import org.springframework.stereotype.Component;

/**
 * Great-circle distance using the Haversine formula.
 *
 * a = sin^2(dLat/2) + cos(lat1) * cos(lat2) * sin^2(dLon/2)
 * d = 2 * R * asin(sqrt(a))
 */
@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    public static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(double fromLatitude, double fromLongitude,
                             double toLatitude, double toLongitude) {
        double lat1 = Math.toRadians(fromLatitude);
        double lat2 = Math.toRadians(toLatitude);
        double deltaLat = Math.toRadians(toLatitude - fromLatitude);
        double deltaLon = Math.toRadians(toLongitude - fromLongitude);

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);

        // Math.min guards against tiny floating-point overshoot above 1.0
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
