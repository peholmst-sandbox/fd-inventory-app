package com.example.firestock.jooq.converters.ids;

import com.example.firestock.domain.primitives.ids.UserId;
import org.jooq.Converter;

import java.util.UUID;

public class UserIdConverter implements Converter<UUID, UserId> {
    @Override
    public UserId from(UUID db) {
        return db == null ? null : UserId.of(db);
    }

    @Override
    public UUID to(UserId user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<UUID> fromType() {
        return UUID.class;
    }

    @Override
    public Class<UserId> toType() {
        return UserId.class;
    }
}
