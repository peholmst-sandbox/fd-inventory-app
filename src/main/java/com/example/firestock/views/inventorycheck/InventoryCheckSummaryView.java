package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.inventorycheck.ApparatusDetails;
import com.example.firestock.inventorycheck.InventoryCheckSummary;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.jooq.enums.CheckStatus;
import com.example.firestock.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

/**
 * Summary view showing inventory check results.
 * Displays statistics and allows completing or abandoning the check.
 */
@Route(value = "check/:apparatusId/summary/:checkId", layout = MainLayout.class)
@PageTitle("Check Summary | FireStock")
@PermitAll
public class InventoryCheckSummaryView extends VerticalLayout implements BeforeEnterObserver {

    private final ShiftInventoryCheckService inventoryCheckService;

    private ApparatusId apparatusId;
    private InventoryCheckId checkId;
    private InventoryCheckSummary checkSummary;
    private String apparatusName;

    public InventoryCheckSummaryView(ShiftInventoryCheckService inventoryCheckService) {
        this.inventoryCheckService = inventoryCheckService;

        addClassName("inventory-summary-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        try {
            String apparatusIdParam = event.getRouteParameters().get("apparatusId").orElse("");
            String checkIdParam = event.getRouteParameters().get("checkId").orElse("");

            this.apparatusId = new ApparatusId(UUID.fromString(apparatusIdParam));
            this.checkId = new InventoryCheckId(UUID.fromString(checkIdParam));

            // Load the check summary
            this.checkSummary = inventoryCheckService.getCheck(checkId);

            // Load apparatus details for the name
            ApparatusDetails apparatusDetails = inventoryCheckService.getApparatusDetails(apparatusId);
            this.apparatusName = apparatusDetails.unitNumber().value();

            buildUI();
        } catch (IllegalArgumentException e) {
            showErrorAndNavigateBack("Invalid apparatus or check ID");
        } catch (AccessDeniedException e) {
            showErrorAndNavigateBack("You don't have access to this check");
        }
    }

    private void showErrorAndNavigateBack(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        getUI().ifPresent(ui -> ui.navigate(ApparatusSelectionView.class));
    }

    private void buildUI() {
        removeAll();

        add(createHeader());
        add(createStatisticsSection());
        add(createIssuesSummarySection());
        add(createFooter());
    }

    private HorizontalLayout createHeader() {
        Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassName("back-button");
        backButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(InventoryCheckView.class, apparatusId.toString()))
        );

        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setPadding(false);
        titleSection.setSpacing(false);

        H2 title = new H2("Check Summary");
        title.addClassName("summary-title");

        Span subtitle = new Span(apparatusName);
        subtitle.addClassName("summary-subtitle");

        titleSection.add(title, subtitle);

        HorizontalLayout header = new HorizontalLayout(backButton, titleSection);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("summary-header");
        header.setWidthFull();
        header.setPadding(true);

        return header;
    }

    private HorizontalLayout createStatisticsSection() {
        int total = checkSummary.totalItems();
        int verified = checkSummary.verifiedCount();
        int issues = checkSummary.issuesFoundCount();
        int present = verified - issues;
        int remaining = total - verified;

        Div totalCard = createStatCard("Total Items", String.valueOf(total), "stat-total", VaadinIcon.LIST);
        Div presentCard = createStatCard("Present", String.valueOf(present), "stat-present", VaadinIcon.CHECK);
        Div issuesCard = createStatCard("Issues", String.valueOf(issues), "stat-issues", VaadinIcon.WARNING);
        Div remainingCard = createStatCard("Remaining", String.valueOf(remaining), "stat-remaining", VaadinIcon.HOURGLASS);

        HorizontalLayout statsRow1 = new HorizontalLayout(totalCard, presentCard);
        statsRow1.setWidthFull();
        statsRow1.setSpacing(true);

        HorizontalLayout statsRow2 = new HorizontalLayout(issuesCard, remainingCard);
        statsRow2.setWidthFull();
        statsRow2.setSpacing(true);

        VerticalLayout statsGrid = new VerticalLayout(statsRow1, statsRow2);
        statsGrid.setPadding(true);
        statsGrid.setSpacing(true);
        statsGrid.addClassName("stats-grid");

        HorizontalLayout wrapper = new HorizontalLayout(statsGrid);
        wrapper.setWidthFull();
        return wrapper;
    }

    private Div createStatCard(String label, String value, String cssClass, VaadinIcon iconType) {
        Div card = new Div();
        card.addClassName("stat-card");
        card.addClassName(cssClass);

        Icon icon = iconType.create();
        icon.addClassName("stat-icon");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("stat-value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");

        VerticalLayout content = new VerticalLayout(icon, valueSpan, labelSpan);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setPadding(false);
        content.setSpacing(false);

        card.add(content);
        return card;
    }

    private VerticalLayout createIssuesSummarySection() {
        H3 issuesHeader = new H3("Check Status");
        issuesHeader.addClassName("issues-header");

        VerticalLayout statusContent = new VerticalLayout();
        statusContent.setPadding(false);
        statusContent.setSpacing(true);

        int remaining = checkSummary.totalItems() - checkSummary.verifiedCount();
        int issues = checkSummary.issuesFoundCount();

        if (remaining > 0) {
            Span remainingInfo = new Span(String.format("%d items still need to be verified", remaining));
            remainingInfo.addClassName("status-info");
            remainingInfo.addClassName("status-warning");
            statusContent.add(remainingInfo);
        }

        if (issues > 0) {
            Span issuesInfo = new Span(String.format("%d issues found (missing or damaged items)", issues));
            issuesInfo.addClassName("status-info");
            issuesInfo.addClassName("status-issues");
            statusContent.add(issuesInfo);
        }

        if (remaining == 0 && issues == 0) {
            Span allGood = new Span("All items verified and accounted for!");
            allGood.addClassName("status-info");
            allGood.addClassName("status-success");
            statusContent.add(allGood);
        } else if (remaining == 0 && issues > 0) {
            Span readyWithIssues = new Span("All items verified. Issues have been logged for follow-up.");
            readyWithIssues.addClassName("status-info");
            readyWithIssues.addClassName("status-warning");
            statusContent.add(readyWithIssues);
        }

        VerticalLayout section = new VerticalLayout(issuesHeader, statusContent);
        section.setPadding(true);
        section.setSpacing(true);
        section.addClassName("issues-section");

        return section;
    }

    private HorizontalLayout createFooter() {
        Button continueButton = new Button("Continue Checking", new Icon(VaadinIcon.ARROW_LEFT));
        continueButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        continueButton.addClassName("continue-btn");
        continueButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(InventoryCheckView.class, apparatusId.toString()))
        );

        Button abandonButton = new Button("Abandon Check", new Icon(VaadinIcon.TRASH));
        abandonButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        abandonButton.addClassName("abandon-btn");
        abandonButton.addClickListener(e -> showAbandonConfirmation());

        Button completeButton = new Button("Complete Check", new Icon(VaadinIcon.CHECK));
        completeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        completeButton.addClassName("complete-btn");
        completeButton.addClickListener(e -> completeCheck());

        // Disable complete button if check is already completed or not all items verified
        if (checkSummary.status() == CheckStatus.COMPLETED ||
                checkSummary.status() == CheckStatus.ABANDONED) {
            completeButton.setEnabled(false);
            abandonButton.setEnabled(false);
            continueButton.setEnabled(false);
        }

        HorizontalLayout footer = new HorizontalLayout(abandonButton, continueButton, completeButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        footer.setPadding(true);
        footer.setSpacing(true);
        footer.addClassName("summary-footer");

        return footer;
    }

    private void showAbandonConfirmation() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Abandon Check?");
        dialog.setText("Are you sure you want to abandon this inventory check? " +
                "This action cannot be undone, and you will need to start a new check.");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Abandon");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> abandonCheck());

        dialog.open();
    }

    private void completeCheck() {
        try {
            inventoryCheckService.completeCheck(checkId);

            Notification notification = Notification.show(
                    "Inventory check completed for " + apparatusName,
                    3000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate(ApparatusSelectionView.class));

        } catch (ShiftInventoryCheckService.IncompleteCheckException e) {
            Notification notification = Notification.show(
                    e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification notification = Notification.show(
                    "Error completing check: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void abandonCheck() {
        try {
            inventoryCheckService.abandonCheck(checkId);

            Notification notification = Notification.show(
                    "Inventory check abandoned",
                    3000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);

            getUI().ifPresent(ui -> ui.navigate(ApparatusSelectionView.class));

        } catch (Exception e) {
            Notification notification = Notification.show(
                    "Error abandoning check: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
