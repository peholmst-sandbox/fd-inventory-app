package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import org.jooq.DSLContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static com.example.firestock.jooq.Tables.APPARATUS;
import static com.example.firestock.jooq.Tables.INVENTORY_CHECK;

/**
 * Evaluates station-based access control for the current user.
 * Used both in SpEL expressions via {@code @stationAccess} and programmatically.
 *
 * <p>Access rules per NFR-04:
 * <ul>
 *   <li>Firefighters can only access their assigned stations</li>
 *   <li>Maintenance Technicians have cross-station access</li>
 *   <li>System Administrators have cross-station access</li>
 * </ul>
 */
@Component("stationAccess")
public class StationAccessEvaluator {

    private final DSLContext create;

    public StationAccessEvaluator(DSLContext create) {
        this.create = create;
    }

    /**
     * Checks if the current user can access the specified station.
     * Intended for use in SpEL expressions: {@code @PreAuthorize("@stationAccess.canAccessStation(#stationId)")}
     *
     * @param stationId the station to check access for
     * @return true if access is allowed
     */
    public boolean canAccessStation(StationId stationId) {
        FirestockUserDetails userDetails = getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }
        return userDetails.hasAccessToStation(stationId);
    }

    /**
     * Checks if the current user can access an apparatus (by looking up its station).
     *
     * @param apparatusId the apparatus to check access for
     * @return true if access is allowed
     */
    public boolean canAccessApparatus(ApparatusId apparatusId) {
        StationId stationId = getStationIdForApparatus(apparatusId);
        return stationId != null && canAccessStation(stationId);
    }

    /**
     * Checks if the current user can access an inventory check (by looking up its station).
     *
     * @param checkId the inventory check to check access for
     * @return true if access is allowed
     */
    public boolean canAccessInventoryCheck(InventoryCheckId checkId) {
        StationId stationId = getStationIdForInventoryCheck(checkId);
        return stationId != null && canAccessStation(stationId);
    }

    /**
     * Requires the current user to have access to the specified station.
     * Throws AccessDeniedException if access is denied.
     *
     * @param stationId the station to check access for
     * @throws AccessDeniedException if the user does not have access
     */
    public void requireStationAccess(StationId stationId) {
        if (!canAccessStation(stationId)) {
            throw new AccessDeniedException("Access denied to station: " + stationId);
        }
    }

    /**
     * Requires the current user to have access to the apparatus.
     * Throws AccessDeniedException if access is denied.
     *
     * @param apparatusId the apparatus to check access for
     * @throws AccessDeniedException if the user does not have access
     */
    public void requireApparatusAccess(ApparatusId apparatusId) {
        if (!canAccessApparatus(apparatusId)) {
            throw new AccessDeniedException("Access denied to apparatus: " + apparatusId);
        }
    }

    /**
     * Requires the current user to have access to the inventory check.
     * Throws AccessDeniedException if access is denied.
     *
     * @param checkId the inventory check to check access for
     * @throws AccessDeniedException if the user does not have access
     */
    public void requireInventoryCheckAccess(InventoryCheckId checkId) {
        if (!canAccessInventoryCheck(checkId)) {
            throw new AccessDeniedException("Access denied to inventory check: " + checkId);
        }
    }

    private FirestockUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof FirestockUserDetails) {
            return (FirestockUserDetails) principal;
        }
        return null;
    }

    private StationId getStationIdForApparatus(ApparatusId apparatusId) {
        return create.select(APPARATUS.STATION_ID)
            .from(APPARATUS)
            .where(APPARATUS.ID.eq(apparatusId))
            .fetchOptional(APPARATUS.STATION_ID)
            .orElse(null);
    }

    private StationId getStationIdForInventoryCheck(InventoryCheckId checkId) {
        return create.select(INVENTORY_CHECK.STATION_ID)
            .from(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.ID.eq(checkId))
            .fetchOptional(INVENTORY_CHECK.STATION_ID)
            .orElse(null);
    }
}
