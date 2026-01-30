package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.TransferRecordId;
import org.jooq.Converter;

import java.util.UUID;

public class TransferRecordIdConverter implements Converter<UUID, TransferRecordId> {
    @Override
    public TransferRecordId from(UUID db) {
        return db == null ? null : TransferRecordId.of(db);
    }

    @Override
    public UUID to(TransferRecordId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<TransferRecordId> toType() {
        return TransferRecordId.class;
    }
}
