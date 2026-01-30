package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import org.jooq.Converter;

import java.util.UUID;

public class CompartmentIdConverter implements Converter<UUID, CompartmentId> {
    @Override
    public CompartmentId from(UUID db) {
        return db == null ? null : CompartmentId.of(db);
    }

    @Override
    public UUID to(CompartmentId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<CompartmentId> toType() {
        return CompartmentId.class;
    }
}
