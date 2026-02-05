package com.example.firestock.views.inventorycheck.components;

import com.example.firestock.inventorycheck.ApparatusWithCheckStatus;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Card component displaying apparatus information with check status.
 * Shows unit number, station, last check date, and current checkers (if any).
 */
public class ApparatusCard extends Div {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final ApparatusWithCheckStatus apparatus;

    public ApparatusCard(ApparatusWithCheckStatus apparatus) {
        this.apparatus = apparatus;

        addClassName("apparatus-card");

        // Add visual indicator for active check
        if (apparatus.hasActiveCheck()) {
            addClassName("apparatus-card-active");
        }

        // Main content layout
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        // Top row: Icon + Unit number + Station
        HorizontalLayout mainRow = createMainRow();

        // Middle row: Last check info
        HorizontalLayout lastCheckRow = createLastCheckRow();

        content.add(mainRow, lastCheckRow);

        // Bottom row: Current checkers (if any)
        if (apparatus.hasActiveCheck() && !apparatus.currentCheckerNames().isEmpty()) {
            Div checkersRow = createCheckersRow();
            content.add(checkersRow);
        }

        add(content);
    }

    private HorizontalLayout createMainRow() {
        Icon truckIcon = VaadinIcon.TRUCK.create();
        truckIcon.addClassName("apparatus-icon");

        H3 unitNumber = new H3(apparatus.unitNumber().value());
        unitNumber.addClassName("apparatus-unit");
        unitNumber.getStyle().set("margin", "0");

        Span stationInfo = new Span(apparatus.stationName());
        stationInfo.addClassName("apparatus-station");

        VerticalLayout textContent = new VerticalLayout(unitNumber, stationInfo);
        textContent.setPadding(false);
        textContent.setSpacing(false);

        // Status indicator for active check
        HorizontalLayout mainRow = new HorizontalLayout(truckIcon, textContent);
        mainRow.setAlignItems(FlexComponent.Alignment.CENTER);
        mainRow.setSpacing(true);
        mainRow.setWidthFull();

        if (apparatus.hasActiveCheck()) {
            Span activeBadge = new Span("IN PROGRESS");
            activeBadge.addClassNames("status-badge", "status-active-check");
            activeBadge.getStyle()
                .set("background", "var(--firestock-primary-bg)")
                .set("color", "var(--firestock-primary)")
                .set("margin-left", "auto")
                .set("margin-top", "0");
            mainRow.add(activeBadge);
        }

        return mainRow;
    }

    private HorizontalLayout createLastCheckRow() {
        Span lastCheckLabel = new Span("Last check: ");
        lastCheckLabel.addClassName("text-secondary");
        lastCheckLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");

        String lastCheckText = formatLastCheckDate(apparatus.lastCheckDate());
        Span lastCheckDate = new Span(lastCheckText);
        lastCheckDate.addClassName("last-check-date");
        lastCheckDate.getStyle().set("font-size", "var(--lumo-font-size-s)");

        // Color code based on recency
        if (apparatus.lastCheckDate() != null) {
            long daysSinceCheck = ChronoUnit.DAYS.between(
                apparatus.lastCheckDate().toLocalDate(),
                LocalDateTime.now().toLocalDate()
            );
            if (daysSinceCheck == 0) {
                lastCheckDate.getStyle().set("color", "var(--firestock-present)");
            } else if (daysSinceCheck <= 1) {
                lastCheckDate.getStyle().set("color", "var(--lumo-secondary-text-color)");
            } else {
                lastCheckDate.getStyle().set("color", "var(--firestock-damaged)");
            }
        }

        HorizontalLayout row = new HorizontalLayout(lastCheckLabel, lastCheckDate);
        row.setSpacing(false);
        row.getStyle().set("margin-top", "var(--lumo-space-s)");

        return row;
    }

    private Div createCheckersRow() {
        List<String> names = apparatus.currentCheckerNames();
        String checkersText = String.join(", ", names);

        Icon userIcon = VaadinIcon.USER.create();
        userIcon.addClassName("checker-badge-icon");
        userIcon.getStyle().set("width", "14px").set("height", "14px");

        Span label = new Span("Being checked by: " + checkersText);

        Div checkersDiv = new Div();
        checkersDiv.addClassName("checker-badge");
        checkersDiv.add(userIcon, label);
        checkersDiv.getStyle().set("margin-top", "var(--lumo-space-s)");

        return checkersDiv;
    }

    private String formatLastCheckDate(LocalDateTime lastCheckDate) {
        if (lastCheckDate == null) {
            return "Never";
        }

        LocalDateTime now = LocalDateTime.now();
        long daysBetween = ChronoUnit.DAYS.between(lastCheckDate.toLocalDate(), now.toLocalDate());

        if (daysBetween == 0) {
            return "Today";
        } else if (daysBetween == 1) {
            return "Yesterday";
        } else if (daysBetween < 7) {
            return daysBetween + " days ago";
        } else {
            return lastCheckDate.format(DATE_FORMATTER);
        }
    }

    /**
     * Returns the apparatus data associated with this card.
     */
    public ApparatusWithCheckStatus getApparatus() {
        return apparatus;
    }

    /**
     * Adds a click listener to the card.
     */
    public void addCardClickListener(ComponentEventListener<ClickEvent<Div>> listener) {
        addClickListener(listener);
    }
}
