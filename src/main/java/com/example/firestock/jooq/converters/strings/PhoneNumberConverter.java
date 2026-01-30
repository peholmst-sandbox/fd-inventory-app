package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.PhoneNumber;
import org.jooq.Converter;

public class PhoneNumberConverter implements Converter<String, PhoneNumber> {
    @Override
    public PhoneNumber from(String db) {
        return db == null ? null : new PhoneNumber(db);
    }

    @Override
    public String to(PhoneNumber user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<PhoneNumber> toType() {
        return PhoneNumber.class;
    }
}
