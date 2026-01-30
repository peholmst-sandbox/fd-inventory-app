package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import org.jooq.Converter;

import java.util.UUID;

public class EquipmentTypeIdConverter implements Converter<UUID, EquipmentTypeId> {
    @Override
    public EquipmentTypeId from(UUID db) {
        return db == null ? null : EquipmentTypeId.of(db);
    }

    @Override
    public UUID to(EquipmentTypeId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<EquipmentTypeId> toType() {
        return EquipmentTypeId.class;
    }
}
