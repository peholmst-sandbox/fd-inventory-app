package com.example.firestock.jooq.converters.numbers;

import com.example.firestock.domain.primitives.numbers.Quantity;
import org.jooq.Converter;

import java.math.BigDecimal;

public class QuantityConverter implements Converter<BigDecimal, Quantity> {
    @Override
    public Quantity from(BigDecimal db) {
        return db == null ? null : new Quantity(db);
    }

    @Override
    public BigDecimal to(Quantity user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<BigDecimal> fromType() {
        return BigDecimal.class;
    }

    @Override
    public Class<Quantity> toType() {
        return Quantity.class;
    }
}
