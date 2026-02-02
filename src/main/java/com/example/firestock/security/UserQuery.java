package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.EmailAddress;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.example.firestock.jooq.Tables.APP_USER;
import static com.example.firestock.jooq.Tables.USER_STATION_ASSIGNMENT;

/**
 * Query class for user-related read operations for authentication.
 */
@Component
class UserQuery {

    private final DSLContext create;

    UserQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds a user by their email address for authentication.
     * Also loads station assignments for the user.
     *
     * @param email the email address to search for
     * @return the user details if found
     */
    Optional<FirestockUserDetails> findByEmail(EmailAddress email) {
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
            .fetchOptional(r -> {
                UserId userId = r.value1();
                Set<StationId> stationIds = new HashSet<>();
                StationId primaryStationId = null;

                // Load station assignments for the user
                var assignments = create.select(
                        USER_STATION_ASSIGNMENT.STATION_ID,
                        USER_STATION_ASSIGNMENT.IS_PRIMARY)
                    .from(USER_STATION_ASSIGNMENT)
                    .where(USER_STATION_ASSIGNMENT.USER_ID.eq(userId))
                    .fetch();

                for (var assignment : assignments) {
                    StationId stationId = assignment.value1();
                    stationIds.add(stationId);
                    if (assignment.value2()) {
                        primaryStationId = stationId;
                    }
                }

                return new FirestockUserDetails(
                    userId,
                    r.value2(),
                    r.value3(),
                    r.value4(),
                    r.value5(),
                    r.value6(),
                    r.value7(),
                    stationIds,
                    primaryStationId
                );
            });
    }

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user exists with this email
     */
    boolean existsByEmail(EmailAddress email) {
        return create.fetchExists(
            create.selectOne()
                .from(APP_USER)
                .where(APP_USER.EMAIL.eq(email))
        );
    }
}
