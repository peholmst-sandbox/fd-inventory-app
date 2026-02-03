package com.example.firestock.domain.apparatus;

import com.example.firestock.jooq.enums.ApparatusType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApparatusType} enum demonstrating apparatus classification.
 */
@DisplayName("ApparatusType")
class ApparatusTypeTest {

    @Test
    void has_all_expected_types() {
        assertThat(ApparatusType.values())
                .containsExactlyInAnyOrder(
                        ApparatusType.ENGINE,
                        ApparatusType.LADDER,
                        ApparatusType.RESCUE,
                        ApparatusType.TANKER,
                        ApparatusType.AMBULANCE,
                        ApparatusType.COMMAND,
                        ApparatusType.UTILITY,
                        ApparatusType.OTHER
                );
    }

    @Test
    void can_be_converted_from_string() {
        assertThat(ApparatusType.valueOf("ENGINE")).isEqualTo(ApparatusType.ENGINE);
        assertThat(ApparatusType.valueOf("LADDER")).isEqualTo(ApparatusType.LADDER);
        assertThat(ApparatusType.valueOf("RESCUE")).isEqualTo(ApparatusType.RESCUE);
    }

    @Test
    void has_readable_name() {
        assertThat(ApparatusType.ENGINE.name()).isEqualTo("ENGINE");
        assertThat(ApparatusType.AMBULANCE.name()).isEqualTo("AMBULANCE");
    }
}
