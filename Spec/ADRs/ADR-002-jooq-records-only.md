# ADR-002: Use Only jOOQ Generated Records, Not POJOs or DAOs

## Status
Accepted

## Context

jOOQ's code generator can produce three types of artifacts:

1. **Records** - Type-safe representations of database rows that integrate with jOOQ's DSL
2. **POJOs** - Plain Java objects with getters/setters for each column
3. **DAOs** - Data Access Objects with generated CRUD methods

We need to decide which generated artifacts to use in FireStock.

### Problems with Generated POJOs

- **Duplication with domain model**: We already have domain primitives (ADR-001) and will have domain entities. Generated POJOs create a redundant layer.
- **Primitive obsession**: Generated POJOs use raw types (`String`, `UUID`) rather than domain primitives (`SerialNumber`, `StationId`).
- **Mapping overhead**: Using POJOs requires mapping to/from jOOQ Records and domain objects, adding boilerplate.
- **Mutable by default**: Generated POJOs have setters, encouraging mutation rather than immutability.

### Problems with Generated DAOs

- **Rigid CRUD operations**: Generated DAOs provide generic `insert`, `update`, `delete` methods that don't express domain intent.
- **Anemic domain model**: DAOs encourage putting business logic in services rather than domain objects.
- **Limited query flexibility**: Complex queries require bypassing the DAO anyway.
- **Transaction boundaries**: DAOs don't naturally compose within transaction boundaries managed by the service layer.

### Benefits of jOOQ Records

- **Native jOOQ integration**: Records work seamlessly with the DSL for queries, inserts, updates.
- **Dirty tracking**: Records track which fields changed, enabling efficient `UPDATE` statements.
- **Type-safe**: Full compile-time checking of column access.
- **Lightweight**: No additional mapping layer needed at the persistence boundary.

## Decision

We will **only generate jOOQ Records** and will **not generate POJOs or DAOs**.

### Code Generator Configuration

```xml
<generator>
    <generate>
        <records>true</records>
        <pojos>false</pojos>
        <daos>false</daos>
    </generate>
</generator>
```

### Data Access Pattern

Use jOOQ's DSL directly in repository classes:

```java
@Repository
public class EquipmentRepository {
    private final DSLContext dsl;

    public EquipmentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<EquipmentItemRecord> findById(EquipmentItemId id) {
        return dsl.selectFrom(EQUIPMENT_ITEM)
            .where(EQUIPMENT_ITEM.ID.eq(id.value()))
            .fetchOptional();
    }

    public void save(EquipmentItemRecord record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID());
            dsl.executeInsert(record);
        } else {
            dsl.executeUpdate(record);
        }
    }

    public List<EquipmentItemRecord> findByApparatus(ApparatusId apparatusId) {
        return dsl.selectFrom(EQUIPMENT_ITEM)
            .where(EQUIPMENT_ITEM.APPARATUS_ID.eq(apparatusId.value()))
            .fetch();
    }
}
```

### Domain Mapping

Map between jOOQ Records and domain objects at the repository boundary:

```java
public Equipment toDomain(EquipmentItemRecord record) {
    return new Equipment(
        new EquipmentItemId(record.getId()),
        new SerialNumber(record.getSerialNumber()),
        // ... other mappings
    );
}

public EquipmentItemRecord toRecord(Equipment equipment) {
    var record = new EquipmentItemRecord();
    record.setId(equipment.id().value());
    record.setSerialNumber(equipment.serialNumber().value());
    // ... other mappings
    return record;
}
```

## Consequences

### Positive
- **Single source of truth**: jOOQ Records for persistence, domain objects for business logic
- **No redundant generated code**: Smaller codebase, faster generation
- **Full query flexibility**: Direct DSL access for any query complexity
- **Clear architecture**: Repository layer owns the mapping between persistence and domain

### Negative
- **Manual repository methods**: Must write query methods rather than inheriting from generated DAOs
- **Mapping code**: Explicit mapping between Records and domain objects

### Neutral
- **Learning curve**: Developers must be familiar with jOOQ's DSL rather than relying on DAO methods

## References
- [ADR-001: Domain Primitives](ADR-001-domain-primitives.md)
- [jOOQ Code Generation](https://www.jooq.org/doc/latest/manual/code-generation/)
- [jOOQ Records vs POJOs](https://www.jooq.org/doc/latest/manual/sql-execution/fetching/pojos/)
