package com.example.firestock.views.audit;

import com.example.firestock.audit.ApparatusAuditInfo;
import com.example.firestock.audit.FormalAuditService;
import com.example.firestock.views.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * View for selecting an apparatus to perform a formal audit.
 * Displays apparatus cards with unit number, last audit date, and active audit status.
 * Restricted to maintenance technicians.
 */
@Route(value = "audit", layout = MainLayout.class)
@PageTitle("Formal Audits | FireStock")
@RolesAllowed("MAINTENANCE_TECHNICIAN")
public class AuditApparatusSelectionView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final FormalAuditService auditService;

    public AuditApparatusSelectionView(FormalAuditService auditService) {
        this.auditService = auditService;

        addClassName("audit-selection-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Formal Audits");
        header.addClassName("view-header");

        Span subtitle = new Span("Select an apparatus to perform a comprehensive equipment audit");
        subtitle.addClassName("text-secondary");

        add(header, subtitle);

        VerticalLayout cardsContainer = new VerticalLayout();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        cardsContainer.addClassName("apparatus-cards");

        List<ApparatusAuditInfo> apparatusList = getApparatusForCurrentUser();

        if (apparatusList.isEmpty()) {
            Span noApparatus = new Span("No apparatus available for auditing.");
            noApparatus.addClassName("text-secondary");
            cardsContainer.add(noApparatus);
        } else {
            apparatusList.forEach(apparatus ->
                    cardsContainer.add(createApparatusCard(apparatus))
            );
        }

        add(cardsContainer);
    }

    private List<ApparatusAuditInfo> getApparatusForCurrentUser() {
        try {
            // Maintenance technicians have cross-station access, so show all apparatus
            return auditService.getAllApparatus();
        } catch (Exception e) {
            Notification.show("Error loading apparatus: " + e.getMessage(),
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return List.of();
        }
    }

    private Div createApparatusCard(ApparatusAuditInfo apparatus) {
        Div card = new Div();
        card.addClassName("apparatus-card");

        Icon truckIcon = VaadinIcon.TRUCK.create();
        truckIcon.addClassName("apparatus-icon");

        H3 unitNumber = new H3(apparatus.unitNumber().value());
        unitNumber.addClassName("apparatus-unit");

        Span stationInfo = new Span(apparatus.stationName());
        stationInfo.addClassName("apparatus-station");

        Span typeInfo = new Span(apparatus.type().getLiteral());
        typeInfo.addClassName("apparatus-type");
        typeInfo.addClassName("text-secondary");

        VerticalLayout textContent = new VerticalLayout(unitNumber, stationInfo, typeInfo);
        textContent.setPadding(false);
        textContent.setSpacing(false);

        HorizontalLayout mainContent = new HorizontalLayout(truckIcon, textContent);
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setSpacing(true);

        // Last audit date
        Span lastAuditLabel = new Span("Last audit: ");
        lastAuditLabel.addClassName("text-secondary");

        String lastAuditDateText = apparatus.lastAuditDate() != null
                ? LocalDateTime.ofInstant(apparatus.lastAuditDate(), ZoneId.systemDefault()).format(DATE_FORMATTER)
                : "Never";
        Span lastAuditDate = new Span(lastAuditDateText);
        lastAuditDate.addClassName("last-audit-date");

        // Add warning if audit is due
        if (apparatus.isAuditDue()) {
            lastAuditDate.addClassName("audit-overdue");
        }

        HorizontalLayout lastAuditInfo = new HorizontalLayout(lastAuditLabel, lastAuditDate);
        lastAuditInfo.setSpacing(false);

        // Status badge
        Div statusBadge = new Div();
        if (apparatus.hasActiveAudit()) {
            statusBadge.setText("In Progress");
            statusBadge.addClassName("status-badge");
            statusBadge.addClassName("status-in-progress");
        } else if (apparatus.isAuditDue()) {
            statusBadge.setText("Audit Due");
            statusBadge.addClassName("status-badge");
            statusBadge.addClassName("status-due");
        }

        VerticalLayout cardContent = new VerticalLayout(mainContent, lastAuditInfo);
        if (statusBadge.getText() != null && !statusBadge.getText().isEmpty()) {
            cardContent.add(statusBadge);
        }
        cardContent.setPadding(false);
        cardContent.setSpacing(true);

        card.add(cardContent);

        card.addClickListener(e ->
                card.getUI().ifPresent(ui ->
                        ui.navigate(FormalAuditView.class, apparatus.id().toString())
                )
        );

        return card;
    }
}
