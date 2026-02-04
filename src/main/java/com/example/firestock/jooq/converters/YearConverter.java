package com.example.firestock.jooq.converters;

import org.jooq.Converter;

import java.time.Year;

/**
 * Global jOOQ converter for converting between database INTEGER columns
 * and Java Year values.
 */
public class YearConverter implements Converter<Integer, Year> {

    @Override
    public Year from(Integer db) {
        return db == null ? null : Year.of(db);
    }

    @Override
    public Integer to(Year user) {
        return user == null ? null : user.getValue();
    }

    @Override
    public Class<Integer> fromType() {
        return Integer.class;
    }

    @Override
    public Class<Year> toType() {
        return Year.class;
    }
}
