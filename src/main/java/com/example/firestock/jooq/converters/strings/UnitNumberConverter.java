package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.UnitNumber;
import org.jooq.Converter;

public class UnitNumberConverter implements Converter<String, UnitNumber> {
    @Override
    public UnitNumber from(String db) {
        return db == null ? null : new UnitNumber(db);
    }

    @Override
    public String to(UnitNumber user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<UnitNumber> toType() {
        return UnitNumber.class;
    }
}
