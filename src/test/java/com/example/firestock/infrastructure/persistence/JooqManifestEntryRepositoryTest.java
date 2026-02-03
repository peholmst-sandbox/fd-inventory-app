package com.example.firestock.infrastructure.persistence;

import com.example.firestock.TestcontainersConfiguration;
import com.example.firestock.domain.manifest.ManifestEntry;
import com.example.firestock.infrastructure.persistence.ManifestEntryRepository;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.numbers.RequiredQuantity;
import com.example.firestock.domain.primitives.strings.StationCode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusStatus;
import com.example.firestock.jooq.enums.ApparatusType;
import com.example.firestock.jooq.enums.CompartmentLocation;
import com.example.firestock.jooq.enums.EquipmentCategory;
import com.example.firestock.jooq.enums.TrackingMethod;
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
class JooqManifestEntryRepositoryTest {

    @Autowired
    private ManifestEntryRepository repository;

    @Autowired
    private DSLContext create;

    private StationId testStationId;
    private ApparatusId testApparatusId;
    private ApparatusId otherApparatusId;
    private CompartmentId testCompartmentId;
    private CompartmentId otherCompartmentId;
    private EquipmentTypeId testEquipmentTypeId;
    private EquipmentTypeId otherEquipmentTypeId;

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
        create.deleteFrom(EQUIPMENT_TYPE).execute();
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

        // Create test compartments
        testCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
                .set(COMPARTMENT.ID, testCompartmentId)
                .set(COMPARTMENT.APPARATUS_ID, testApparatusId)
                .set(COMPARTMENT.CODE, "D1")
                .set(COMPARTMENT.NAME, "Driver Side 1")
                .set(COMPARTMENT.LOCATION, CompartmentLocation.DRIVER_SIDE)
                .set(COMPARTMENT.DISPLAY_ORDER, 1)
                .execute();

        otherCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
                .set(COMPARTMENT.ID, otherCompartmentId)
                .set(COMPARTMENT.APPARATUS_ID, testApparatusId)
                .set(COMPARTMENT.CODE, "P1")
                .set(COMPARTMENT.NAME, "Passenger Side 1")
                .set(COMPARTMENT.LOCATION, CompartmentLocation.PASSENGER_SIDE)
                .set(COMPARTMENT.DISPLAY_ORDER, 2)
                .execute();

        // Create test equipment types
        testEquipmentTypeId = EquipmentTypeId.generate();
        create.insertInto(EQUIPMENT_TYPE)
                .set(EQUIPMENT_TYPE.ID, testEquipmentTypeId)
                .set(EQUIPMENT_TYPE.CODE, "SCBA")
                .set(EQUIPMENT_TYPE.NAME, "Self Contained Breathing Apparatus")
                .set(EQUIPMENT_TYPE.CATEGORY, EquipmentCategory.BREATHING)
                .set(EQUIPMENT_TYPE.TRACKING_METHOD, TrackingMethod.SERIALIZED)
                .execute();

        otherEquipmentTypeId = EquipmentTypeId.generate();
        create.insertInto(EQUIPMENT_TYPE)
                .set(EQUIPMENT_TYPE.ID, otherEquipmentTypeId)
                .set(EQUIPMENT_TYPE.CODE, "HALLIGAN")
                .set(EQUIPMENT_TYPE.NAME, "Halligan Bar")
                .set(EQUIPMENT_TYPE.CATEGORY, EquipmentCategory.TOOLS_HAND)
                .set(EQUIPMENT_TYPE.TRACKING_METHOD, TrackingMethod.SERIALIZED)
                .execute();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("inserts new manifest entry")
        void insertsNewManifestEntry() {
            var entry = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    2
            );

            var saved = repository.save(entry);

            assertNotNull(saved);
            assertEquals(entry.id(), saved.id());
        }

        @Test
        @DisplayName("inserts manifest entry with all fields")
        void insertsManifestEntryWithAllFields() {
            var entry = new ManifestEntry(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    RequiredQuantity.of(3),
                    true,
                    5,
                    "Test notes"
            );

            repository.save(entry);
            var loaded = repository.findById(entry.id());

            assertTrue(loaded.isPresent());
            assertEquals(testApparatusId, loaded.get().apparatusId());
            assertEquals(testCompartmentId, loaded.get().compartmentId());
            assertEquals(testEquipmentTypeId, loaded.get().equipmentTypeId());
            assertEquals(3, loaded.get().requiredQuantity().value());
            assertTrue(loaded.get().isCritical());
            assertEquals(5, loaded.get().displayOrder());
            assertEquals("Test notes", loaded.get().notes());
        }

        @Test
        @DisplayName("updates existing manifest entry")
        void updatesExistingManifestEntry() {
            var entry = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    2
            );
            repository.save(entry);

            var updated = entry.withRequiredQuantity(5).asOptional();
            repository.save(updated);

            var loaded = repository.findById(entry.id());
            assertTrue(loaded.isPresent());
            assertEquals(5, loaded.get().requiredQuantity().value());
            assertFalse(loaded.get().isCritical());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns manifest entry when exists")
        void returnsManifestEntryWhenExists() {
            var entry = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            repository.save(entry);

            var result = repository.findById(entry.id());

            assertTrue(result.isPresent());
            assertEquals(entry.id(), result.get().id());
        }

        @Test
        @DisplayName("returns empty when not found")
        void returnsEmptyWhenNotFound() {
            var result = repository.findById(ManifestEntryId.generate());
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        @DisplayName("returns true when entry exists")
        void returnsTrueWhenExists() {
            var entry = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            repository.save(entry);

            assertTrue(repository.existsById(entry.id()));
        }

        @Test
        @DisplayName("returns false when entry does not exist")
        void returnsFalseWhenNotExists() {
            assertFalse(repository.existsById(ManifestEntryId.generate()));
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteByIdTests {

        @Test
        @DisplayName("deletes existing manifest entry")
        void deletesExistingManifestEntry() {
            var entry = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            repository.save(entry);

            repository.deleteById(entry.id());

            assertFalse(repository.existsById(entry.id()));
        }

        @Test
        @DisplayName("does not throw when entry does not exist")
        void doesNotThrowWhenNotExists() {
            assertDoesNotThrow(() -> repository.deleteById(ManifestEntryId.generate()));
        }
    }

    @Nested
    @DisplayName("findByApparatusId()")
    class FindByApparatusIdTests {

        @Test
        @DisplayName("returns all entries for apparatus")
        void returnsAllEntriesForApparatus() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            ).withDisplayOrder(2);
            var entry2 = ManifestEntry.optional(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    otherEquipmentTypeId,
                    2
            ).withDisplayOrder(1);

            // Create compartment for other apparatus
            var otherApparatusCompartmentId = CompartmentId.generate();
            create.insertInto(COMPARTMENT)
                    .set(COMPARTMENT.ID, otherApparatusCompartmentId)
                    .set(COMPARTMENT.APPARATUS_ID, otherApparatusId)
                    .set(COMPARTMENT.CODE, "R1")
                    .set(COMPARTMENT.NAME, "Rear 1")
                    .set(COMPARTMENT.LOCATION, CompartmentLocation.REAR)
                    .execute();

            var entry3 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    otherApparatusId,
                    otherApparatusCompartmentId,
                    testEquipmentTypeId,
                    1
            );

            repository.save(entry1);
            repository.save(entry2);
            repository.save(entry3);

            var result = repository.findByApparatusId(testApparatusId);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("returns entries ordered by display order")
        void returnsEntriesOrderedByDisplayOrder() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            ).withDisplayOrder(3);
            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    otherEquipmentTypeId,
                    1
            ).withDisplayOrder(1);

            repository.save(entry1);
            repository.save(entry2);

            var result = repository.findByApparatusId(testApparatusId);

            assertEquals(2, result.size());
            assertEquals(entry2.id(), result.get(0).id());
            assertEquals(entry1.id(), result.get(1).id());
        }
    }

    @Nested
    @DisplayName("findCriticalByApparatusId()")
    class FindCriticalByApparatusIdTests {

        @Test
        @DisplayName("returns only critical entries")
        void returnsOnlyCriticalEntries() {
            var critical = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var optional = ManifestEntry.optional(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    otherEquipmentTypeId,
                    2
            );

            repository.save(critical);
            repository.save(optional);

            var result = repository.findCriticalByApparatusId(testApparatusId);

            assertEquals(1, result.size());
            assertEquals(critical.id(), result.get(0).id());
        }
    }

    @Nested
    @DisplayName("findByCompartmentId()")
    class FindByCompartmentIdTests {

        @Test
        @DisplayName("returns entries for compartment")
        void returnsEntriesForCompartment() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    otherEquipmentTypeId,
                    1
            );
            var entry3 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    testEquipmentTypeId,
                    1
            );

            repository.save(entry1);
            repository.save(entry2);
            repository.save(entry3);

            var result = repository.findByCompartmentId(testCompartmentId);

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("findByApparatusIdAndEquipmentTypeId()")
    class FindByApparatusIdAndEquipmentTypeIdTests {

        @Test
        @DisplayName("returns entries matching apparatus and equipment type")
        void returnsMatchingEntries() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var entry3 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    otherEquipmentTypeId,
                    1
            );

            repository.save(entry1);
            repository.save(entry2);
            repository.save(entry3);

            var result = repository.findByApparatusIdAndEquipmentTypeId(testApparatusId, testEquipmentTypeId);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(e -> e.equipmentTypeId().equals(testEquipmentTypeId)));
        }
    }

    @Nested
    @DisplayName("deleteByApparatusId()")
    class DeleteByApparatusIdTests {

        @Test
        @DisplayName("deletes all entries for apparatus")
        void deletesAllEntriesForApparatus() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    otherEquipmentTypeId,
                    1
            );

            // Create compartment for other apparatus
            var otherApparatusCompartmentId = CompartmentId.generate();
            create.insertInto(COMPARTMENT)
                    .set(COMPARTMENT.ID, otherApparatusCompartmentId)
                    .set(COMPARTMENT.APPARATUS_ID, otherApparatusId)
                    .set(COMPARTMENT.CODE, "R1")
                    .set(COMPARTMENT.NAME, "Rear 1")
                    .set(COMPARTMENT.LOCATION, CompartmentLocation.REAR)
                    .execute();

            var entry3 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    otherApparatusId,
                    otherApparatusCompartmentId,
                    testEquipmentTypeId,
                    1
            );

            repository.save(entry1);
            repository.save(entry2);
            repository.save(entry3);

            int deleted = repository.deleteByApparatusId(testApparatusId);

            assertEquals(2, deleted);
            assertEquals(0, repository.countByApparatusId(testApparatusId));
            assertEquals(1, repository.countByApparatusId(otherApparatusId));
        }
    }

    @Nested
    @DisplayName("deleteByCompartmentId()")
    class DeleteByCompartmentIdTests {

        @Test
        @DisplayName("deletes all entries for compartment")
        void deletesAllEntriesForCompartment() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    otherEquipmentTypeId,
                    1
            );
            var entry3 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    testEquipmentTypeId,
                    1
            );

            repository.save(entry1);
            repository.save(entry2);
            repository.save(entry3);

            int deleted = repository.deleteByCompartmentId(testCompartmentId);

            assertEquals(2, deleted);
            assertEquals(0, repository.countByCompartmentId(testCompartmentId));
            assertEquals(1, repository.countByCompartmentId(otherCompartmentId));
        }
    }

    @Nested
    @DisplayName("countByApparatusId()")
    class CountByApparatusIdTests {

        @Test
        @DisplayName("counts entries for apparatus")
        void countsEntriesForApparatus() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    otherEquipmentTypeId,
                    1
            );

            repository.save(entry1);
            repository.save(entry2);

            assertEquals(2, repository.countByApparatusId(testApparatusId));
        }
    }

    @Nested
    @DisplayName("countCriticalByApparatusId()")
    class CountCriticalByApparatusIdTests {

        @Test
        @DisplayName("counts only critical entries")
        void countsOnlyCriticalEntries() {
            var critical1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var critical2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    otherEquipmentTypeId,
                    1
            );
            var optional = ManifestEntry.optional(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    otherEquipmentTypeId,
                    1
            );

            repository.save(critical1);
            repository.save(critical2);
            repository.save(optional);

            assertEquals(2, repository.countCriticalByApparatusId(testApparatusId));
        }
    }

    @Nested
    @DisplayName("countByCompartmentId()")
    class CountByCompartmentIdTests {

        @Test
        @DisplayName("counts entries for compartment")
        void countsEntriesForCompartment() {
            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    testEquipmentTypeId,
                    1
            );
            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    testCompartmentId,
                    otherEquipmentTypeId,
                    1
            );
            var entry3 = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    testApparatusId,
                    otherCompartmentId,
                    testEquipmentTypeId,
                    1
            );

            repository.save(entry1);
            repository.save(entry2);
            repository.save(entry3);

            assertEquals(2, repository.countByCompartmentId(testCompartmentId));
            assertEquals(1, repository.countByCompartmentId(otherCompartmentId));
        }
    }
}
