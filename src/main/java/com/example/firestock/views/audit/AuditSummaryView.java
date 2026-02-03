package com.example.firestock.views.audit;

import com.example.firestock.audit.AuditDetails;
import com.example.firestock.audit.FormalAuditService;
import com.example.firestock.domain.audit.AuditException;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.AuditStatus;
import com.example.firestock.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.AccessDeniedException;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Summary view for a formal audit showing completion status,
 * issues found, and options to complete or continue the audit.
 * Restricted to maintenance technicians.
 */
@Route(value = "audit", layout = MainLayout.class)
@PageTitle("Audit Summary | FireStock")
@RolesAllowed("MAINTENANCE_TECHNICIAN")
public class AuditSummaryView extends VerticalLayout implements HasUrlParameter<String> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final FormalAuditService auditService;

    private ApparatusId apparatusId;
    private FormalAuditId auditId;
    private AuditDetails auditDetails;

    public AuditSummaryView(FormalAuditService auditService) {
        this.auditService = auditService;

        addClassName("audit-summary-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        // Expected format: {apparatusId}/summary/{auditId}
        String[] parts = parameter.split("/");
        if (parts.length < 3 || !"summary".equals(parts[1])) {
            showErrorAndNavigateBack("Invalid URL format");
            return;
        }

        try {
            this.apparatusId = new ApparatusId(UUID.fromString(parts[0]));
            this.auditId = new FormalAuditId(UUID.fromString(parts[2]));
        } catch (IllegalArgumentException e) {
            showErrorAndNavigateBack("Invalid ID format");
            return;
        }

        try {
            this.auditDetails = auditService.getAuditDetails(auditId);
            buildUI();
        } catch (AccessDeniedException e) {
            showErrorAndNavigateBack("You don't have access to this audit");
        } catch (IllegalArgumentException e) {
            showErrorAndNavigateBack("Audit not found");
        }
    }

    private void showErrorAndNavigateBack(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        getUI().ifPresent(ui -> ui.navigate(AuditApparatusSelectionView.class));
    }

    private void buildUI() {
        removeAll();

        add(createHeader());
        add(createStatusSection());
        add(createStatisticsSection());
        add(createCompartmentsSummary());
        add(createActions());
    }

    private HorizontalLayout createHeader() {
        Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(FormalAuditView.class, apparatusId.toString()))
        );

        H2 title = new H2("Audit Summary");

        HorizontalLayout header = new HorizontalLayout(backButton, title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setWidthFull();

        return header;
    }

    private VerticalLayout createStatusSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);
        section.addClassName("status-section");

        H3 unitNumber = new H3(auditDetails.unitNumber().value());
        unitNumber.addClassName("unit-number");

        Span statusBadge = new Span(auditDetails.status().getLiteral());
        statusBadge.addClassName("status-badge");
        statusBadge.addClassName("status-" + auditDetails.status().getLiteral().toLowerCase().replace("_", "-"));

        Span startedAt = new Span("Started: " + auditDetails.startedAt().format(DATE_TIME_FORMATTER));
        startedAt.addClassName("text-secondary");

        if (auditDetails.completedAt() != null) {
            Span completedAt = new Span("Completed: " + auditDetails.completedAt().format(DATE_TIME_FORMATTER));
            completedAt.addClassName("text-secondary");
            section.add(unitNumber, statusBadge, startedAt, completedAt);
        } else {
            section.add(unitNumber, statusBadge, startedAt);
        }

        // Stale warning
        var summary = auditService.getAudit(auditId);
        if (summary.isStale()) {
            Span staleWarning = new Span("This audit is more than 7 days old and should be completed or abandoned.");
            staleWarning.addClassName("stale-warning");
            section.add(staleWarning);
        }

        return section;
    }

    private HorizontalLayout createStatisticsSection() {
        HorizontalLayout stats = new HorizontalLayout();
        stats.setWidthFull();
        stats.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
        stats.addClassName("stats-section");

        stats.add(createStatCard("Total Items", String.valueOf(auditDetails.totalItems()), VaadinIcon.LIST));
        stats.add(createStatCard("Audited", String.valueOf(auditDetails.auditedCount()), VaadinIcon.CHECK));
        stats.add(createStatCard("Issues Found", String.valueOf(auditDetails.issuesFoundCount()), VaadinIcon.EXCLAMATION_CIRCLE));
        stats.add(createStatCard("Unexpected Items", String.valueOf(auditDetails.unexpectedItemsCount()), VaadinIcon.QUESTION_CIRCLE));

        return stats;
    }

    private Div createStatCard(String label, String value, VaadinIcon icon) {
        Div card = new Div();
        card.addClassName("stat-card");

        Icon iconElement = icon.create();
        iconElement.addClassName("stat-icon");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("stat-value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");

        VerticalLayout content = new VerticalLayout(iconElement, valueSpan, labelSpan);
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);

        card.add(content);
        return card;
    }

    private VerticalLayout createCompartmentsSummary() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H3 header = new H3("Compartments");
        section.add(header);

        for (var compartment : auditDetails.compartments()) {
            Div compartmentCard = new Div();
            compartmentCard.addClassName("compartment-summary-card");

            Span name = new Span(compartment.name());
            name.addClassName("compartment-name");

            Span progress = new Span(String.format("%d / %d items",
                    compartment.auditedItems(), compartment.totalItems()));
            progress.addClassName("compartment-progress");

            Icon statusIcon;
            if (compartment.isComplete()) {
                statusIcon = VaadinIcon.CHECK_CIRCLE.create();
                statusIcon.addClassName("status-complete");
            } else {
                statusIcon = VaadinIcon.CLOCK.create();
                statusIcon.addClassName("status-incomplete");
            }

            int issues = compartment.issuesCount();
            Span issuesSpan = new Span(issues > 0 ? issues + " issues" : "No issues");
            issuesSpan.addClassName(issues > 0 ? "has-issues" : "no-issues");

            HorizontalLayout cardContent = new HorizontalLayout(statusIcon, name, progress, issuesSpan);
            cardContent.setAlignItems(FlexComponent.Alignment.CENTER);
            cardContent.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            cardContent.setWidthFull();

            compartmentCard.add(cardContent);
            section.add(compartmentCard);
        }

        return section;
    }

    private HorizontalLayout createActions() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        actions.setPadding(true);
        actions.setSpacing(true);

        if (auditDetails.status() == AuditStatus.IN_PROGRESS) {
            Button continueButton = new Button("Continue Audit", new Icon(VaadinIcon.ARROW_RIGHT));
            continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            continueButton.addClickListener(e ->
                    getUI().ifPresent(ui -> ui.navigate(FormalAuditView.class, apparatusId.toString()))
            );

            Button completeButton = new Button("Complete Audit", new Icon(VaadinIcon.CHECK));
            completeButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            completeButton.setEnabled(auditDetails.isAllAudited());
            completeButton.addClickListener(e -> completeAudit());

            if (!auditDetails.isAllAudited()) {
                int remaining = auditDetails.totalItems() - auditDetails.auditedCount();
                completeButton.setTooltipText(remaining + " items remaining to audit");
            }

            Button abandonButton = new Button("Abandon Audit", new Icon(VaadinIcon.TRASH));
            abandonButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            abandonButton.addClickListener(e -> abandonAudit());

            actions.add(continueButton, completeButton, abandonButton);
        } else {
            // Audit is completed or abandoned - just show back button
            Button backButton = new Button("Back to Audits", new Icon(VaadinIcon.ARROW_LEFT));
            backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            backButton.addClickListener(e ->
                    getUI().ifPresent(ui -> ui.navigate(AuditApparatusSelectionView.class))
            );
            actions.add(backButton);
        }

        return actions;
    }

    private void completeAudit() {
        try {
            auditService.completeAudit(auditId);
            Notification.show("Audit completed successfully!",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            // Refresh the view
            this.auditDetails = auditService.getAuditDetails(auditId);
            buildUI();
        } catch (AuditException.IncompleteAuditException e) {
            Notification.show("Cannot complete: " + e.getMessage(),
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show("Error completing audit: " + e.getMessage(),
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void abandonAudit() {
        try {
            auditService.abandonAudit(auditId);
            Notification.show("Audit abandoned.",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            getUI().ifPresent(ui -> ui.navigate(AuditApparatusSelectionView.class));
        } catch (Exception e) {
            Notification.show("Error abandoning audit: " + e.getMessage(),
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
