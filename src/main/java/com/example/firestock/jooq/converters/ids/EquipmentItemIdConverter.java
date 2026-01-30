package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import org.jooq.Converter;

import java.util.UUID;

public class EquipmentItemIdConverter implements Converter<UUID, EquipmentItemId> {
    @Override
    public EquipmentItemId from(UUID db) {
        return db == null ? null : EquipmentItemId.of(db);
    }

    @Override
    public UUID to(EquipmentItemId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<EquipmentItemId> toType() {
        return EquipmentItemId.class;
    }
}
