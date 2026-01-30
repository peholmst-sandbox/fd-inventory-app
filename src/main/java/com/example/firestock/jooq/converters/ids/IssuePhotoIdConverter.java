package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.IssuePhotoId;
import org.jooq.Converter;

import java.util.UUID;

public class IssuePhotoIdConverter implements Converter<UUID, IssuePhotoId> {
    @Override
    public IssuePhotoId from(UUID db) {
        return db == null ? null : IssuePhotoId.of(db);
    }

    @Override
    public UUID to(IssuePhotoId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<IssuePhotoId> toType() {
        return IssuePhotoId.class;
    }
}
