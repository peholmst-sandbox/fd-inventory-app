package com.example.firestock.views.inventorycheck;

import com.example.firestock.inventorycheck.ActiveCheckInfo;
import com.example.firestock.inventorycheck.ApparatusWithCheckStatus;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.MainLayout;
import com.example.firestock.views.inventorycheck.components.ApparatusCard;
import com.example.firestock.views.inventorycheck.components.ResumeBanner;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Select Apparatus View - First screen in the inventory check flow.
 * Shows the station's apparatus list with resume capability for in-progress checks.
 *
 * <p>Features:
 * <ul>
 *   <li>Header with station name and back button</li>
 *   <li>Resume banner for in-progress checks (visible to all station users)</li>
 *   <li>Apparatus cards showing unit number, last check date, and current checkers</li>
 *   <li>Sorted by last check date (oldest first to prioritize overdue checks)</li>
 * </ul>
 */
@Route(value = "inventory-check", layout = MainLayout.class)
@PageTitle("Inventory Check | FireStock")
@PermitAll
public class SelectApparatusView extends Div {

    private final ShiftInventoryCheckService inventoryCheckService;
    private final VerticalLayout content;
    private Span headerTitle;
    private Div cardList;

    public SelectApparatusView(ShiftInventoryCheckService inventoryCheckService) {
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

        // Load data
        loadContent();
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("ic-header");

        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassName("ic-touch-btn");
        backButton.addClickListener(e -> navigateToHome());

        headerTitle = new Span("Inventory Check");
        headerTitle.addClassName("ic-header-title");

        header.add(backButton, headerTitle);
        return header;
    }

    private void updateHeaderWithStationName(String stationName) {
        if (headerTitle != null && stationName != null && !stationName.isBlank()) {
            headerTitle.setText(stationName + " Inventory Check");
        }
    }

    private void loadContent() {
        content.removeAll();

        FirestockUserDetails user = getCurrentUser();
        if (user.getPrimaryStationId() == null) {
            showNoStationMessage();
            return;
        }

        // Load apparatus list first to get station name
        List<ApparatusWithCheckStatus> apparatusList = inventoryCheckService
            .getApparatusWithCheckStatus(user.getPrimaryStationId());

        // Update header with station name if available
        if (!apparatusList.isEmpty()) {
            updateHeaderWithStationName(apparatusList.get(0).stationName());
        }

        // Check for active check to show resume banner
        Optional<ActiveCheckInfo> activeCheck = inventoryCheckService
            .getActiveCheckForStation(user.getPrimaryStationId());

        if (activeCheck.isPresent()) {
            ResumeBanner resumeBanner = new ResumeBanner(activeCheck.get());
            resumeBanner.addResumeClickListener(e ->
                navigateToCompartmentSelection(activeCheck.get().apparatusId().toString())
            );
            content.add(resumeBanner);
        }

        // Section title
        H2 sectionTitle = new H2("Select Apparatus");
        sectionTitle.addClassName("ic-section-title");
        content.add(sectionTitle);

        if (apparatusList.isEmpty()) {
            showNoApparatusMessage();
            return;
        }

        // Sort: oldest check first (never checked at top), then by date ascending
        List<ApparatusWithCheckStatus> sortedList = apparatusList.stream()
            .sorted(Comparator.comparing(
                ApparatusWithCheckStatus::lastCheckDate,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ))
            .toList();

        // Create card list
        cardList = new Div();
        cardList.addClassName("ic-card-list");

        for (ApparatusWithCheckStatus apparatus : sortedList) {
            ApparatusCard card = new ApparatusCard(apparatus);
            card.addCardClickListener(e ->
                navigateToCompartmentSelection(apparatus.id().toString())
            );
            cardList.add(card);
        }

        content.add(cardList);
    }

    private void showNoStationMessage() {
        Div messageCard = new Div();
        messageCard.addClassName("warning-banner");

        Span icon = new Span();
        icon.addClassName("warning-banner-icon");
        icon.setText("\u26A0");

        Span text = new Span("You are not assigned to any station. Contact your administrator.");
        text.addClassName("warning-banner-text");

        messageCard.add(icon, text);
        content.add(messageCard);
    }

    private void showNoApparatusMessage() {
        Div messageCard = new Div();
        messageCard.getStyle()
            .set("text-align", "center")
            .set("padding", "var(--lumo-space-xl)")
            .set("color", "var(--lumo-secondary-text-color)");

        Span text = new Span("No apparatus assigned to your station.");
        messageCard.add(text);
        content.add(messageCard);
    }

    private void navigateToHome() {
        getUI().ifPresent(ui -> ui.navigate(""));
    }

    private void navigateToCompartmentSelection(String apparatusId) {
        // Navigate to the compartment selection view (Step 3)
        // Route: /inventory-check/apparatus/{apparatusId}/compartments
        getUI().ifPresent(ui ->
            ui.navigate("inventory-check/apparatus/" + apparatusId + "/compartments")
        );
    }

    private FirestockUserDetails getCurrentUser() {
        return (FirestockUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }

}
