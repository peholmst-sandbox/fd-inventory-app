package com.example.fdinventoryapp;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static com.example.fdinventoryapp.jooq.Tables.HELLO_WORLD;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class HelloWorldRepositoryIT {

    @Autowired
    private DSLContext dsl;

    @Test
    void can_read_initial_hello_world_record() {
        // Flyway migration inserts "Hello, World!" - verify it exists
        var result = dsl.selectFrom(HELLO_WORLD)
            .where(HELLO_WORLD.MESSAGE.eq("Hello, World!"))
            .fetchOne();

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Hello, World!");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void can_insert_new_record() {
        String testMessage = "Test Message";

        var insertedRecord = dsl.insertInto(HELLO_WORLD)
            .set(HELLO_WORLD.MESSAGE, testMessage)
            .returning()
            .fetchOne();

        assertThat(insertedRecord).isNotNull();
        assertThat(insertedRecord.getMessage()).isEqualTo(testMessage);
        assertThat(insertedRecord.getCreatedAt()).isNotNull();
    }

    @Test
    void can_update_record() {
        // Insert a record to update
        var record = dsl.insertInto(HELLO_WORLD)
            .set(HELLO_WORLD.MESSAGE, "Original")
            .returning()
            .fetchOne();

        // Update it
        int updated = dsl.update(HELLO_WORLD)
            .set(HELLO_WORLD.MESSAGE, "Updated")
            .where(HELLO_WORLD.ID.eq(record.getId()))
            .execute();

        assertThat(updated).isEqualTo(1);

        // Verify the update
        var updatedRecord = dsl.selectFrom(HELLO_WORLD)
            .where(HELLO_WORLD.ID.eq(record.getId()))
            .fetchOne();

        assertThat(updatedRecord.getMessage()).isEqualTo("Updated");
    }

    @Test
    void can_delete_record() {
        // Insert a record to delete
        var record = dsl.insertInto(HELLO_WORLD)
            .set(HELLO_WORLD.MESSAGE, "To Delete")
            .returning()
            .fetchOne();

        // Delete it
        int deleted = dsl.deleteFrom(HELLO_WORLD)
            .where(HELLO_WORLD.ID.eq(record.getId()))
            .execute();

        assertThat(deleted).isEqualTo(1);

        // Verify deletion
        var deletedRecord = dsl.selectFrom(HELLO_WORLD)
            .where(HELLO_WORLD.ID.eq(record.getId()))
            .fetchOne();

        assertThat(deletedRecord).isNull();
    }

    @Test
    void created_at_is_set_automatically() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        var record = dsl.insertInto(HELLO_WORLD)
            .set(HELLO_WORLD.MESSAGE, "Timestamp Test")
            .returning()
            .fetchOne();

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(record.getCreatedAt())
            .isAfter(before)
            .isBefore(after);
    }
}
