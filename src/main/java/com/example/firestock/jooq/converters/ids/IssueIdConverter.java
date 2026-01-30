package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.IssueId;
import org.jooq.Converter;

import java.util.UUID;

public class IssueIdConverter implements Converter<UUID, IssueId> {
    @Override
    public IssueId from(UUID db) {
        return db == null ? null : IssueId.of(db);
    }

    @Override
    public UUID to(IssueId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<IssueId> toType() {
        return IssueId.class;
    }
}
