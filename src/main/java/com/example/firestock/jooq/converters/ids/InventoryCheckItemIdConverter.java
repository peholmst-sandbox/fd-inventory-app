package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.InventoryCheckItemId;
import org.jooq.Converter;

import java.util.UUID;

public class InventoryCheckItemIdConverter implements Converter<UUID, InventoryCheckItemId> {
    @Override
    public InventoryCheckItemId from(UUID db) {
        return db == null ? null : InventoryCheckItemId.of(db);
    }

    @Override
    public UUID to(InventoryCheckItemId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<InventoryCheckItemId> toType() {
        return InventoryCheckItemId.class;
    }
}
