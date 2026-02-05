package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.inventorycheck.ApparatusDetails;
import com.example.firestock.inventorycheck.CompartmentCheckProgress;
import com.example.firestock.inventorycheck.InventoryCheckSummary;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService.CompartmentLockedException;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.MainLayout;
import com.example.firestock.views.inventorycheck.components.CheckProgressBar;
import com.example.firestock.views.inventorycheck.components.CompartmentCard;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Select Compartment View - Third screen in the inventory check flow.
 * Shows compartments for the selected apparatus with progress tracking.
 *
 * <p>Features:
 * <ul>
 *   <li>Header with apparatus name and back button</li>
 *   <li>Overall progress bar showing compartment completion</li>
 *   <li>Compartment cards showing name, item count, progress, and current checker</li>
 *   <li>Sorted by display order</li>
 *   <li>Footer with View Summary button</li>
 * </ul>
 *
 * <p>Route: /inventory-check/apparatus/{apparatusId}/compartments
 */
@Route(value = "inventory-check/apparatus/:apparatusId/compartments", layout = MainLayout.class)
@PageTitle("Select Compartment | FireStock")
@PermitAll
public class SelectCompartmentView extends Div implements BeforeEnterObserver {

    // Route parameter names
    private static final String PARAM_APPARATUS_ID = "apparatusId";

    private final ShiftInventoryCheckService inventoryCheckService;
    private final VerticalLayout content;
    private Span headerTitle;
    private Div cardList;
    private Div footer;

    private ApparatusId apparatusId;
    private InventoryCheckId checkId;
    private ApparatusDetails apparatusDetails;

    // ==================== Static Navigation Helpers ====================

    /**
     * Navigates to this view for the specified apparatus.
     *
     * @param apparatusId the apparatus to show compartments for
     */
    public static void showView(ApparatusId apparatusId) {
        UI.getCurrent().navigate(SelectCompartmentView.class,
            new RouteParameters(new RouteParam(PARAM_APPARATUS_ID, apparatusId.toString())));
    }

    // ==================== Constructor ====================

    public SelectCompartmentView(ShiftInventoryCheckService inventoryCheckService) {
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
        var apparatusIdParam = event.getRouteParameters().get(PARAM_APPARATUS_ID);

        if (apparatusIdParam.isEmpty()) {
            navigateToSelectApparatus();
            return;
        }

        try {
            this.apparatusId = new ApparatusId(UUID.fromString(apparatusIdParam.get()));
            loadContent();
        } catch (IllegalArgumentException e) {
            Notification.show("Invalid apparatus ID", 3000, Notification.Position.MIDDLE)
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
        backButton.addClickListener(e -> navigateToSelectApparatus());

        headerTitle = new Span("Inventory Check");
        headerTitle.addClassName("ic-header-title");

        header.add(backButton, headerTitle);
        return header;
    }

    private Div createFooter() {
        Div footerDiv = new Div();
        footerDiv.addClassName("ic-footer");

        Button viewSummaryButton = new Button("VIEW SUMMARY", VaadinIcon.LIST.create());
        viewSummaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewSummaryButton.addClassNames("ic-touch-btn", "ic-touch-btn-large");
        viewSummaryButton.addClickListener(e -> navigateToSummary());

        footerDiv.add(viewSummaryButton);
        return footerDiv;
    }

    private void updateHeaderWithApparatusName(String unitNumber) {
        if (headerTitle != null && unitNumber != null && !unitNumber.isBlank()) {
            headerTitle.setText(unitNumber + " Inventory Check");
        }
    }

    private void loadContent() {
        content.removeAll();

        FirestockUserDetails user = getCurrentUser();

        try {
            // Get apparatus details
            apparatusDetails = inventoryCheckService.getApparatusDetails(apparatusId);
            updateHeaderWithApparatusName(apparatusDetails.unitNumber().value());

            // Get or start inventory check for this apparatus
            InventoryCheckSummary activeCheck = inventoryCheckService.getActiveCheck(apparatusId)
                .orElseGet(() -> inventoryCheckService.startCheck(apparatusId, user.getUserId()));

            this.checkId = activeCheck.id();

            // Get compartment progress
            List<CompartmentCheckProgress> compartments = inventoryCheckService.getCompartmentProgress(checkId);

            // Calculate overall progress (compartments fully checked)
            int fullyCheckedCount = (int) compartments.stream()
                .filter(CompartmentCheckProgress::isFullyChecked)
                .count();

            // Section title
            H2 sectionTitle = new H2("Select Compartment");
            sectionTitle.addClassName("ic-section-title");
            content.add(sectionTitle);

            // Progress section
            CheckProgressBar progressBar = new CheckProgressBar(
                fullyCheckedCount,
                compartments.size(),
                "compartments"
            );
            content.add(progressBar);

            if (compartments.isEmpty()) {
                showNoCompartmentsMessage();
                return;
            }

            // Sort compartments by display order
            List<CompartmentCheckProgress> sortedCompartments = compartments.stream()
                .sorted(Comparator.comparingInt(CompartmentCheckProgress::displayOrder))
                .toList();

            // Create card list
            cardList = new Div();
            cardList.addClassName("ic-card-list");

            for (CompartmentCheckProgress compartment : sortedCompartments) {
                CompartmentCard card = new CompartmentCard(compartment);
                card.addCardClickListener(e -> handleCompartmentClick(compartment));
                cardList.add(card);
            }

            content.add(cardList);

        } catch (ShiftInventoryCheckService.ActiveCheckExistsException e) {
            // This shouldn't happen since we're getting the active check first,
            // but handle it gracefully
            Notification.show("An active check already exists", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
        } catch (Exception e) {
            showErrorMessage("Unable to load compartments: " + e.getMessage());
        }
    }

    private void handleCompartmentClick(CompartmentCheckProgress compartment) {
        FirestockUserDetails user = getCurrentUser();
        CompartmentId compartmentId = compartment.id();

        try {
            // Try to acquire lock on the compartment
            inventoryCheckService.startCheckingCompartment(checkId, compartmentId, user.getUserId());

            // Lock acquired - navigate to editable check view
            navigateToCheckCompartment(compartmentId, false);

        } catch (CompartmentLockedException e) {
            // Another user has the lock - navigate to read-only view
            Notification.show("Compartment is being checked by " + e.getLockedByName(),
                    3000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);

            navigateToCheckCompartment(compartmentId, true);
        }
    }

    private void showNoCompartmentsMessage() {
        Div messageCard = new Div();
        messageCard.getStyle()
            .set("text-align", "center")
            .set("padding", "var(--lumo-space-xl)")
            .set("color", "var(--lumo-secondary-text-color)");

        Span text = new Span("No compartments found for this apparatus.");
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

    private void navigateToSelectApparatus() {
        getUI().ifPresent(ui -> ui.navigate(SelectApparatusView.class));
    }

    private void navigateToCheckCompartment(CompartmentId compartmentId, boolean readOnly) {
        // Navigate to Step 4: Check Compartment View (or read-only variant)
        // TODO: When CheckCompartmentView is created, use its static navigation helper:
        // CheckCompartmentView.showView(checkId, compartmentId) or
        // CheckCompartmentReadOnlyView.showView(checkId, compartmentId)
        //
        // Route pattern: /inventory-check/check/:checkId/compartment/:compartmentId
        // or read-only:  /inventory-check/check/:checkId/compartment/:compartmentId/readonly
        String baseRoute = "inventory-check/check/" + checkId.toString() +
            "/compartment/" + compartmentId.toString();
        String route = readOnly ? baseRoute + "/readonly" : baseRoute;
        getUI().ifPresent(ui -> ui.navigate(route));
    }

    private void navigateToSummary() {
        // Navigate to Step 6: View Summary
        // TODO: When ViewSummaryView is created, use its static navigation helper:
        // ViewSummaryView.showView(checkId)
        //
        // Route pattern: /inventory-check/check/:checkId/summary
        String route = "inventory-check/check/" + checkId.toString() + "/summary";
        getUI().ifPresent(ui -> ui.navigate(route));
    }

    private FirestockUserDetails getCurrentUser() {
        return (FirestockUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
