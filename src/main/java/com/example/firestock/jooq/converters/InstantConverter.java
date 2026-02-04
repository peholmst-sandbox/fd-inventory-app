package com.example.firestock.jooq.converters;

import org.jooq.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Global jOOQ converter for converting between database TIMESTAMP columns (LocalDateTime)
 * and Java Instant values.
 *
 * <p>This converter uses the system default timezone for the conversion, which ensures
 * consistent behaviour across the application.
 */
public class InstantConverter implements Converter<LocalDateTime, Instant> {
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    @Override
    public Instant from(LocalDateTime db) {
        return db == null ? null : db.atZone(SYSTEM_ZONE).toInstant();
    }

    @Override
    public LocalDateTime to(Instant user) {
        return user == null ? null : LocalDateTime.ofInstant(user, SYSTEM_ZONE);
    }

    @Override
    public Class<LocalDateTime> fromType() {
        return LocalDateTime.class;
    }

    @Override
    public Class<Instant> toType() {
        return Instant.class;
    }
}
