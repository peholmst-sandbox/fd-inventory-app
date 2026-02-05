package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.inventorycheck.InventoryCheckSummary;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService.IncompleteCheckException;
import com.example.firestock.views.MainLayout;
import com.example.firestock.views.inventorycheck.dialogs.ConfirmAbandonDialog;
import com.example.firestock.views.inventorycheck.dialogs.ConfirmCompleteDialog;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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

import java.util.UUID;

/**
 * View Summary View - Summary dashboard with complete/abandon options.
 *
 * <p>Features:
 * <ul>
 *   <li>Header with back button and title</li>
 *   <li>2x2 stats grid showing: Total Items, Present, Issues, Remaining</li>
 *   <li>Footer with Abandon Check and Complete Check buttons</li>
 * </ul>
 *
 * <p>Route: /inventory-check/check/{checkId}/summary
 */
@Route(value = "inventory-check/check/:checkId/summary", layout = MainLayout.class)
@PageTitle("Check Summary | FireStock")
@PermitAll
public class ViewSummaryView extends Div implements BeforeEnterObserver {

    // Route parameter names
    private static final String PARAM_CHECK_ID = "checkId";

    private final ShiftInventoryCheckService inventoryCheckService;
    private final VerticalLayout content;
    private Span headerTitle;
    private Div footer;
    private Button completeButton;

    private InventoryCheckId checkId;
    private InventoryCheckSummary checkSummary;

    // ==================== Static Navigation Helpers ====================

    /**
     * Navigates to this view for the specified inventory check.
     *
     * @param checkId the inventory check to show summary for
     */
    public static void showView(InventoryCheckId checkId) {
        UI.getCurrent().navigate(ViewSummaryView.class,
            new RouteParameters(new RouteParam(PARAM_CHECK_ID, checkId.toString())));
    }

    // ==================== Constructor ====================

    public ViewSummaryView(ShiftInventoryCheckService inventoryCheckService) {
        this.inventoryCheckService = inventoryCheckService;

        addClassName("inventory-check-base");

        // Header
        Div header = createHeader();

        // Scrollable content area
        content = new VerticalLayout();
        content.addClassName("ic-content");
        content.setPadding(false);
        content.setSpacing(false);

        // Footer
        footer = createFooter();

        add(header, content, footer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var checkIdParam = event.getRouteParameters().get(PARAM_CHECK_ID);

        if (checkIdParam.isEmpty()) {
            navigateToSelectApparatus();
            return;
        }

        try {
            this.checkId = new InventoryCheckId(UUID.fromString(checkIdParam.get()));
            loadContent();
        } catch (IllegalArgumentException e) {
            Notification.show("Invalid check ID", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            navigateToSelectApparatus();
        }
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("ic-header");

        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassName("ic-touch-btn");
        backButton.addClickListener(e -> navigateBack());

        headerTitle = new Span("Check Summary");
        headerTitle.addClassName("ic-header-title");

        header.add(backButton, headerTitle);
        return header;
    }

    private Div createFooter() {
        Div footerDiv = new Div();
        footerDiv.addClassName("ic-footer");

        Button abandonButton = new Button("Abandon Check", VaadinIcon.TRASH.create());
        abandonButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        abandonButton.addClassName("ic-touch-btn");
        abandonButton.addClickListener(e -> showAbandonDialog());

        completeButton = new Button("Complete Check", VaadinIcon.CHECK.create());
        completeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        completeButton.addClassName("ic-touch-btn");
        completeButton.addClickListener(e -> showCompleteDialog());

        footerDiv.add(abandonButton, completeButton);
        return footerDiv;
    }

    private void loadContent() {
        content.removeAll();

        try {
            // Get check summary
            checkSummary = inventoryCheckService.getCheck(checkId);

            // Update header
            headerTitle.setText("Check Summary");

            // Section title
            H2 sectionTitle = new H2("Inventory Check Summary");
            sectionTitle.addClassName("ic-section-title");
            content.add(sectionTitle);

            // Stats grid
            Div statsGrid = createStatsGrid();
            content.add(statsGrid);

            // Update complete button state based on whether all items are verified
            updateCompleteButtonState();

        } catch (Exception e) {
            showErrorMessage("Unable to load check summary: " + e.getMessage());
        }
    }

    private Div createStatsGrid() {
        Div grid = new Div();
        grid.addClassName("ic-stats-grid");

        // Calculate stats
        int total = checkSummary.totalItems();
        int verified = checkSummary.verifiedCount();
        int issues = checkSummary.issuesFoundCount();
        int present = verified - issues; // Items verified without issues
        int remaining = total - verified;

        // Total Items
        grid.add(createStatCard("Total Items", total, "ic-stat-total"));

        // Present (verified without issues)
        grid.add(createStatCard("Present", present, "ic-stat-present"));

        // Issues
        grid.add(createStatCard("Issues", issues, "ic-stat-issues"));

        // Remaining
        grid.add(createStatCard("Remaining", remaining, "ic-stat-remaining"));

        return grid;
    }

    private Div createStatCard(String label, int value, String additionalClass) {
        Div card = new Div();
        card.addClassNames("ic-stat-card", additionalClass);

        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.addClassName("ic-stat-value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("ic-stat-label");

        card.add(valueSpan, labelSpan);
        return card;
    }

    private void updateCompleteButtonState() {
        boolean canComplete = checkSummary.isComplete();
        completeButton.setEnabled(canComplete);

        if (!canComplete) {
            int remaining = checkSummary.totalItems() - checkSummary.verifiedCount();
            completeButton.setTooltipText(
                String.format("%d items remaining to verify", remaining));
        } else {
            completeButton.setTooltipText(null);
        }
    }

    private void showAbandonDialog() {
        ConfirmAbandonDialog dialog = new ConfirmAbandonDialog(this::handleAbandon);
        dialog.open();
    }

    private void showCompleteDialog() {
        if (!checkSummary.isComplete()) {
            Notification.show("Not all items have been verified", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        ConfirmCompleteDialog dialog = new ConfirmCompleteDialog(
            checkSummary.verifiedCount(),
            checkSummary.issuesFoundCount(),
            this::handleComplete
        );
        dialog.open();
    }

    private void handleAbandon() {
        try {
            inventoryCheckService.abandonCheck(checkId);

            Notification.show("Inventory check abandoned", 3000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);

            navigateToSelectApparatus();

        } catch (Exception e) {
            Notification.show("Failed to abandon check: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void handleComplete() {
        try {
            inventoryCheckService.completeCheck(checkId);

            Notification.show("Inventory check completed successfully!",
                    3000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            navigateToSelectApparatus();

        } catch (IncompleteCheckException e) {
            Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            // Refresh to update the UI
            loadContent();

        } catch (Exception e) {
            Notification.show("Failed to complete check: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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

    private void navigateBack() {
        // Navigate back to Select Compartment view
        if (checkSummary != null) {
            SelectCompartmentView.showView(checkSummary.apparatusId());
        } else {
            navigateToSelectApparatus();
        }
    }

    private void navigateToSelectApparatus() {
        getUI().ifPresent(ui -> ui.navigate(SelectApparatusView.class));
    }
}
