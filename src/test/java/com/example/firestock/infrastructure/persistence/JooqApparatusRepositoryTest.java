package com.example.firestock.infrastructure.persistence;

import com.example.firestock.TestcontainersConfiguration;
import com.example.firestock.domain.apparatus.Apparatus;
import com.example.firestock.domain.apparatus.ApparatusRepository;
import com.example.firestock.domain.apparatus.ApparatusStatus;
import com.example.firestock.domain.apparatus.ApparatusType;
import com.example.firestock.domain.apparatus.DecommissionedApparatus;
import com.example.firestock.domain.apparatus.InServiceApparatus;
import com.example.firestock.domain.apparatus.OutOfServiceApparatus;
import com.example.firestock.domain.apparatus.ReserveApparatus;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.StationCode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Year;

import static com.example.firestock.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JooqApparatusRepositoryTest {

    @Autowired
    private ApparatusRepository repository;

    @Autowired
    private DSLContext create;

    private StationId testStationId;
    private StationId otherStationId;

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

        // Create test stations
        testStationId = StationId.generate();
        create.insertInto(STATION)
                .set(STATION.ID, testStationId)
                .set(STATION.CODE, new StationCode("ST01"))
                .set(STATION.NAME, "Test Station")
                .execute();

        otherStationId = StationId.generate();
        create.insertInto(STATION)
                .set(STATION.ID, otherStationId)
                .set(STATION.CODE, new StationCode("ST02"))
                .set(STATION.NAME, "Other Station")
                .execute();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("inserts new InServiceApparatus")
        void insertsNewInServiceApparatus() {
            var apparatus = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );

            var saved = repository.save(apparatus);

            assertNotNull(saved);
            assertEquals(apparatus.id(), saved.id());
            assertTrue(saved instanceof InServiceApparatus);
        }

        @Test
        @DisplayName("inserts InServiceApparatus with all fields")
        void insertsInServiceApparatusWithAllFields() {
            var apparatus = new InServiceApparatus(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    "1HTMS12345",
                    ApparatusType.ENGINE,
                    "Pierce",
                    "Velocity",
                    Year.of(2022),
                    testStationId,
                    new Barcode("APP-001"),
                    "Test notes"
            );

            repository.save(apparatus);
            var loaded = repository.findById(apparatus.id());

            assertTrue(loaded.isPresent());
            var result = (InServiceApparatus) loaded.get();
            assertEquals("1HTMS12345", result.vin());
            assertEquals("Pierce", result.make());
            assertEquals("Velocity", result.model());
            assertEquals(Year.of(2022), result.year());
            assertEquals("APP-001", result.barcode().value());
            assertEquals("Test notes", result.notes());
        }

        @Test
        @DisplayName("updates existing apparatus")
        void updatesExistingApparatus() {
            var apparatus = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(apparatus);

            var updated = apparatus.withVehicleDetails("VIN123", "Pierce", "Arrow", Year.of(2023));
            repository.save(updated);

            var loaded = repository.findById(apparatus.id());
            assertTrue(loaded.isPresent());
            assertEquals("VIN123", loaded.get().vin());
            assertEquals("Pierce", loaded.get().make());
        }

        @Test
        @DisplayName("saves OutOfServiceApparatus")
        void savesOutOfServiceApparatus() {
            var inService = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(inService);

            var outOfService = inService.putOutOfService("Engine repair needed");
            repository.save(outOfService);

            var loaded = repository.findById(outOfService.id());
            assertTrue(loaded.isPresent());
            assertTrue(loaded.get() instanceof OutOfServiceApparatus);
            assertEquals(ApparatusStatus.OUT_OF_SERVICE, loaded.get().status());
            assertEquals("Engine repair needed", loaded.get().notes());
        }

        @Test
        @DisplayName("saves ReserveApparatus")
        void savesReserveApparatus() {
            var inService = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(inService);

            var reserve = inService.moveToReserve("Moved to reserve fleet");
            repository.save(reserve);

            var loaded = repository.findById(reserve.id());
            assertTrue(loaded.isPresent());
            assertTrue(loaded.get() instanceof ReserveApparatus);
            assertEquals(ApparatusStatus.RESERVE, loaded.get().status());
        }

        @Test
        @DisplayName("saves DecommissionedApparatus")
        void savesDecommissionedApparatus() {
            var inService = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(inService);

            var decommissioned = inService.decommission("End of service life");
            repository.save(decommissioned);

            var loaded = repository.findById(decommissioned.id());
            assertTrue(loaded.isPresent());
            assertTrue(loaded.get() instanceof DecommissionedApparatus);
            assertEquals(ApparatusStatus.DECOMMISSIONED, loaded.get().status());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns empty when not found")
        void returnsEmptyWhenNotFound() {
            var result = repository.findById(ApparatusId.generate());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("maps to correct sealed type based on status")
        void mapsToCorrectSealedType() {
            // Create apparatus in different states
            var inService = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(inService);

            var outOfService = new OutOfServiceApparatus(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 2"),
                    null,
                    ApparatusType.ENGINE,
                    null, null, null,
                    testStationId,
                    null,
                    "Maintenance"
            );
            repository.save(outOfService);

            // Verify correct types
            var loadedInService = repository.findById(inService.id());
            assertTrue(loadedInService.isPresent());
            assertTrue(loadedInService.get() instanceof InServiceApparatus);

            var loadedOutOfService = repository.findById(outOfService.id());
            assertTrue(loadedOutOfService.isPresent());
            assertTrue(loadedOutOfService.get() instanceof OutOfServiceApparatus);
        }
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        @DisplayName("returns true when apparatus exists")
        void returnsTrueWhenExists() {
            var apparatus = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(apparatus);

            assertTrue(repository.existsById(apparatus.id()));
        }

        @Test
        @DisplayName("returns false when apparatus does not exist")
        void returnsFalseWhenNotExists() {
            assertFalse(repository.existsById(ApparatusId.generate()));
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteByIdTests {

        @Test
        @DisplayName("deletes existing apparatus")
        void deletesExistingApparatus() {
            var apparatus = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(apparatus);

            repository.deleteById(apparatus.id());

            assertFalse(repository.existsById(apparatus.id()));
        }

        @Test
        @DisplayName("does not throw when apparatus does not exist")
        void doesNotThrowWhenNotExists() {
            assertDoesNotThrow(() -> repository.deleteById(ApparatusId.generate()));
        }
    }

    @Nested
    @DisplayName("findInServiceById()")
    class FindInServiceByIdTests {

        @Test
        @DisplayName("returns InServiceApparatus when in service")
        void returnsWhenInService() {
            var apparatus = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(apparatus);

            var result = repository.findInServiceById(apparatus.id());

            assertTrue(result.isPresent());
            assertEquals(apparatus.id(), result.get().id());
        }

        @Test
        @DisplayName("returns empty when not in service")
        void returnsEmptyWhenNotInService() {
            var apparatus = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            repository.save(apparatus);

            var outOfService = apparatus.putOutOfService("Maintenance");
            repository.save(outOfService);

            var result = repository.findInServiceById(apparatus.id());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByStationId()")
    class FindByStationIdTests {

        @Test
        @DisplayName("returns all apparatus for station")
        void returnsAllApparatusForStation() {
            // Create apparatus at test station
            var apparatus1 = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            var apparatus2 = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Ladder 1"),
                    ApparatusType.LADDER,
                    testStationId
            );
            // Create apparatus at other station
            var apparatus3 = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 2"),
                    ApparatusType.ENGINE,
                    otherStationId
            );

            repository.save(apparatus1);
            repository.save(apparatus2);
            repository.save(apparatus3);

            var result = repository.findByStationId(testStationId);

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("findInServiceByStationId()")
    class FindInServiceByStationIdTests {

        @Test
        @DisplayName("returns only in-service apparatus for station")
        void returnsOnlyInServiceApparatusForStation() {
            var inService = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            var outOfService = new OutOfServiceApparatus(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 2"),
                    null,
                    ApparatusType.ENGINE,
                    null, null, null,
                    testStationId,
                    null,
                    "Maintenance"
            );

            repository.save(inService);
            repository.save(outOfService);

            var result = repository.findInServiceByStationId(testStationId);

            assertEquals(1, result.size());
            assertEquals(inService.id(), result.get(0).id());
        }
    }

    @Nested
    @DisplayName("findAllInService()")
    class FindAllInServiceTests {

        @Test
        @DisplayName("returns all in-service apparatus across all stations")
        void returnsAllInServiceApparatus() {
            var inService1 = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            var inService2 = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 2"),
                    ApparatusType.ENGINE,
                    otherStationId
            );
            var outOfService = new OutOfServiceApparatus(
                    ApparatusId.generate(),
                    new UnitNumber("Ladder 1"),
                    null,
                    ApparatusType.LADDER,
                    null, null, null,
                    testStationId,
                    null,
                    null
            );

            repository.save(inService1);
            repository.save(inService2);
            repository.save(outOfService);

            var result = repository.findAllInService();

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("findAllActive()")
    class FindAllActiveTests {

        @Test
        @DisplayName("excludes decommissioned apparatus")
        void excludesDecommissionedApparatus() {
            var inService = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            var decommissioned = new DecommissionedApparatus(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 2"),
                    null,
                    ApparatusType.ENGINE,
                    null, null, null,
                    testStationId,
                    null,
                    "Retired"
            );

            repository.save(inService);
            repository.save(decommissioned);

            var result = repository.findAllActive();

            assertEquals(1, result.size());
            assertEquals(inService.id(), result.get(0).id());
        }
    }

    @Nested
    @DisplayName("countByStationId()")
    class CountByStationIdTests {

        @Test
        @DisplayName("counts all apparatus at station")
        void countsAllApparatusAtStation() {
            var apparatus1 = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            var apparatus2 = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Ladder 1"),
                    ApparatusType.LADDER,
                    testStationId
            );
            var apparatusOther = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 2"),
                    ApparatusType.ENGINE,
                    otherStationId
            );

            repository.save(apparatus1);
            repository.save(apparatus2);
            repository.save(apparatusOther);

            assertEquals(2, repository.countByStationId(testStationId));
            assertEquals(1, repository.countByStationId(otherStationId));
        }
    }

    @Nested
    @DisplayName("countInServiceByStationId()")
    class CountInServiceByStationIdTests {

        @Test
        @DisplayName("counts only in-service apparatus at station")
        void countsOnlyInServiceApparatusAtStation() {
            var inService = InServiceApparatus.create(
                    ApparatusId.generate(),
                    new UnitNumber("Engine 1"),
                    ApparatusType.ENGINE,
                    testStationId
            );
            var outOfService = new OutOfServiceApparatus(
                    ApparatusId.generate(),
                    new UnitNumber("Ladder 1"),
                    null,
                    ApparatusType.LADDER,
                    null, null, null,
                    testStationId,
                    null,
                    null
            );

            repository.save(inService);
            repository.save(outOfService);

            assertEquals(1, repository.countInServiceByStationId(testStationId));
        }
    }
}
