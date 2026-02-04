package com.example.firestock.domain.apparatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApparatusStatus} enum demonstrating apparatus lifecycle states.
 */
@DisplayName("ApparatusStatus")
class ApparatusStatusTest {

    @Test
    void has_all_expected_statuses() {
        assertThat(ApparatusStatus.values())
                .containsExactlyInAnyOrder(
                        ApparatusStatus.IN_SERVICE,
                        ApparatusStatus.OUT_OF_SERVICE,
                        ApparatusStatus.RESERVE,
                        ApparatusStatus.DECOMMISSIONED
                );
    }

    @Test
    void can_be_converted_from_string() {
        assertThat(ApparatusStatus.valueOf("IN_SERVICE")).isEqualTo(ApparatusStatus.IN_SERVICE);
        assertThat(ApparatusStatus.valueOf("OUT_OF_SERVICE")).isEqualTo(ApparatusStatus.OUT_OF_SERVICE);
        assertThat(ApparatusStatus.valueOf("RESERVE")).isEqualTo(ApparatusStatus.RESERVE);
        assertThat(ApparatusStatus.valueOf("DECOMMISSIONED")).isEqualTo(ApparatusStatus.DECOMMISSIONED);
    }

    @Test
    void has_readable_name() {
        assertThat(ApparatusStatus.IN_SERVICE.name()).isEqualTo("IN_SERVICE");
        assertThat(ApparatusStatus.DECOMMISSIONED.name()).isEqualTo("DECOMMISSIONED");
    }
}
