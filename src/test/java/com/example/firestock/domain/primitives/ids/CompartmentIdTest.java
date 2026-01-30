package com.example.firestock.domain.primitives.ids;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompartmentIdTest {

    @Test
    void accepts_valid_uuid() {
        var uuid = UUID.randomUUID();
        var id = new CompartmentId(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new CompartmentId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Compartment ID cannot be null");
    }

    @Test
    void generate_creates_new_id() {
        var id1 = CompartmentId.generate();
        var id2 = CompartmentId.generate();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void of_creates_id_from_uuid() {
        var uuid = UUID.randomUUID();
        var id = CompartmentId.of(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void toString_returns_uuid_string() {
        var uuid = UUID.randomUUID();
        var id = new CompartmentId(uuid);
        assertThat(id.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void equals_compares_by_value() {
        var uuid = UUID.randomUUID();
        var id1 = new CompartmentId(uuid);
        var id2 = new CompartmentId(uuid);
        assertThat(id1).isEqualTo(id2);
    }
}
