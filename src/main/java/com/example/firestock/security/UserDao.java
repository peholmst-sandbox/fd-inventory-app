package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.BadgeNumber;
import com.example.firestock.domain.primitives.strings.EmailAddress;
import com.example.firestock.jooq.enums.UserRole;
import com.example.firestock.jooq.tables.records.AppUserRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.example.firestock.jooq.Tables.APP_USER;

/**
 * DAO class for user write operations.
 */
@Component
class UserDao {

    private final DSLContext create;

    UserDao(DSLContext create) {
        this.create = create;
    }

    /**
     * Creates a new user with a password.
     *
     * @param email the user's email address
     * @param passwordHash the bcrypt hash of the user's password
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param badgeNumber optional badge number
     * @param role the user's role
     * @return the ID of the newly created user
     */
    UserId insert(EmailAddress email, String passwordHash, String firstName,
                         String lastName, BadgeNumber badgeNumber, UserRole role) {
        AppUserRecord record = create.newRecord(APP_USER);
        record.setEmail(email);
        record.setPasswordHash(passwordHash);
        record.setFirstName(firstName);
        record.setLastName(lastName);
        record.setBadgeNumber(badgeNumber);
        record.setRole(role);
        record.setIsActive(true);
        record.store();
        return record.getId();
    }

    /**
     * Updates the last login timestamp for a user.
     *
     * @param userId the user ID
     */
    void updateLastLogin(UserId userId) {
        create.update(APP_USER)
            .set(APP_USER.LAST_LOGIN_AT, Instant.now())
            .set(APP_USER.UPDATED_AT, Instant.now())
            .where(APP_USER.ID.eq(userId))
            .execute();
    }
}
