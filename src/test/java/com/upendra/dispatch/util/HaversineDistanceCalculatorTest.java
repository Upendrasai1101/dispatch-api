package com.upendra.dispatch.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final DistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void samePointIsZeroKm() {
        assertThat(calculator.distanceKm(28.6139, 77.2090, 28.6139, 77.2090)).isEqualTo(0.0);
    }

    @Test
    void connaughtPlaceToKarolBaghSampleCoordinates() {
        double km = calculator.distanceKm(28.6139, 77.2090, 28.7041, 77.1025);
        assertThat(km).isCloseTo(14.44, within(0.01));
    }

    @Test
    void delhiToMumbaiIsAbout1148Km() {
        double km = calculator.distanceKm(28.6139, 77.2090, 19.0760, 72.8777);
        assertThat(km).isCloseTo(1148.1, within(1.0));
    }

    @Test
    void distanceIsSymmetric() {
        double there = calculator.distanceKm(12.9716, 77.5946, 13.0827, 80.2707);
        double back = calculator.distanceKm(13.0827, 80.2707, 12.9716, 77.5946);
        assertThat(there).isCloseTo(back, within(1e-9));
    }

    @Test
    void oppositeSidesOfEarthIsHalfTheCircumference() {
        double km = calculator.distanceKm(0, 0, 0, 180);
        assertThat(km).isCloseTo(Math.PI * HaversineDistanceCalculator.EARTH_RADIUS_KM, within(0.001));
    }
}
