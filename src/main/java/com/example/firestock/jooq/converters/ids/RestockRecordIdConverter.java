package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.RestockRecordId;
import org.jooq.Converter;

import java.util.UUID;

public class RestockRecordIdConverter implements Converter<UUID, RestockRecordId> {
    @Override
    public RestockRecordId from(UUID db) {
        return db == null ? null : RestockRecordId.of(db);
    }

    @Override
    public UUID to(RestockRecordId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<RestockRecordId> toType() {
        return RestockRecordId.class;
    }
}
