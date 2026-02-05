package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.inventorycheck.ApparatusDetails;
import com.example.firestock.inventorycheck.CheckableItem;
import com.example.firestock.inventorycheck.CheckableItemWithStatus;
import com.example.firestock.inventorycheck.CompartmentWithItems;
import com.example.firestock.inventorycheck.InventoryCheckSummary;
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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
public class CheckCompartmentView extends Div implements BeforeEnterObserver {

    // Route parameter names
    private static final String PARAM_CHECK_ID = "checkId";
    private static final String PARAM_COMPARTMENT_ID = "compartmentId";

    private final ShiftInventoryCheckService inventoryCheckService;
    private final VerticalLayout content;
    private Span headerTitle;
    private CheckProgressBar progressBar;
    private Div itemList;

    private InventoryCheckId checkId;
    private CompartmentId compartmentId;
    private ApparatusDetails apparatusDetails;
    private CompartmentWithItems compartment;
    private final Map<String, Component> itemCards = new HashMap<>();

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
        this.inventoryCheckService = inventoryCheckService;

        addClassName("inventory-check-base");

        // Header
        Div header = createHeader();

        // Scrollable content area
        content = new VerticalLayout();
        content.addClassName("ic-content");
        content.setPadding(false);
        content.setSpacing(false);

        add(header, content);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var checkIdParam = event.getRouteParameters().get(PARAM_CHECK_ID);
        var compartmentIdParam = event.getRouteParameters().get(PARAM_COMPARTMENT_ID);

        if (checkIdParam.isEmpty() || compartmentIdParam.isEmpty()) {
            navigateToSelectApparatus();
            return;
        }

        try {
            this.checkId = new InventoryCheckId(UUID.fromString(checkIdParam.get()));
            this.compartmentId = new CompartmentId(UUID.fromString(compartmentIdParam.get()));
            loadContent();
        } catch (IllegalArgumentException e) {
            Notification.show("Invalid parameters", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            navigateToSelectApparatus();
        }
    }

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

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("ic-header");

        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassName("ic-touch-btn");
        backButton.addClickListener(e -> navigateBack());

        headerTitle = new Span("Check Compartment");
        headerTitle.addClassName("ic-header-title");

        header.add(backButton, headerTitle);
        return header;
    }

    private void updateHeader(String apparatusName, String compartmentName) {
        if (headerTitle != null) {
            headerTitle.setText(apparatusName + " - " + compartmentName);
        }
    }

    private void loadContent() {
        content.removeAll();
        itemCards.clear();

        try {
            // Get check summary to find apparatus ID
            InventoryCheckSummary checkSummary = inventoryCheckService.getCheck(checkId);

            // Get apparatus details
            apparatusDetails = inventoryCheckService.getApparatusDetails(checkSummary.apparatusId());

            // Find the compartment
            compartment = apparatusDetails.compartments().stream()
                .filter(c -> c.id().equals(compartmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Compartment not found"));

            // Update header
            updateHeader(apparatusDetails.unitNumber().value(), compartment.name());

            // Get items with status
            List<CheckableItemWithStatus> items = inventoryCheckService.getItemsWithStatus(checkId, compartmentId);

            // Calculate progress
            long verifiedCount = items.stream().filter(CheckableItemWithStatus::isVerified).count();
            int totalItems = items.size();

            // Progress section
            progressBar = new CheckProgressBar((int) verifiedCount, totalItems, "items");
            content.add(progressBar);

            if (items.isEmpty()) {
                showNoItemsMessage();
                return;
            }

            // Create item list
            itemList = new Div();
            itemList.addClassName("ic-card-list");

            for (CheckableItemWithStatus itemWithStatus : items) {
                Component card = createItemCard(itemWithStatus);
                String itemKey = getItemKey(itemWithStatus.item());
                itemCards.put(itemKey, card);
                itemList.add(card);
            }

            content.add(itemList);

        } catch (Exception e) {
            showErrorMessage("Unable to load items: " + e.getMessage());
        }
    }

    private Component createItemCard(CheckableItemWithStatus itemWithStatus) {
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

    private String getItemKey(CheckableItem item) {
        if (item.equipmentItemId() != null) {
            return "eq:" + item.equipmentItemId().toString();
        } else {
            return "cs:" + item.consumableStockId().toString();
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
        // Get expected quantity
        BigDecimal expected = item.requiredQuantity() != null ? item.requiredQuantity() : BigDecimal.ZERO;

        // Determine status based on quantity discrepancy
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
            // Count verified items
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

            // Reload the progress bar with new counts
            // Note: For a simpler approach, we just reload the content
            // For better UX, we should update the progress bar component
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

    // ==================== UI Helpers ====================

    private void showNoItemsMessage() {
        Div messageCard = new Div();
        messageCard.getStyle()
            .set("text-align", "center")
            .set("padding", "var(--lumo-space-xl)")
            .set("color", "var(--lumo-secondary-text-color)");

        Span text = new Span("No items found in this compartment.");
        messageCard.add(text);
        content.add(messageCard);
    }

    private void showErrorMessage(String message) {
        Div messageCard = new Div();
        messageCard.addClassName("warning-banner");

        Span icon = new Span();
        icon.addClassName("warning-banner-icon");
        icon.setText("\u26A0");

        Span text = new Span(message);
        text.addClassName("warning-banner-text");

        messageCard.add(icon, text);
        content.add(messageCard);
    }

    // ==================== Navigation ====================

    private void navigateBack() {
        // Navigate back to Select Compartment view
        if (checkId != null && apparatusDetails != null) {
            SelectCompartmentView.showView(apparatusDetails.id());
        } else {
            navigateToSelectApparatus();
        }
    }

    private void navigateToSelectApparatus() {
        getUI().ifPresent(ui -> ui.navigate(SelectApparatusView.class));
    }

    private FirestockUserDetails getCurrentUser() {
        return (FirestockUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
