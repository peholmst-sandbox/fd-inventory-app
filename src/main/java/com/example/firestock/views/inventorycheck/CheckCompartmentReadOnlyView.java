package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.inventorycheck.CheckableItem;
import com.example.firestock.inventorycheck.CheckableItemWithStatus;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.MainLayout;
import com.example.firestock.views.inventorycheck.broadcast.InventoryCheckBroadcaster;
import com.example.firestock.views.inventorycheck.broadcast.InventoryCheckBroadcaster.CheckCompletedEvent;
import com.example.firestock.views.inventorycheck.broadcast.InventoryCheckBroadcaster.CompartmentLockChangedEvent;
import com.example.firestock.views.inventorycheck.broadcast.InventoryCheckBroadcaster.InventoryCheckEvent;
import com.example.firestock.views.inventorycheck.broadcast.InventoryCheckBroadcaster.ItemVerifiedEvent;
import com.example.firestock.views.inventorycheck.dialogs.ConfirmTakeOverDialog;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;

/**
 * Check Compartment Read-Only View - Fifth screen in the inventory check flow.
 * Displays compartment being checked by another user with take-over option.
 *
 * <p>Features:
 * <ul>
 *   <li>Header with back button and apparatus/compartment name</li>
 *   <li>Warning banner showing who is checking the compartment</li>
 *   <li>Progress bar showing items checked</li>
 *   <li>Scrollable list of items (read-only, no action buttons)</li>
 *   <li>Footer with BACK and TAKE OVER buttons</li>
 *   <li>Real-time updates as the other user checks items</li>
 * </ul>
 *
 * <p>Route: /inventory-check/check/{checkId}/compartment/{compartmentId}/readonly
 */
@Route(value = "inventory-check/check/:checkId/compartment/:compartmentId/readonly", layout = MainLayout.class)
@PageTitle("Check Compartment (Read Only) | FireStock")
@PermitAll
public class CheckCompartmentReadOnlyView extends AbstractCheckCompartmentView {

    private final InventoryCheckBroadcaster broadcaster;
    private Registration broadcasterRegistration;
    private String currentCheckerName;
    private Span warningText;

    // ==================== Static Navigation Helpers ====================

    /**
     * Navigates to this view for the specified check and compartment.
     *
     * @param checkId the inventory check ID
     * @param compartmentId the compartment to view (read-only)
     */
    public static void showView(InventoryCheckId checkId, CompartmentId compartmentId) {
        UI.getCurrent().navigate(CheckCompartmentReadOnlyView.class,
            new RouteParameters(
                new RouteParam(PARAM_CHECK_ID, checkId.toString()),
                new RouteParam(PARAM_COMPARTMENT_ID, compartmentId.toString())
            ));
    }

    // ==================== Constructor ====================

    public CheckCompartmentReadOnlyView(ShiftInventoryCheckService inventoryCheckService,
                                        InventoryCheckBroadcaster broadcaster) {
        super(inventoryCheckService);
        this.broadcaster = broadcaster;
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Register with broadcaster for real-time updates
        if (apparatusDetails != null) {
            UI ui = attachEvent.getUI();
            broadcasterRegistration = broadcaster.register(apparatusDetails.id(), event -> {
                ui.access(() -> handleBroadcastEvent(event));
            });
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Unregister from broadcaster
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    // ==================== Template Method Implementations ====================

    @Override
    protected Component createFooter() {
        Div footer = new Div();
        footer.addClassName("ic-footer");
        footer.getStyle().set("display", "flex").set("gap", "var(--lumo-space-m)");

        Button backButton = new Button("BACK", VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassNames("ic-touch-btn", "ic-touch-btn-large");
        backButton.addClickListener(e -> navigateBack());

        Button takeOverButton = new Button("TAKE OVER", VaadinIcon.HAND.create());
        takeOverButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        takeOverButton.addClassNames("ic-touch-btn", "ic-touch-btn-large");
        takeOverButton.addClickListener(e -> handleTakeOverClick());

        footer.add(backButton, takeOverButton);
        return footer;
    }

    @Override
    protected Component createAdditionalHeaderContent() {
        // Get who is currently checking this compartment
        currentCheckerName = inventoryCheckService.getCompartmentCheckerName(checkId, compartmentId)
            .orElse("Another user");

        return createWarningBanner();
    }

    @Override
    protected Component createItemCard(CheckableItemWithStatus itemWithStatus) {
        return createReadOnlyItemCard(itemWithStatus);
    }

    // ==================== Read-Only Item Card ====================

    private Component createReadOnlyItemCard(CheckableItemWithStatus itemWithStatus) {
        CheckableItem item = itemWithStatus.item();

        Div card = new Div();
        card.addClassName("ic-item-card");
        card.addClassName("readonly");

        // Apply state-based styling
        if (itemWithStatus.isVerified()) {
            card.addClassName("verified");
            if (itemWithStatus.hasIssue()) {
                card.removeClassName("verified");
                card.addClassName("issue");
            }
        }

        // Header with name
        Div header = new Div();
        header.addClassName("ic-item-header");
        Span nameSpan = new Span(item.name());
        nameSpan.addClassName("ic-item-name");
        header.add(nameSpan);

        // Type name
        String typeName = item.isConsumable() ? "Consumable" : item.typeName();
        Span typeSpan = new Span(typeName);
        typeSpan.addClassName("ic-item-type");

        card.add(header, typeSpan);

        // Serial number (for equipment)
        if (!item.isConsumable() && item.serialNumber() != null) {
            Span serialSpan = new Span("S/N: " + item.serialNumber().value());
            serialSpan.addClassName("ic-item-serial");
            card.add(serialSpan);
        }

        // Quantity (for consumables)
        if (item.isConsumable()) {
            BigDecimal expected = item.requiredQuantity() != null ? item.requiredQuantity() : BigDecimal.ZERO;
            BigDecimal actual = item.currentQuantity() != null ? item.currentQuantity().value() : expected;

            Div quantityDiv = new Div();
            quantityDiv.getStyle().set("margin", "var(--lumo-space-xs) 0");
            quantityDiv.add(new Span("Expected: " + formatQuantity(expected) + " | Actual: " + formatQuantity(actual)));
            card.add(quantityDiv);
        }

        // Status badge
        Span statusBadge = createStatusBadge(itemWithStatus.verificationStatus(), item.isConsumable());
        card.add(statusBadge);

        // Verified by info (if verified)
        if (itemWithStatus.isVerified() && itemWithStatus.verifiedByName() != null) {
            Span verifiedBySpan = new Span("Verified by " + itemWithStatus.verifiedByName());
            verifiedBySpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
            card.add(verifiedBySpan);
        }

        return card;
    }

    // ==================== Warning Banner ====================

    private Div createWarningBanner() {
        Div warningBanner = new Div();
        warningBanner.addClassName("warning-banner");
        warningBanner.getStyle()
            .set("background-color", "var(--lumo-warning-color-10pct)")
            .set("border", "1px solid var(--lumo-warning-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "var(--lumo-space-m)")
            .set("margin-bottom", "var(--lumo-space-m)")
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "var(--lumo-space-s)");

        Span icon = new Span();
        icon.addClassName("warning-banner-icon");
        icon.setText("\u26A0");
        icon.getStyle().set("font-size", "var(--lumo-font-size-xl)");

        warningText = new Span("This compartment is being checked by " + currentCheckerName);
        warningText.addClassName("warning-banner-text");

        warningBanner.add(icon, warningText);
        return warningBanner;
    }

    // ==================== Take Over Handling ====================

    private void handleTakeOverClick() {
        // Refresh the current checker name before showing dialog
        currentCheckerName = inventoryCheckService.getCompartmentCheckerName(checkId, compartmentId)
            .orElse("Another user");

        ConfirmTakeOverDialog dialog = new ConfirmTakeOverDialog(currentCheckerName, this::performTakeOver);
        dialog.open();
    }

    private void performTakeOver() {
        FirestockUserDetails user = getCurrentUser();

        try {
            // Take over the compartment (service broadcasts CheckTakeOverEvent)
            String previousChecker = inventoryCheckService.takeOverCompartment(checkId, compartmentId, user.getUserId());

            // Show notification
            if (previousChecker != null) {
                Notification.show("Took over from " + previousChecker, 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            // Navigate to editable Check Compartment view
            CheckCompartmentView.showView(checkId, compartmentId);

        } catch (Exception e) {
            Notification.show("Unable to take over: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ==================== Real-time Update Handling ====================

    private void handleBroadcastEvent(InventoryCheckEvent event) {
        if (event instanceof ItemVerifiedEvent itemEvent) {
            if (itemEvent.compartmentId().equals(compartmentId)) {
                // Reload content to reflect the update
                loadContent();
            }
        } else if (event instanceof CompartmentLockChangedEvent lockEvent) {
            if (lockEvent.compartmentId().equals(compartmentId)) {
                if (!lockEvent.isLocked()) {
                    // Compartment was unlocked - the other user left
                    Notification.show("Compartment is now available", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    // Navigate to editable view since we can now check it
                    CheckCompartmentView.showView(checkId, compartmentId);
                } else {
                    // Update checker name
                    currentCheckerName = lockEvent.lockedByName();
                    if (warningText != null) {
                        warningText.setText("This compartment is being checked by " + currentCheckerName);
                    }
                }
            }
        } else if (event instanceof CheckCompletedEvent) {
            // Check was completed, navigate back to apparatus selection
            Notification.show("Inventory check completed", 3000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            navigateToSelectApparatus();
        }
    }
}
