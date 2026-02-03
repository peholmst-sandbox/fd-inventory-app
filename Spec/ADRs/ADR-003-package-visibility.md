# ADR-003: Use Package-Private Visibility for Internal Classes

## Status
Accepted

## Context

Currently all classes in the FireStock codebase use `public` visibility, regardless of whether they are intended for use outside their package. This creates several problems:

1. **Unclear public API**: It's not obvious which classes form a package's intended public interface versus internal implementation details.

2. **Accidental coupling**: Developers can inadvertently depend on internal classes, making refactoring difficult and creating tight coupling between packages.

3. **Encapsulation violation**: The principle of information hiding is undermined when all implementation details are exposed.

4. **Maintenance burden**: Changes to "internal" classes that happen to be public require considering all potential external consumers.

The codebase has clear layering with distinct responsibilities:
- **DAOs** - Data access operations (internal to the package)
- **Queries** - Complex query builders (internal to the package)
- **DTOs** - Data transfer objects (some cross package boundaries, some don't)
- **Services** - Business logic (form the public API)
- **Views** - Vaadin UI components (some are routed, some are internal dialogs)

## Decision

We will use **package-private (default) visibility** for all classes not intended for use outside their package. Public visibility is reserved for:

1. **Service classes** - Form the package's public API for business operations
2. **Cross-package DTOs** - Data objects that must be passed between packages
3. **Domain primitives** - Designed for cross-package use (per ADR-001)
4. **Spring configuration classes** - Required to be public for component scanning
5. **Vaadin routed views** - Views with `@Route` annotation need public access
6. **Security evaluators** - Classes referenced via SpEL expressions in `@PreAuthorize`

### Visibility Guidelines by Class Type

| Class Type | Default Visibility | Public When |
|------------|-------------------|-------------|
| DAO | Package-private | Never - internal implementation |
| Query | Package-private | Never - internal implementation |
| Internal DTO | Package-private | Never - used only within package |
| Cross-package DTO | Public | Always - designed for external use |
| Service | Public | Always - forms the public API |
| Internal Dialog | Package-private | Never - UI implementation detail |
| Routed View | Public | Always - needs routing access |
| Domain Primitive | Public | Always - designed for cross-package use |
| Security Evaluator | Public | Always - referenced via SpEL |

### Code Examples

**Package-private DAO (internal):**
```java
// No modifier = package-private
class InventoryCheckDao {
    private final DSLContext dsl;

    InventoryCheckDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    InventoryCheckRecord insert(InventoryCheckRecord record) {
        // ...
    }
}
```

**Public Service (API):**
```java
@Service
public class ShiftInventoryCheckService {
    private final InventoryCheckDao inventoryCheckDao;  // Uses internal DAO

    public StartedInventoryCheckDTO startInventoryCheck(ApparatusId apparatusId) {
        // ...
    }
}
```

**Package-private internal dialog:**
```java
class ItemVerificationDialog extends Dialog {
    // Internal UI component, not intended for reuse outside the view package
}
```

## Consequences

### Positive

- **Compile-time enforcement**: The compiler prevents accidental use of internal classes from other packages.
- **Clear API definition**: Public classes explicitly define what each package offers to others.
- **Safer refactoring**: Internal classes can be modified or removed without affecting other packages.
- **Better encapsulation**: Implementation details are properly hidden.
- **Self-documenting**: Visibility modifiers communicate intent about class usage.

### Negative

- **Discipline required**: Developers must consciously decide visibility for each new class.
- **Refactoring needed**: Existing code needs to be updated to use appropriate visibility.
- **Test considerations**: Package-private classes can only be tested from the same package (which is generally desirable for unit tests).

### Neutral

- **IDE support**: Modern IDEs clearly indicate visibility and can flag accidental dependencies.

## Concrete Examples from Current Codebase

Based on the current codebase structure, here are specific visibility recommendations:

**Should become package-private:**
- `InventoryCheckDao` - Internal to `inventorycheck` package
- `EquipmentDao` - Internal to `inventorycheck` package
- `ApparatusQuery` - Internal query builder
- `EquipmentQuery` - Internal query builder
- `ItemVerificationDialog` - Internal to `views.inventorycheck`

**Should remain public:**
- `ShiftInventoryCheckService` - Public service API
- `StationAccessEvaluator` - Used via SpEL in `@PreAuthorize` annotations
- All domain primitives in `domain.primitives` package
- `StartedInventoryCheckDTO` - Returned from public service methods

## References
- [ADR-001: Domain Primitives](ADR-001-domain-primitives.md)
- [Effective Java, Item 15: Minimize the accessibility of classes and members](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/)
- [Oracle: Controlling Access to Members of a Class](https://docs.oracle.com/javase/tutorial/java/javaOO/accesscontrol.html)
