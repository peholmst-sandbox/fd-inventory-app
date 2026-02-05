package com.example.firestock.views.inventorycheck.broadcast;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Broadcaster for real-time inventory check updates.
 * Enables collaboration between multiple users checking the same apparatus.
 *
 * <p>This component manages event subscriptions per apparatus and broadcasts
 * events to all registered listeners when inventory check state changes occur.
 *
 * <p>Events are broadcast asynchronously on a single-threaded executor to avoid
 * blocking the calling thread.
 */
@Component
public class InventoryCheckBroadcaster {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Map<ApparatusId, Set<Consumer<InventoryCheckEvent>>> listeners =
        new ConcurrentHashMap<>();

    /**
     * Registers a listener for events related to a specific apparatus.
     *
     * @param apparatusId the apparatus to receive events for
     * @param listener the event handler
     * @return a registration that can be used to unregister the listener
     */
    public synchronized Registration register(
            ApparatusId apparatusId, Consumer<InventoryCheckEvent> listener) {
        listeners.computeIfAbsent(apparatusId, k -> ConcurrentHashMap.newKeySet())
            .add(listener);
        return () -> {
            Set<Consumer<InventoryCheckEvent>> set = listeners.get(apparatusId);
            if (set != null) {
                set.remove(listener);
                if (set.isEmpty()) {
                    listeners.remove(apparatusId);
                }
            }
        };
    }

    /**
     * Broadcasts an event to all listeners subscribed to the event's apparatus.
     *
     * @param event the event to broadcast
     */
    public void broadcast(InventoryCheckEvent event) {
        Set<Consumer<InventoryCheckEvent>> apparatusListeners = listeners.get(event.apparatusId());
        if (apparatusListeners != null) {
            for (Consumer<InventoryCheckEvent> listener : apparatusListeners) {
                executor.execute(() -> listener.accept(event));
            }
        }
    }

    // ==================== Event Definitions ====================

    /**
     * Base interface for all inventory check events.
     * All events are scoped to a specific apparatus.
     */
    public sealed interface InventoryCheckEvent permits
            ItemVerifiedEvent,
            CompartmentLockChangedEvent,
            CheckTakeOverEvent,
            CheckCompletedEvent {

        /**
         * Gets the apparatus this event relates to.
         *
         * @return the apparatus ID
         */
        ApparatusId apparatusId();
    }

    /**
     * Event broadcast when an item is verified during an inventory check.
     *
     * @param apparatusId the apparatus being checked
     * @param compartmentId the compartment containing the verified item
     * @param itemId the equipment item ID or consumable stock ID as string
     * @param status the verification status assigned to the item
     * @param verifiedByName the display name of the user who verified the item
     */
    public record ItemVerifiedEvent(
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        String itemId,
        VerificationStatus status,
        String verifiedByName
    ) implements InventoryCheckEvent {}

    /**
     * Event broadcast when a compartment lock is acquired or released.
     *
     * @param apparatusId the apparatus being checked
     * @param compartmentId the compartment whose lock changed
     * @param lockedByName the display name of the user holding the lock, or null if unlocked
     * @param isLocked true if the compartment is now locked, false if unlocked
     */
    public record CompartmentLockChangedEvent(
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        String lockedByName,
        boolean isLocked
    ) implements InventoryCheckEvent {}

    /**
     * Event broadcast when a user takes over a compartment from another user.
     *
     * @param apparatusId the apparatus being checked
     * @param compartmentId the compartment that was taken over
     * @param previousCheckerName the display name of the user who was checking
     * @param newCheckerName the display name of the user who took over
     */
    public record CheckTakeOverEvent(
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        String previousCheckerName,
        String newCheckerName
    ) implements InventoryCheckEvent {}

    /**
     * Event broadcast when an inventory check is completed.
     *
     * @param apparatusId the apparatus that was checked
     * @param checkId the completed inventory check ID
     */
    public record CheckCompletedEvent(
        ApparatusId apparatusId,
        InventoryCheckId checkId
    ) implements InventoryCheckEvent {}
}
