package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.inventorycheck.ApparatusDetails;
import com.example.firestock.inventorycheck.CheckableItem;
import com.example.firestock.inventorycheck.CheckableItemWithStatus;
import com.example.firestock.inventorycheck.CompartmentWithItems;
import com.example.firestock.inventorycheck.InventoryCheckSummary;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.inventorycheck.components.CheckProgressBar;
import com.vaadin.flow.component.Component;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for Check Compartment views (editable and read-only).
 * Contains shared logic for displaying compartment items during inventory checks.
 *
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #createItemCard(CheckableItemWithStatus)} - How to render each item</li>
 *   <li>{@link #getItemCardKey(CheckableItemWithStatus)} - Unique key for item tracking</li>
 * </ul>
 *
 * <p>Subclasses may override:
 * <ul>
 *   <li>{@link #createFooter()} - Optional footer component</li>
 *   <li>{@link #createAdditionalHeaderContent()} - Content between header and progress bar</li>
 *   <li>{@link #onContentLoaded()} - Hook after content is loaded</li>
 * </ul>
 */
public abstract class AbstractCheckCompartmentView extends Div implements BeforeEnterObserver {

    // Route parameter names
    protected static final String PARAM_CHECK_ID = "checkId";
    protected static final String PARAM_COMPARTMENT_ID = "compartmentId";

    protected final ShiftInventoryCheckService inventoryCheckService;
    protected final VerticalLayout content;
    protected final Map<String, Component> itemCards = new HashMap<>();

    private Span headerTitle;
    protected CheckProgressBar progressBar;
    protected Div itemList;

    protected InventoryCheckId checkId;
    protected CompartmentId compartmentId;
    protected ApparatusDetails apparatusDetails;
    protected CompartmentWithItems compartment;

    protected AbstractCheckCompartmentView(ShiftInventoryCheckService inventoryCheckService) {
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

        // Optional footer
        Component footer = createFooter();
        if (footer != null) {
            add(footer);
        }
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

    // ==================== Template Methods ====================

    /**
     * Creates a card component for displaying an item.
     * Subclasses implement this to provide editable or read-only item cards.
     *
     * @param itemWithStatus the item with its verification status
     * @return the card component
     */
    protected abstract Component createItemCard(CheckableItemWithStatus itemWithStatus);

    /**
     * Gets the unique key for tracking an item card.
     *
     * @param itemWithStatus the item
     * @return unique key string
     */
    protected String getItemCardKey(CheckableItemWithStatus itemWithStatus) {
        return getItemKey(itemWithStatus.item());
    }

    /**
     * Creates an optional footer component.
     * Default implementation returns null (no footer).
     *
     * @return the footer component, or null
     */
    protected Component createFooter() {
        return null;
    }

    /**
     * Creates optional content to display between header and progress bar.
     * Default implementation returns null.
     *
     * @return the additional content, or null
     */
    protected Component createAdditionalHeaderContent() {
        return null;
    }

    /**
     * Hook called after content is successfully loaded.
     * Default implementation does nothing.
     */
    protected void onContentLoaded() {
        // Override in subclasses if needed
    }

    // ==================== Shared Implementation ====================

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

    protected void updateHeader(String apparatusName, String compartmentName) {
        if (headerTitle != null) {
            headerTitle.setText(apparatusName + " - " + compartmentName);
        }
    }

    protected void loadContent() {
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

            // Additional header content (e.g., warning banner)
            Component additionalContent = createAdditionalHeaderContent();
            if (additionalContent != null) {
                content.add(additionalContent);
            }

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
                String itemKey = getItemCardKey(itemWithStatus);
                itemCards.put(itemKey, card);
                itemList.add(card);
            }

            content.add(itemList);

            // Notify subclass
            onContentLoaded();

        } catch (Exception e) {
            showErrorMessage("Unable to load items: " + e.getMessage());
        }
    }

    // ==================== Shared Utility Methods ====================

    protected String getItemKey(CheckableItem item) {
        if (item.equipmentItemId() != null) {
            return "eq:" + item.equipmentItemId().toString();
        } else {
            return "cs:" + item.consumableStockId().toString();
        }
    }

    protected Span createStatusBadge(VerificationStatus status, boolean isConsumable) {
        Span badge = new Span();
        badge.addClassName("status-badge");

        if (status == null) {
            badge.setText("NOT CHECKED");
            badge.addClassName("status-unchecked");
        } else {
            switch (status) {
                case PRESENT -> {
                    badge.setText(isConsumable ? "OK" : "PRESENT");
                    badge.addClassName("status-present");
                }
                case MISSING -> {
                    badge.setText("MISSING");
                    badge.addClassName("status-missing");
                }
                case PRESENT_DAMAGED -> {
                    badge.setText("DAMAGED");
                    badge.addClassName("status-damaged");
                }
                case EXPIRED -> {
                    badge.setText("EXPIRED");
                    badge.addClassName("status-damaged");
                }
                case LOW_QUANTITY -> {
                    badge.setText("DISCREPANCY");
                    badge.addClassName("status-damaged");
                }
                case SKIPPED -> {
                    badge.setText("SKIPPED");
                    badge.addClassName("status-unchecked");
                }
            }
        }

        return badge;
    }

    protected String formatQuantity(BigDecimal qty) {
        if (qty == null) return "0";
        return qty.stripTrailingZeros().toPlainString();
    }

    protected void showNoItemsMessage() {
        Div messageCard = new Div();
        messageCard.getStyle()
            .set("text-align", "center")
            .set("padding", "var(--lumo-space-xl)")
            .set("color", "var(--lumo-secondary-text-color)");

        Span text = new Span("No items found in this compartment.");
        messageCard.add(text);
        content.add(messageCard);
    }

    protected void showErrorMessage(String message) {
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

    protected void navigateBack() {
        if (apparatusDetails != null) {
            SelectCompartmentView.showView(apparatusDetails.id());
        } else {
            navigateToSelectApparatus();
        }
    }

    protected void navigateToSelectApparatus() {
        getUI().ifPresent(ui -> ui.navigate(SelectApparatusView.class));
    }

    protected FirestockUserDetails getCurrentUser() {
        return (FirestockUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
