package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.FormalAuditItemId;
import org.jooq.Converter;

import java.util.UUID;

public class FormalAuditItemIdConverter implements Converter<UUID, FormalAuditItemId> {
    @Override
    public FormalAuditItemId from(UUID db) {
        return db == null ? null : FormalAuditItemId.of(db);
    }

    @Override
    public UUID to(FormalAuditItemId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<FormalAuditItemId> toType() {
        return FormalAuditItemId.class;
    }
}
