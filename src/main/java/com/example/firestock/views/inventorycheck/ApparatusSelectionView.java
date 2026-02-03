package com.example.firestock.views.inventorycheck;

import com.example.firestock.inventorycheck.ApparatusSummary;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Home view for selecting an apparatus to perform inventory check.
 * Displays apparatus cards with unit number, station, and last check date.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Select Apparatus | FireStock")
@PermitAll
public class ApparatusSelectionView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final ShiftInventoryCheckService inventoryCheckService;

    public ApparatusSelectionView(ShiftInventoryCheckService inventoryCheckService) {
        this.inventoryCheckService = inventoryCheckService;

        addClassName("apparatus-selection-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Select Apparatus");
        header.addClassName("view-header");

        Span subtitle = new Span("Choose an apparatus to begin shift inventory check");
        subtitle.addClassName("text-secondary");

        add(header, subtitle);

        VerticalLayout cardsContainer = new VerticalLayout();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        cardsContainer.addClassName("apparatus-cards");

        List<ApparatusSummary> apparatusList = getApparatusForCurrentUser();

        if (apparatusList.isEmpty()) {
            Span noApparatus = new Span("No apparatus assigned to your station.");
            noApparatus.addClassName("text-secondary");
            cardsContainer.add(noApparatus);
        } else {
            apparatusList.forEach(apparatus ->
                    cardsContainer.add(createApparatusCard(apparatus))
            );
        }

        add(cardsContainer);
    }

    private List<ApparatusSummary> getApparatusForCurrentUser() {
        FirestockUserDetails user = (FirestockUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        var primaryStationId = user.getPrimaryStationId();
        if (primaryStationId == null) {
            return List.of();
        }

        return inventoryCheckService.getApparatusForStation(primaryStationId);
    }

    private Div createApparatusCard(ApparatusSummary apparatus) {
        Div card = new Div();
        card.addClassName("apparatus-card");

        Icon truckIcon = VaadinIcon.TRUCK.create();
        truckIcon.addClassName("apparatus-icon");

        H3 unitNumber = new H3(apparatus.unitNumber().value());
        unitNumber.addClassName("apparatus-unit");

        Span stationInfo = new Span(apparatus.stationName());
        stationInfo.addClassName("apparatus-station");

        VerticalLayout textContent = new VerticalLayout(unitNumber, stationInfo);
        textContent.setPadding(false);
        textContent.setSpacing(false);

        HorizontalLayout mainContent = new HorizontalLayout(truckIcon, textContent);
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setSpacing(true);

        Span lastCheckLabel = new Span("Last check: ");
        lastCheckLabel.addClassName("text-secondary");

        String lastCheckDateText = apparatus.lastCheckDate() != null
                ? apparatus.lastCheckDate().format(DATE_FORMATTER)
                : "Never";
        Span lastCheckDate = new Span(lastCheckDateText);
        lastCheckDate.addClassName("last-check-date");

        HorizontalLayout lastCheckInfo = new HorizontalLayout(lastCheckLabel, lastCheckDate);
        lastCheckInfo.setSpacing(false);

        VerticalLayout cardContent = new VerticalLayout(mainContent, lastCheckInfo);
        cardContent.setPadding(false);
        cardContent.setSpacing(true);

        card.add(cardContent);

        card.addClickListener(e ->
                card.getUI().ifPresent(ui ->
                        ui.navigate(InventoryCheckView.class, apparatus.id().toString())
                )
        );

        return card;
    }
}
