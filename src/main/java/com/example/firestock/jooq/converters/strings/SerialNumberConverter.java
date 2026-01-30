package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.SerialNumber;
import org.jooq.Converter;

public class SerialNumberConverter implements Converter<String, SerialNumber> {
    @Override
    public SerialNumber from(String db) {
        return db == null ? null : new SerialNumber(db);
    }

    @Override
    public String to(SerialNumber user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<SerialNumber> toType() {
        return SerialNumber.class;
    }
}
