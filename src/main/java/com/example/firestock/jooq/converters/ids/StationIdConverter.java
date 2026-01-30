package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.StationId;
import org.jooq.Converter;

import java.util.UUID;

public class StationIdConverter implements Converter<UUID, StationId> {
    @Override
    public StationId from(UUID db) {
        return db == null ? null : StationId.of(db);
    }

    @Override
    public UUID to(StationId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<StationId> toType() {
        return StationId.class;
    }
}
