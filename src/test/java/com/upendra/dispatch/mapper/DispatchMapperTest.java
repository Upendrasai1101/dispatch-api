package com.upendra.dispatch.mapper;

import com.upendra.dispatch.domain.Priority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchMapperTest {

    @Test
    void distanceIsFormattedWithTwoDecimalsAndKm() {
        assertThat(DispatchMapper.formatDistance(5)).isEqualTo("5.00 km");
        assertThat(DispatchMapper.formatDistance(14.442261)).isEqualTo("14.44 km");
        assertThat(DispatchMapper.formatDistance(0)).isEqualTo("0.00 km");
    }

    @Test
    void priorityIsCaseInsensitive() {
        assertThat(DispatchMapper.toPriority("high")).isEqualTo(Priority.HIGH);
        assertThat(DispatchMapper.toPriority("Medium")).isEqualTo(Priority.MEDIUM);
        assertThat(DispatchMapper.toPriority("LOW")).isEqualTo(Priority.LOW);
    }

    @Test
    void roundsToTwoDecimals() {
        assertThat(DispatchMapper.round2(0.1 + 0.2)).isEqualTo(0.3);
        assertThat(DispatchMapper.round2(129.0)).isEqualTo(129.0);
    }
}
