package com.example.firestock.infrastructure.persistence;

import com.example.firestock.TestcontainersConfiguration;
import com.example.firestock.domain.apparatus.Compartment;
import com.example.firestock.domain.apparatus.CompartmentLocation;
import com.example.firestock.domain.apparatus.CompartmentRepository;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.StationCode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusStatus;
import com.example.firestock.jooq.enums.ApparatusType;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static com.example.firestock.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JooqCompartmentRepositoryTest {

    @Autowired
    private CompartmentRepository repository;

    @Autowired
    private DSLContext create;

    private StationId testStationId;
    private ApparatusId testApparatusId;
    private ApparatusId otherApparatusId;

    @BeforeEach
    void setUp() {
        // Clean up test data
        create.deleteFrom(FORMAL_AUDIT_ITEM).execute();
        create.deleteFrom(FORMAL_AUDIT).execute();
        create.deleteFrom(INVENTORY_CHECK_ITEM).execute();
        create.deleteFrom(ISSUE).execute();
        create.deleteFrom(INVENTORY_CHECK).execute();
        create.deleteFrom(EQUIPMENT_ITEM).execute();
        create.deleteFrom(CONSUMABLE_STOCK).execute();
        create.deleteFrom(MANIFEST_ENTRY).execute();
        create.deleteFrom(COMPARTMENT).execute();
        create.deleteFrom(APPARATUS).execute();
        create.deleteFrom(USER_STATION_ASSIGNMENT).execute();
        create.deleteFrom(APP_USER).execute();
        create.deleteFrom(STATION).execute();

        // Create test station
        testStationId = StationId.generate();
        create.insertInto(STATION)
                .set(STATION.ID, testStationId)
                .set(STATION.CODE, new StationCode("ST01"))
                .set(STATION.NAME, "Test Station")
                .execute();

        // Create test apparatus
        testApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
                .set(APPARATUS.ID, testApparatusId)
                .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 1"))
                .set(APPARATUS.TYPE, ApparatusType.ENGINE)
                .set(APPARATUS.STATION_ID, testStationId)
                .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
                .execute();

        otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
                .set(APPARATUS.ID, otherApparatusId)
                .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Ladder 1"))
                .set(APPARATUS.TYPE, ApparatusType.LADDER)
                .set(APPARATUS.STATION_ID, testStationId)
                .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
                .execute();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("inserts new compartment")
        void insertsNewCompartment() {
            var compartment = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );

            var saved = repository.save(compartment);

            assertNotNull(saved);
            assertEquals(compartment.id(), saved.id());
        }

        @Test
        @DisplayName("inserts compartment with all fields")
        void insertsCompartmentWithAllFields() {
            var compartment = new Compartment(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE,
                    "First driver side compartment",
                    1
            );

            repository.save(compartment);
            var loaded = repository.findById(compartment.id());

            assertTrue(loaded.isPresent());
            assertEquals("D1", loaded.get().code());
            assertEquals("Driver Side 1", loaded.get().name());
            assertEquals(CompartmentLocation.DRIVER_SIDE, loaded.get().location());
            assertEquals("First driver side compartment", loaded.get().description());
            assertEquals(1, loaded.get().displayOrder());
        }

        @Test
        @DisplayName("updates existing compartment")
        void updatesExistingCompartment() {
            var compartment = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );
            repository.save(compartment);

            var updated = compartment.withName("Updated Name").withDisplayOrder(5);
            repository.save(updated);

            var loaded = repository.findById(compartment.id());
            assertTrue(loaded.isPresent());
            assertEquals("Updated Name", loaded.get().name());
            assertEquals(5, loaded.get().displayOrder());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns compartment when exists")
        void returnsCompartmentWhenExists() {
            var compartment = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );
            repository.save(compartment);

            var result = repository.findById(compartment.id());

            assertTrue(result.isPresent());
            assertEquals(compartment.id(), result.get().id());
        }

        @Test
        @DisplayName("returns empty when not found")
        void returnsEmptyWhenNotFound() {
            var result = repository.findById(CompartmentId.generate());
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        @DisplayName("returns true when compartment exists")
        void returnsTrueWhenExists() {
            var compartment = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );
            repository.save(compartment);

            assertTrue(repository.existsById(compartment.id()));
        }

        @Test
        @DisplayName("returns false when compartment does not exist")
        void returnsFalseWhenNotExists() {
            assertFalse(repository.existsById(CompartmentId.generate()));
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteByIdTests {

        @Test
        @DisplayName("deletes existing compartment")
        void deletesExistingCompartment() {
            var compartment = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );
            repository.save(compartment);

            repository.deleteById(compartment.id());

            assertFalse(repository.existsById(compartment.id()));
        }

        @Test
        @DisplayName("does not throw when compartment does not exist")
        void doesNotThrowWhenNotExists() {
            assertDoesNotThrow(() -> repository.deleteById(CompartmentId.generate()));
        }
    }

    @Nested
    @DisplayName("findByApparatusId()")
    class FindByApparatusIdTests {

        @Test
        @DisplayName("returns all compartments for apparatus")
        void returnsAllCompartmentsForApparatus() {
            var comp1 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE,
                    2
            );
            var comp2 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "P1",
                    "Passenger Side 1",
                    CompartmentLocation.PASSENGER_SIDE,
                    1
            );
            var comp3 = Compartment.create(
                    CompartmentId.generate(),
                    otherApparatusId,
                    "R1",
                    "Rear 1",
                    CompartmentLocation.REAR
            );

            repository.save(comp1);
            repository.save(comp2);
            repository.save(comp3);

            var result = repository.findByApparatusId(testApparatusId);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("returns compartments ordered by display order")
        void returnsCompartmentsOrderedByDisplayOrder() {
            var comp1 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE,
                    3
            );
            var comp2 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "P1",
                    "Passenger Side 1",
                    CompartmentLocation.PASSENGER_SIDE,
                    1
            );
            var comp3 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "R1",
                    "Rear 1",
                    CompartmentLocation.REAR,
                    2
            );

            repository.save(comp1);
            repository.save(comp2);
            repository.save(comp3);

            var result = repository.findByApparatusId(testApparatusId);

            assertEquals(3, result.size());
            assertEquals("P1", result.get(0).code());
            assertEquals("R1", result.get(1).code());
            assertEquals("D1", result.get(2).code());
        }
    }

    @Nested
    @DisplayName("findByApparatusIdAndLocation()")
    class FindByApparatusIdAndLocationTests {

        @Test
        @DisplayName("returns compartments at specified location")
        void returnsCompartmentsAtLocation() {
            var comp1 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );
            var comp2 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D2",
                    "Driver Side 2",
                    CompartmentLocation.DRIVER_SIDE
            );
            var comp3 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "P1",
                    "Passenger Side 1",
                    CompartmentLocation.PASSENGER_SIDE
            );

            repository.save(comp1);
            repository.save(comp2);
            repository.save(comp3);

            var result = repository.findByApparatusIdAndLocation(testApparatusId, CompartmentLocation.DRIVER_SIDE);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(c -> c.location() == CompartmentLocation.DRIVER_SIDE));
        }
    }

    @Nested
    @DisplayName("deleteByApparatusId()")
    class DeleteByApparatusIdTests {

        @Test
        @DisplayName("deletes all compartments for apparatus")
        void deletesAllCompartmentsForApparatus() {
            var comp1 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );
            var comp2 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "P1",
                    "Passenger Side 1",
                    CompartmentLocation.PASSENGER_SIDE
            );
            var comp3 = Compartment.create(
                    CompartmentId.generate(),
                    otherApparatusId,
                    "R1",
                    "Rear 1",
                    CompartmentLocation.REAR
            );

            repository.save(comp1);
            repository.save(comp2);
            repository.save(comp3);

            int deleted = repository.deleteByApparatusId(testApparatusId);

            assertEquals(2, deleted);
            assertEquals(0, repository.countByApparatusId(testApparatusId));
            assertEquals(1, repository.countByApparatusId(otherApparatusId));
        }
    }

    @Nested
    @DisplayName("countByApparatusId()")
    class CountByApparatusIdTests {

        @Test
        @DisplayName("counts compartments for apparatus")
        void countsCompartmentsForApparatus() {
            var comp1 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "D1",
                    "Driver Side 1",
                    CompartmentLocation.DRIVER_SIDE
            );
            var comp2 = Compartment.create(
                    CompartmentId.generate(),
                    testApparatusId,
                    "P1",
                    "Passenger Side 1",
                    CompartmentLocation.PASSENGER_SIDE
            );
            var comp3 = Compartment.create(
                    CompartmentId.generate(),
                    otherApparatusId,
                    "R1",
                    "Rear 1",
                    CompartmentLocation.REAR
            );

            repository.save(comp1);
            repository.save(comp2);
            repository.save(comp3);

            assertEquals(2, repository.countByApparatusId(testApparatusId));
            assertEquals(1, repository.countByApparatusId(otherApparatusId));
        }
    }
}
