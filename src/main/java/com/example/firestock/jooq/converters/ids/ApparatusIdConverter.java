package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import org.jooq.Converter;

import java.util.UUID;

public class ApparatusIdConverter implements Converter<UUID, ApparatusId> {
    @Override
    public ApparatusId from(UUID db) {
        return db == null ? null : ApparatusId.of(db);
    }

    @Override
    public UUID to(ApparatusId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<ApparatusId> toType() {
        return ApparatusId.class;
    }
}
