package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.ReferenceNumber;
import org.jooq.Converter;

public class ReferenceNumberConverter implements Converter<String, ReferenceNumber> {
    @Override
    public ReferenceNumber from(String db) {
        return db == null ? null : new ReferenceNumber(db);
    }

    @Override
    public String to(ReferenceNumber user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<ReferenceNumber> toType() {
        return ReferenceNumber.class;
    }
}
