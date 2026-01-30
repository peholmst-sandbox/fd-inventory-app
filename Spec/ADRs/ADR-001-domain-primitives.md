# ADR-001: Use Domain Primitives for Type Safety and Validation

## Status
Accepted

## Context

FireStock manages fire apparatus inventory with many domain-meaningful values represented as primitive types:
- **Identifiers**: Station codes, unit numbers, badge numbers, serial numbers, barcodes, reference numbers
- **Contact info**: Email addresses, phone numbers
- **Quantities**: Stock quantities, required quantities
- **Entity IDs**: UUIDs for stations, apparatus, equipment, users, etc.

Using raw primitives creates several problems:

1. **Parameter mix-ups**: Methods accepting multiple UUIDs or strings can have arguments swapped without compiler errors:
   ```java
   // Which UUID is which? Easy to swap by mistake.
   void transferEquipment(UUID equipmentId, UUID sourceApparatusId, UUID destApparatusId)
   ```

2. **Scattered validation**: Constraints must be checked everywhere the value is used, leading to inconsistent enforcement.

3. **No domain meaning**: A `String` doesn't communicate whether it's a serial number, email, or SQL query.

4. **Security risk**: Unvalidated strings are attack vectors for injection vulnerabilities (OWASP Top 10 #3).

## Decision

We will use **Domain Primitives** for all domain-meaningful values. A domain primitive is:

1. **Immutable** - A value object where two instances with the same value are interchangeable
2. **Self-validating** - Validates all constraints in the constructor; an instance is always valid
3. **Type-safe** - The compiler prevents mixing values with different domain meanings

### Implementation Pattern

Use Java records with validation in the compact constructor:

```java
public record SerialNumber(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9\\-]{5,50}$");

    public SerialNumber {
        Objects.requireNonNull(value, "Serial number cannot be null");
        value = value.strip().toUpperCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid serial number format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### Domain Primitives to Implement

**String Wrappers:**
| Primitive | Wraps | Validation Rules |
|-----------|-------|------------------|
| `StationCode` | String | 1-20 chars, alphanumeric with hyphens |
| `UnitNumber` | String | 1-50 chars, apparatus identifier format |
| `BadgeNumber` | String | 1-50 chars, alphanumeric |
| `EmailAddress` | String | Valid email format |
| `SerialNumber` | String | 1-100 chars, alphanumeric with hyphens |
| `Barcode` | String | 1-100 chars, valid barcode format |
| `ReferenceNumber` | String | System-generated reference format |
| `PhoneNumber` | String | Valid phone number format |

**Numeric Wrappers:**
| Primitive | Wraps | Validation Rules |
|-----------|-------|------------------|
| `Quantity` | BigDecimal | Non-negative, max 2 decimal places |
| `RequiredQuantity` | int | Positive integer |

**Typed Entity IDs:**
| Primitive | Wraps | Purpose |
|-----------|-------|---------|
| `StationId` | UUID | Prevent mixing station IDs with other entity IDs |
| `ApparatusId` | UUID | Prevent mixing apparatus IDs |
| `EquipmentItemId` | UUID | Prevent mixing equipment IDs |
| `CompartmentId` | UUID | Prevent mixing compartment IDs |
| `UserId` | UUID | Prevent mixing user IDs |
| `InventoryCheckId` | UUID | Prevent mixing check IDs |
| `IssueId` | UUID | Prevent mixing issue IDs |

### Vaadin Integration

For UI binding, create converters:

```java
public class SerialNumberConverter implements Converter<String, SerialNumber> {
    public static final SerialNumberConverter INSTANCE = new SerialNumberConverter();

    @Override
    public Result<SerialNumber> convertToModel(String value, ValueContext context) {
        if (value == null || value.isBlank()) {
            return Result.ok(null);
        }
        try {
            return Result.ok(new SerialNumber(value));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public String convertToPresentation(SerialNumber serialNumber, ValueContext context) {
        return serialNumber == null ? "" : serialNumber.toString();
    }
}
```

### jOOQ Integration

Create custom jOOQ converters to map between domain primitives and database columns:

```java
public class SerialNumberBinding implements Binding<String, SerialNumber> {
    // Implementation maps SerialNumber <-> VARCHAR
}
```

## Consequences

### Positive
- **Compile-time safety**: Impossible to pass wrong type of ID or string to a method
- **Self-documenting code**: Method signatures clearly communicate expected types
- **Centralized validation**: Business rules enforced in one place
- **Reduced bugs**: Parameter order mistakes caught by compiler
- **Defense in depth**: Complements database constraints with application-level validation

### Negative
- **More classes**: Each domain primitive requires its own class
- **Conversion overhead**: Need converters for UI binding and persistence
- **Learning curve**: Team must understand the pattern

### Neutral
- **jOOQ generated POJOs**: Will still use primitive types; domain primitives used in domain/service layer

## References
- [Vaadin: Domain Primitives](https://vaadin.com/docs/latest/building-apps/deep-dives/application-layer/domain-primitives)
- [Secure by Design](https://www.manning.com/books/secure-by-design) by Dan Bergh Johnsson, Daniel Deogun, Daniel Sawano
