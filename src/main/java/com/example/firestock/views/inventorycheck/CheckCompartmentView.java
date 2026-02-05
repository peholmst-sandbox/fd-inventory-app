package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.inventorycheck.CheckableItem;
import com.example.firestock.inventorycheck.CheckableItemWithStatus;
import com.example.firestock.inventorycheck.ItemVerificationRequest;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.MainLayout;
import com.example.firestock.views.inventorycheck.components.CheckProgressBar;
import com.example.firestock.views.inventorycheck.components.ConsumableItemCard;
import com.example.firestock.views.inventorycheck.components.EquipmentItemCard;
import com.example.firestock.views.inventorycheck.dialogs.MarkAsDamagedDialog;
import com.example.firestock.views.inventorycheck.dialogs.MarkAsMissingDialog;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;

/**
 * Check Compartment View - Fourth screen in the inventory check flow.
 * Main item verification screen with Present/Missing/Damaged actions.
 *
 * <p>Features:
 * <ul>
 *   <li>Header with back button and apparatus/compartment name</li>
 *   <li>Progress bar showing items checked</li>
 *   <li>Scrollable list of items (unchecked first, then checked)</li>
 *   <li>Equipment items with Present/Missing/Damaged buttons</li>
 *   <li>Consumable items with quantity stepper</li>
 *   <li>Compartment lock release on detach</li>
 * </ul>
 *
 * <p>Route: /inventory-check/check/{checkId}/compartment/{compartmentId}
 */
@Route(value = "inventory-check/check/:checkId/compartment/:compartmentId", layout = MainLayout.class)
@PageTitle("Check Compartment | FireStock")
@PermitAll
public class CheckCompartmentView extends AbstractCheckCompartmentView {

    // ==================== Static Navigation Helpers ====================

    /**
     * Navigates to this view for the specified check and compartment.
     *
     * @param checkId the inventory check ID
     * @param compartmentId the compartment to check
     */
    public static void showView(InventoryCheckId checkId, CompartmentId compartmentId) {
        UI.getCurrent().navigate(CheckCompartmentView.class,
            new RouteParameters(
                new RouteParam(PARAM_CHECK_ID, checkId.toString()),
                new RouteParam(PARAM_COMPARTMENT_ID, compartmentId.toString())
            ));
    }

    // ==================== Constructor ====================

    public CheckCompartmentView(ShiftInventoryCheckService inventoryCheckService) {
        super(inventoryCheckService);
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // TODO: Register with broadcaster for real-time updates (Step 7)
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Release compartment lock
        if (checkId != null && compartmentId != null) {
            FirestockUserDetails user = getCurrentUser();
            inventoryCheckService.stopCheckingCompartment(checkId, compartmentId, user.getUserId());
        }
        // TODO: Unregister from broadcaster (Step 7)
    }

    // ==================== Template Method Implementations ====================

    @Override
    protected Component createItemCard(CheckableItemWithStatus itemWithStatus) {
        CheckableItem item = itemWithStatus.item();

        if (item.isConsumable()) {
            return new ConsumableItemCard(itemWithStatus, this::handleConsumableVerification);
        } else {
            return new EquipmentItemCard(
                itemWithStatus,
                this::handlePresentClick,
                this::handleMissingClick,
                this::handleDamagedClick
            );
        }
    }

    // ==================== Item Verification Handlers ====================

    private void handlePresentClick(CheckableItem item) {
        verifyItem(item, VerificationStatus.PRESENT, null, null);
    }

    private void handleMissingClick(CheckableItem item) {
        MarkAsMissingDialog dialog = new MarkAsMissingDialog(item, (missingItem, notes) -> {
            verifyItem(missingItem, VerificationStatus.MISSING, notes, null);
        });
        dialog.open();
    }

    private void handleDamagedClick(CheckableItem item) {
        MarkAsDamagedDialog dialog = new MarkAsDamagedDialog(item, (damagedItem, notes) -> {
            verifyItem(damagedItem, VerificationStatus.PRESENT_DAMAGED, notes, null);
        });
        dialog.open();
    }

    private void handleConsumableVerification(CheckableItem item, BigDecimal actualQuantity) {
        VerificationStatus status = VerificationStatus.PRESENT;
        BigDecimal expected = item.requiredQuantity() != null ? item.requiredQuantity() : BigDecimal.ZERO;

        if (actualQuantity.compareTo(expected) != 0) {
            status = VerificationStatus.LOW_QUANTITY;
        }

        // Get notes from the card if there's a discrepancy
        String notes = null;
        String itemKey = getItemKey(item);
        Component card = itemCards.get(itemKey);
        if (card instanceof ConsumableItemCard consumableCard) {
            notes = consumableCard.getNotes();
        }

        verifyItem(item, status, notes, actualQuantity);
    }

    private void verifyItem(CheckableItem item, VerificationStatus status, String notes, BigDecimal quantity) {
        FirestockUserDetails user = getCurrentUser();

        try {
            ItemVerificationRequest request = new ItemVerificationRequest(
                checkId,
                item.equipmentItemId(),
                item.consumableStockId(),
                compartmentId,
                null, // manifestEntryId
                status,
                notes,
                quantity != null ? new Quantity(quantity) : null,
                item.requiredQuantity() != null ? new Quantity(item.requiredQuantity()) : null
            );

            inventoryCheckService.verifyItem(request, user.getUserId());

            // Update UI
            updateItemCard(item, status);
            updateProgress();

            // Show success notification
            String statusText = switch (status) {
                case PRESENT -> "marked as present";
                case MISSING -> "marked as missing";
                case PRESENT_DAMAGED -> "marked as damaged";
                case LOW_QUANTITY -> "quantity verified";
                default -> "verified";
            };
            Notification.show(item.name() + " " + statusText, 2000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Move card to bottom of list
            moveCardToBottom(item);

        } catch (ShiftInventoryCheckService.ItemAlreadyVerifiedException e) {
            Notification.show("This item has already been verified", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
        } catch (ShiftInventoryCheckService.QuantityDiscrepancyRequiresNotesException e) {
            Notification.show("Notes are required for quantity discrepancies greater than 20%",
                    3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show("Error verifying item: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateItemCard(CheckableItem item, VerificationStatus status) {
        String itemKey = getItemKey(item);
        Component card = itemCards.get(itemKey);

        if (card instanceof EquipmentItemCard equipmentCard) {
            equipmentCard.updateStatus(status);
        } else if (card instanceof ConsumableItemCard consumableCard) {
            consumableCard.updateStatus(status);
        }
    }

    private void updateProgress() {
        if (progressBar != null && itemList != null) {
            long verifiedCount = itemCards.values().stream()
                .filter(card -> {
                    if (card instanceof EquipmentItemCard ec) {
                        return ec.getItemWithStatus().isVerified() ||
                               ec.getElement().getClassList().contains("verified") ||
                               ec.getElement().getClassList().contains("issue");
                    } else if (card instanceof ConsumableItemCard cc) {
                        return cc.getItemWithStatus().isVerified() ||
                               cc.getElement().getClassList().contains("verified") ||
                               cc.getElement().getClassList().contains("issue");
                    }
                    return false;
                })
                .count();

            int totalItems = itemCards.size();
            content.getChildren()
                .filter(c -> c instanceof CheckProgressBar)
                .forEach(content::remove);

            progressBar = new CheckProgressBar((int) verifiedCount + 1, totalItems, "items");
            content.addComponentAsFirst(progressBar);
        }
    }

    private void moveCardToBottom(CheckableItem item) {
        String itemKey = getItemKey(item);
        Component card = itemCards.get(itemKey);

        if (card != null && itemList != null) {
            itemList.remove(card);
            itemList.add(card);
        }
    }
}
