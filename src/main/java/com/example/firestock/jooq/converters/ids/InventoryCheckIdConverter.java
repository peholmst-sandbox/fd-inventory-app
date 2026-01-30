package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import org.jooq.Converter;

import java.util.UUID;

public class InventoryCheckIdConverter implements Converter<UUID, InventoryCheckId> {
    @Override
    public InventoryCheckId from(UUID db) {
        return db == null ? null : InventoryCheckId.of(db);
    }

    @Override
    public UUID to(InventoryCheckId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<InventoryCheckId> toType() {
        return InventoryCheckId.class;
    }
}
