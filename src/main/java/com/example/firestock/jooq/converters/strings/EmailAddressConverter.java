package com.example.firestock.jooq.converters.strings;

import com.example.firestock.domain.primitives.strings.EmailAddress;
import org.jooq.Converter;

public class EmailAddressConverter implements Converter<String, EmailAddress> {
    @Override
    public EmailAddress from(String db) {
        return db == null ? null : new EmailAddress(db);
    }

    @Override
    public String to(EmailAddress user) {
        return user == null ? null : user.value();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<EmailAddress> toType() {
        return EmailAddress.class;
    }
}
