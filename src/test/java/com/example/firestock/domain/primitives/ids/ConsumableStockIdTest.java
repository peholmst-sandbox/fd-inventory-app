package com.example.firestock.domain.primitives.ids;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumableStockIdTest {

    @Test
    void accepts_valid_uuid() {
        var uuid = UUID.randomUUID();
        var id = new ConsumableStockId(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new ConsumableStockId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Consumable stock ID cannot be null");
    }

    @Test
    void generate_creates_new_id() {
        var id1 = ConsumableStockId.generate();
        var id2 = ConsumableStockId.generate();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void of_creates_id_from_uuid() {
        var uuid = UUID.randomUUID();
        var id = ConsumableStockId.of(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void toString_returns_uuid_string() {
        var uuid = UUID.randomUUID();
        var id = new ConsumableStockId(uuid);
        assertThat(id.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void equals_compares_by_value() {
        var uuid = UUID.randomUUID();
        var id1 = new ConsumableStockId(uuid);
        var id2 = new ConsumableStockId(uuid);
        assertThat(id1).isEqualTo(id2);
    }
}
