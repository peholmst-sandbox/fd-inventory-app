package com.example.firestock.security;

import com.example.firestock.domain.primitives.strings.EmailAddress;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.example.firestock.jooq.Tables.APP_USER;

/**
 * Query class for user-related read operations for authentication.
 */
@Component
public class UserQuery {

    private final DSLContext create;

    public UserQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds a user by their email address for authentication.
     *
     * @param email the email address to search for
     * @return the user details if found
     */
    public Optional<FirestockUserDetails> findByEmail(EmailAddress email) {
        return create.select(
                APP_USER.ID,
                APP_USER.EMAIL,
                APP_USER.PASSWORD_HASH,
                APP_USER.FIRST_NAME,
                APP_USER.LAST_NAME,
                APP_USER.ROLE,
                APP_USER.IS_ACTIVE)
            .from(APP_USER)
            .where(APP_USER.EMAIL.eq(email))
            .fetchOptional(r -> new FirestockUserDetails(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                r.value7()
            ));
    }

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user exists with this email
     */
    public boolean existsByEmail(EmailAddress email) {
        return create.fetchExists(
            create.selectOne()
                .from(APP_USER)
                .where(APP_USER.EMAIL.eq(email))
        );
    }
}
