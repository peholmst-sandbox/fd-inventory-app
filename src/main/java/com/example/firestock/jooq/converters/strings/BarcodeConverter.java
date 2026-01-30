package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.Barcode;
import org.jooq.Converter;

public class BarcodeConverter implements Converter<String, Barcode> {
    @Override
    public Barcode from(String db) {
        return db == null ? null : new Barcode(db);
    }

    @Override
    public String to(Barcode user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<Barcode> toType() {
        return Barcode.class;
    }
}
