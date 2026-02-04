package com.example.firestock.domain.apparatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompartmentLocation} enum demonstrating physical locations on apparatus.
 */
@DisplayName("CompartmentLocation")
class CompartmentLocationTest {

    @Test
    void has_all_expected_locations() {
        assertThat(CompartmentLocation.values())
                .containsExactlyInAnyOrder(
                        CompartmentLocation.DRIVER_SIDE,
                        CompartmentLocation.PASSENGER_SIDE,
                        CompartmentLocation.REAR,
                        CompartmentLocation.FRONT,
                        CompartmentLocation.TOP,
                        CompartmentLocation.INTERIOR,
                        CompartmentLocation.CROSSLAY
                );
    }

    @Test
    void can_be_converted_from_string() {
        assertThat(CompartmentLocation.valueOf("DRIVER_SIDE")).isEqualTo(CompartmentLocation.DRIVER_SIDE);
        assertThat(CompartmentLocation.valueOf("PASSENGER_SIDE")).isEqualTo(CompartmentLocation.PASSENGER_SIDE);
        assertThat(CompartmentLocation.valueOf("REAR")).isEqualTo(CompartmentLocation.REAR);
    }

    @Test
    void has_readable_name() {
        assertThat(CompartmentLocation.DRIVER_SIDE.name()).isEqualTo("DRIVER_SIDE");
        assertThat(CompartmentLocation.CROSSLAY.name()).isEqualTo("CROSSLAY");
    }
}
