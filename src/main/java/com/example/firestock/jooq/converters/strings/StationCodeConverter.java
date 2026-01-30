package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.StationCode;
import org.jooq.Converter;

public class StationCodeConverter implements Converter<String, StationCode> {
    @Override
    public StationCode from(String db) {
        return db == null ? null : new StationCode(db);
    }

    @Override
    public String to(StationCode user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<StationCode> toType() {
        return StationCode.class;
    }
}
