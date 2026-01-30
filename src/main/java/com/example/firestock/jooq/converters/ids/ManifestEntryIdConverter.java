package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import org.jooq.Converter;

import java.util.UUID;

public class ManifestEntryIdConverter implements Converter<UUID, ManifestEntryId> {
    @Override
    public ManifestEntryId from(UUID db) {
        return db == null ? null : ManifestEntryId.of(db);
    }

    @Override
    public UUID to(ManifestEntryId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<ManifestEntryId> toType() {
        return ManifestEntryId.class;
    }
}
