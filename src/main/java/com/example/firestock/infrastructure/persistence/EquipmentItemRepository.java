package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.equipment.EquipmentItem;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.infrastructure.persistence.mapper.EquipmentItemMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.EQUIPMENT_ITEM;

/**
 * Repository for {@link EquipmentItem} persistence.
 *
 * <p>Provides basic CRUD operations for equipment items. This repository focuses
 * on operations needed for inventory checks and status updates. Additional
 * query capabilities can be added as needed.
 */
@Repository
public class EquipmentItemRepository {

    private final DSLContext create;
    private final EquipmentItemMapper mapper;

    public EquipmentItemRepository(DSLContext create, EquipmentItemMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    /**
     * Finds an equipment item by its ID.
     *
     * @param id the equipment item ID
     * @return the equipment item, or empty if not found
     */
    public Optional<EquipmentItem> findById(EquipmentItemId id) {
        return create.selectFrom(EQUIPMENT_ITEM)
                .where(EQUIPMENT_ITEM.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Saves an equipment item (insert or update).
     *
     * @param item the equipment item to save
     * @return the saved equipment item
     */
    public EquipmentItem save(EquipmentItem item) {
        var record = create.newRecord(EQUIPMENT_ITEM);
        mapper.updateRecord(record, item);

        if (existsById(item.id())) {
            record.setUpdatedAt(Instant.now());
            record.update();
        } else {
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            record.insert();
        }

        return item;
    }

    /**
     * Checks if an equipment item with the given ID exists.
     *
     * @param id the equipment item ID
     * @return true if the item exists
     */
    public boolean existsById(EquipmentItemId id) {
        return create.fetchExists(
                create.selectFrom(EQUIPMENT_ITEM)
                        .where(EQUIPMENT_ITEM.ID.eq(id))
        );
    }
}
