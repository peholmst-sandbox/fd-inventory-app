package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import org.jooq.Converter;

import java.util.UUID;

public class ConsumableStockIdConverter implements Converter<UUID, ConsumableStockId> {
    @Override
    public ConsumableStockId from(UUID db) {
        return db == null ? null : ConsumableStockId.of(db);
    }

    @Override
    public UUID to(ConsumableStockId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<ConsumableStockId> toType() {
        return ConsumableStockId.class;
    }
}
