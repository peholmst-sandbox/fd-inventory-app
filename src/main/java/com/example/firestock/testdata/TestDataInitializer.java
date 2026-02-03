package com.example.firestock.testdata;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.domain.primitives.strings.StationCode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusType;
import com.example.firestock.jooq.enums.CompartmentLocation;
import com.example.firestock.jooq.enums.EquipmentCategory;
import com.example.firestock.jooq.enums.TrackingMethod;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.example.firestock.jooq.Tables.*;

/**
 * Creates test data on application startup when enabled via configuration.
 * Only runs when firestock.testdata.create-test-data=true.
 *
 * <p>This initializer runs after TestUserInitializer (Order 100 vs default)
 * to ensure the test station exists.
 */
@Component
@Order(100)
class TestDataInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TestDataInitializer.class);

    private final TestDataProperties testDataProperties;
    private final DSLContext create;

    // Store created IDs for reference
    private final Map<String, EquipmentTypeId> equipmentTypes = new HashMap<>();

    TestDataInitializer(TestDataProperties testDataProperties, DSLContext create) {
        this.testDataProperties = testDataProperties;
        this.create = create;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!testDataProperties.isCreateTestData()) {
            return;
        }

        LOG.warn("Creating test data - this should only be enabled in development!");

        // Find the test station created by TestUserInitializer
        StationId testStationId = findTestStation();
        if (testStationId == null) {
            LOG.error("Test station TEST01 not found. Make sure firestock.security.create-test-users=true");
            return;
        }

        // Create equipment types
        createEquipmentTypes();

        // Create apparatus with compartments and equipment
        createTestApparatus(testStationId);

        LOG.info("Test data creation completed");
    }

    private StationId findTestStation() {
        return create.select(STATION.ID)
                .from(STATION)
                .where(STATION.CODE.eq(new StationCode("TEST01")))
                .fetchOptional(STATION.ID)
                .orElse(null);
    }

    private void createEquipmentTypes() {
        // SCBA (Self-Contained Breathing Apparatus)
        createEquipmentTypeIfNotExists("SCBA", "Self-Contained Breathing Apparatus",
                EquipmentCategory.BREATHING, TrackingMethod.SERIALIZED,
                "Scott", "Air-Pak X3 Pro", true, 365);

        // Hand Tools
        createEquipmentTypeIfNotExists("HALLIGAN", "Halligan Bar",
                EquipmentCategory.TOOLS_HAND, TrackingMethod.SERIALIZED,
                "Pro-Bar", "Halligan 30\"", false, null);

        createEquipmentTypeIfNotExists("AXE-FLAT", "Flat Head Axe",
                EquipmentCategory.TOOLS_HAND, TrackingMethod.SERIALIZED,
                "Council Tool", "Dayton Pattern 6lb", false, null);

        createEquipmentTypeIfNotExists("PIKE-POLE", "Pike Pole",
                EquipmentCategory.TOOLS_HAND, TrackingMethod.SERIALIZED,
                "Nupla", "Classic 6ft", false, null);

        // Electronics
        createEquipmentTypeIfNotExists("TIC", "Thermal Imaging Camera",
                EquipmentCategory.ELECTRONICS, TrackingMethod.SERIALIZED,
                "FLIR", "K55", true, 365);

        createEquipmentTypeIfNotExists("RADIO-PORT", "Portable Radio",
                EquipmentCategory.ELECTRONICS, TrackingMethod.SERIALIZED,
                "Motorola", "APX 8000", false, null);

        // Medical
        createEquipmentTypeIfNotExists("AED", "Automated External Defibrillator",
                EquipmentCategory.MEDICAL, TrackingMethod.SERIALIZED,
                "Philips", "HeartStart FRx", true, 730);

        createEquipmentTypeIfNotExists("FAK", "First Aid Kit",
                EquipmentCategory.MEDICAL, TrackingMethod.SERIALIZED,
                "North American Rescue", "Trauma Kit", false, null);

        // Hose
        createEquipmentTypeIfNotExists("HOSE-175", "Attack Hose 1.75\"",
                EquipmentCategory.HOSE, TrackingMethod.SERIALIZED,
                "Snap-Tite", "Armored Attack 1.75\"x50'", true, 365);

        createEquipmentTypeIfNotExists("HOSE-4", "Supply Hose 4\"",
                EquipmentCategory.HOSE, TrackingMethod.SERIALIZED,
                "Key Hose", "Big10 LDH 4\"x100'", true, 365);

        // Nozzles
        createEquipmentTypeIfNotExists("NOZZLE-FOG", "Fog Nozzle",
                EquipmentCategory.NOZZLES, TrackingMethod.SERIALIZED,
                "Elkhart Brass", "Phantom ST", false, null);

        createEquipmentTypeIfNotExists("NOZZLE-SMOOTH", "Smooth Bore Nozzle",
                EquipmentCategory.NOZZLES, TrackingMethod.SERIALIZED,
                "Akron Brass", "Turbojet", false, null);

        // Ladders (tracked as OTHER)
        createEquipmentTypeIfNotExists("LADDER-GROUND", "Ground Ladder",
                EquipmentCategory.OTHER, TrackingMethod.SERIALIZED,
                "Duo-Safety", "Aluminum 14ft", true, 365);

        createEquipmentTypeIfNotExists("LADDER-ROOF", "Roof Ladder",
                EquipmentCategory.OTHER, TrackingMethod.SERIALIZED,
                "Duo-Safety", "Aluminum 16ft w/Hooks", true, 365);

        createEquipmentTypeIfNotExists("LADDER-EXT", "Extension Ladder",
                EquipmentCategory.OTHER, TrackingMethod.SERIALIZED,
                "Duo-Safety", "Aluminum 24ft", true, 365);
    }

    private void createEquipmentTypeIfNotExists(String code, String name,
                                                  EquipmentCategory category, TrackingMethod trackingMethod,
                                                  String manufacturer, String model,
                                                  boolean requiresTesting, Integer testIntervalDays) {
        var existing = create.select(EQUIPMENT_TYPE.ID)
                .from(EQUIPMENT_TYPE)
                .where(EQUIPMENT_TYPE.CODE.eq(code))
                .fetchOptional(EQUIPMENT_TYPE.ID);

        if (existing.isPresent()) {
            equipmentTypes.put(code, existing.get());
            LOG.debug("Equipment type {} already exists", code);
            return;
        }

        EquipmentTypeId typeId = EquipmentTypeId.generate();
        create.insertInto(EQUIPMENT_TYPE)
                .set(EQUIPMENT_TYPE.ID, typeId)
                .set(EQUIPMENT_TYPE.CODE, code)
                .set(EQUIPMENT_TYPE.NAME, name)
                .set(EQUIPMENT_TYPE.CATEGORY, category)
                .set(EQUIPMENT_TYPE.TRACKING_METHOD, trackingMethod)
                .set(EQUIPMENT_TYPE.MANUFACTURER, manufacturer)
                .set(EQUIPMENT_TYPE.MODEL, model)
                .set(EQUIPMENT_TYPE.REQUIRES_TESTING, requiresTesting)
                .set(EQUIPMENT_TYPE.TEST_INTERVAL_DAYS, testIntervalDays)
                .set(EQUIPMENT_TYPE.IS_ACTIVE, true)
                .execute();

        equipmentTypes.put(code, typeId);
        LOG.info("Created equipment type: {} - {}", code, name);
    }

    private void createTestApparatus(StationId stationId) {
        UnitNumber unitNumber = new UnitNumber("Engine 1");

        // Check if apparatus already exists
        var existing = create.select(APPARATUS.ID)
                .from(APPARATUS)
                .where(APPARATUS.UNIT_NUMBER.eq(unitNumber))
                .fetchOptional(APPARATUS.ID);

        if (existing.isPresent()) {
            LOG.info("Apparatus {} already exists, skipping creation", unitNumber);
            return;
        }

        // Create apparatus
        ApparatusId apparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
                .set(APPARATUS.ID, apparatusId)
                .set(APPARATUS.UNIT_NUMBER, unitNumber)
                .set(APPARATUS.TYPE, ApparatusType.ENGINE)
                .set(APPARATUS.STATION_ID, stationId)
                .set(APPARATUS.MAKE, "Pierce")
                .set(APPARATUS.MODEL, "Arrow XT")
                .set(APPARATUS.YEAR, 2022)
                .set(APPARATUS.VIN, "4P1CD01H35A000001")
                .execute();

        LOG.info("Created apparatus: {}", unitNumber);

        // Create compartments
        CompartmentId leftSide = createCompartment(apparatusId, "L1", "Left Side - Front", CompartmentLocation.DRIVER_SIDE, 1);
        CompartmentId rightSide = createCompartment(apparatusId, "R1", "Right Side - Front", CompartmentLocation.PASSENGER_SIDE, 2);
        CompartmentId rear = createCompartment(apparatusId, "REAR", "Rear Compartment", CompartmentLocation.REAR, 3);
        CompartmentId top = createCompartment(apparatusId, "TOP", "Top / Ladder Rack", CompartmentLocation.TOP, 4);
        CompartmentId interior = createCompartment(apparatusId, "CAB", "Cab Interior", CompartmentLocation.INTERIOR, 5);

        // Create equipment items in compartments
        // Left Side - SCBA and forcible entry tools
        createEquipmentItem("SCBA", "SCBA-2024-001", apparatusId, leftSide);
        createEquipmentItem("SCBA", "SCBA-2024-002", apparatusId, leftSide);
        createEquipmentItem("HALLIGAN", "HB-2023-015", apparatusId, leftSide);
        createEquipmentItem("AXE-FLAT", "AXE-2023-008", apparatusId, leftSide);
        createEquipmentItem("PIKE-POLE", "PP6-2022-003", apparatusId, leftSide);

        // Right Side - Electronics and medical
        createEquipmentItem("TIC", "TIC-2024-001", apparatusId, rightSide);
        createEquipmentItem("RADIO-PORT", "RAD-2024-101", apparatusId, rightSide);
        createEquipmentItem("RADIO-PORT", "RAD-2024-102", apparatusId, rightSide);
        createEquipmentItem("FAK", "FAK-2024-001", apparatusId, rightSide);
        createEquipmentItem("AED", "AED-2023-005", apparatusId, rightSide);

        // Rear - Hose and nozzles
        createEquipmentItem("HOSE-175", "AH175-2024-001", apparatusId, rear);
        createEquipmentItem("HOSE-175", "AH175-2024-002", apparatusId, rear);
        createEquipmentItem("HOSE-4", "SH4-2024-001", apparatusId, rear);
        createEquipmentItem("NOZZLE-FOG", "NF-2024-001", apparatusId, rear);
        createEquipmentItem("NOZZLE-SMOOTH", "NSB-2024-001", apparatusId, rear);

        // Top - Ladders
        createEquipmentItem("LADDER-GROUND", "GL14-2024-001", apparatusId, top);
        createEquipmentItem("LADDER-ROOF", "RL16-2024-001", apparatusId, top);
        createEquipmentItem("LADDER-EXT", "EL24-2023-001", apparatusId, top);

        // Cab Interior - Additional SCBA for crew
        createEquipmentItem("SCBA", "SCBA-2024-003", apparatusId, interior);
        createEquipmentItem("SCBA", "SCBA-2024-004", apparatusId, interior);

        LOG.info("Created {} equipment items for {}", 20, unitNumber);
    }

    private CompartmentId createCompartment(ApparatusId apparatusId, String code, String name,
                                              CompartmentLocation location, int displayOrder) {
        CompartmentId compartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
                .set(COMPARTMENT.ID, compartmentId)
                .set(COMPARTMENT.APPARATUS_ID, apparatusId)
                .set(COMPARTMENT.CODE, code)
                .set(COMPARTMENT.NAME, name)
                .set(COMPARTMENT.LOCATION, location)
                .set(COMPARTMENT.DISPLAY_ORDER, displayOrder)
                .execute();

        LOG.debug("Created compartment: {} - {}", code, name);
        return compartmentId;
    }

    private void createEquipmentItem(String typeCode, String serialNumber,
                                       ApparatusId apparatusId, CompartmentId compartmentId) {
        EquipmentTypeId typeId = equipmentTypes.get(typeCode);
        if (typeId == null) {
            LOG.warn("Equipment type {} not found, skipping item {}", typeCode, serialNumber);
            return;
        }

        EquipmentItemId itemId = EquipmentItemId.generate();
        // Note: chk_location_consistency constraint requires:
        // - If on apparatus: apparatus_id and compartment_id set, station_id NULL
        // - If in storage: station_id set, apparatus_id and compartment_id NULL
        // Note: home_station_id must be NULL for DEPARTMENT-owned equipment per chk_crew_owned_home_station constraint
        create.insertInto(EQUIPMENT_ITEM)
                .set(EQUIPMENT_ITEM.ID, itemId)
                .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, typeId)
                .set(EQUIPMENT_ITEM.SERIAL_NUMBER, new SerialNumber(serialNumber))
                .set(EQUIPMENT_ITEM.APPARATUS_ID, apparatusId)
                .set(EQUIPMENT_ITEM.COMPARTMENT_ID, compartmentId)
                .set(EQUIPMENT_ITEM.ACQUISITION_DATE, LocalDate.now().minusYears(1))
                .execute();

        LOG.debug("Created equipment item: {} ({})", serialNumber, typeCode);
    }
}
