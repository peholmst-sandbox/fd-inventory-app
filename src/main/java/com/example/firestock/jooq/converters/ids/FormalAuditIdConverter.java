package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.FormalAuditId;
import org.jooq.Converter;

import java.util.UUID;

public class FormalAuditIdConverter implements Converter<UUID, FormalAuditId> {
    @Override
    public FormalAuditId from(UUID db) {
        return db == null ? null : FormalAuditId.of(db);
    }

    @Override
    public UUID to(FormalAuditId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<FormalAuditId> toType() {
        return FormalAuditId.class;
    }
}
