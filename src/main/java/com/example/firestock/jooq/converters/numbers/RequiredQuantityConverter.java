package com.example.firestock.jooq.converters.numbers;

import com.example.firestock.domain.primitives.numbers.RequiredQuantity;
import org.jooq.Converter;

public class RequiredQuantityConverter implements Converter<Integer, RequiredQuantity> {
    @Override
    public RequiredQuantity from(Integer db) {
        return db == null ? null : RequiredQuantity.of(db);
    }

    @Override
    public Integer to(RequiredQuantity user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<Integer> fromType() {
        return Integer.class;
    }

    @Override
    public Class<RequiredQuantity> toType() {
        return RequiredQuantity.class;
    }
}
