package com.upendra.dispatch.util;

/**
 * Calculates the distance between two coordinates in kilometres.
 * An interface so the dispatch strategy does not depend on one formula
 * (easy to swap for a road-distance API later, and easy to mock in tests).
 */
public interface DistanceCalculator {

    double distanceKm(double fromLatitude, double fromLongitude,
                      double toLatitude, double toLongitude);
}
