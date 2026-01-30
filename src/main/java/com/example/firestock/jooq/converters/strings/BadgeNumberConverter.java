package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.BadgeNumber;
import org.jooq.Converter;

public class BadgeNumberConverter implements Converter<String, BadgeNumber> {
    @Override
    public BadgeNumber from(String db) {
        return db == null ? null : new BadgeNumber(db);
    }

    @Override
    public String to(BadgeNumber user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<BadgeNumber> toType() {
        return BadgeNumber.class;
    }
}
