package com.example.firestock.views.inventorycheck;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Home view for selecting an apparatus to perform inventory check.
 * Displays apparatus cards with unit number, station, and last check date.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Select Apparatus | FireStock")
public class ApparatusSelectionView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    public ApparatusSelectionView() {
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

        getMockApparatusData().forEach(apparatus ->
                cardsContainer.add(createApparatusCard(apparatus))
        );

        add(cardsContainer);
    }

    private Div createApparatusCard(ApparatusData apparatus) {
        Div card = new Div();
        card.addClassName("apparatus-card");

        Icon truckIcon = VaadinIcon.TRUCK.create();
        truckIcon.addClassName("apparatus-icon");

        H3 unitNumber = new H3(apparatus.unitNumber());
        unitNumber.addClassName("apparatus-unit");

        Span stationInfo = new Span(apparatus.station());
        stationInfo.addClassName("apparatus-station");

        VerticalLayout textContent = new VerticalLayout(unitNumber, stationInfo);
        textContent.setPadding(false);
        textContent.setSpacing(false);

        HorizontalLayout mainContent = new HorizontalLayout(truckIcon, textContent);
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setSpacing(true);

        Span lastCheckLabel = new Span("Last check: ");
        lastCheckLabel.addClassName("text-secondary");

        Span lastCheckDate = new Span(apparatus.lastCheckDate().format(DATE_FORMATTER));
        lastCheckDate.addClassName("last-check-date");

        HorizontalLayout lastCheckInfo = new HorizontalLayout(lastCheckLabel, lastCheckDate);
        lastCheckInfo.setSpacing(false);

        VerticalLayout cardContent = new VerticalLayout(mainContent, lastCheckInfo);
        cardContent.setPadding(false);
        cardContent.setSpacing(true);

        card.add(cardContent);

        card.addClickListener(e ->
                card.getUI().ifPresent(ui ->
                        ui.navigate(InventoryCheckView.class, apparatus.id())
                )
        );

        return card;
    }

    private List<ApparatusData> getMockApparatusData() {
        return List.of(
                new ApparatusData("1", "Engine 1", "Station 1 - Downtown", LocalDate.now().minusDays(1)),
                new ApparatusData("2", "Ladder 2", "Station 2 - Westside", LocalDate.now().minusDays(2)),
                new ApparatusData("3", "Rescue 3", "Station 3 - Industrial", LocalDate.now().minusDays(3))
        );
    }

    private record ApparatusData(String id, String unitNumber, String station, LocalDate lastCheckDate) {}
}
