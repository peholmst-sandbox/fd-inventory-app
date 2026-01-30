package com.example.firestock.views.inventorycheck;

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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

/**
 * Summary view showing inventory check results.
 * Displays statistics and list of issues (missing/damaged items).
 */
@Route(value = "check/:apparatusId/summary", layout = MainLayout.class)
@PageTitle("Check Summary | FireStock")
public class InventoryCheckSummaryView extends VerticalLayout implements BeforeEnterObserver {

    private String apparatusId;
    private String apparatusName;

    public InventoryCheckSummaryView() {
        addClassName("inventory-summary-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.apparatusId = event.getRouteParameters().get("apparatusId").orElse("1");
        this.apparatusName = getApparatusName(apparatusId);
        buildUI();
    }

    private void buildUI() {
        removeAll();

        add(createHeader());
        add(createStatisticsSection());
        add(createIssuesSection());
        add(createFooter());
    }

    private HorizontalLayout createHeader() {
        Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassName("back-button");
        backButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(InventoryCheckView.class, apparatusId))
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
        // Mock statistics data
        SummaryStats stats = getMockStats();

        Div totalCard = createStatCard("Total Items", String.valueOf(stats.total()), "stat-total", VaadinIcon.LIST);
        Div presentCard = createStatCard("Present", String.valueOf(stats.present()), "stat-present", VaadinIcon.CHECK);
        Div missingCard = createStatCard("Missing", String.valueOf(stats.missing()), "stat-missing", VaadinIcon.CLOSE);
        Div damagedCard = createStatCard("Damaged", String.valueOf(stats.damaged()), "stat-damaged", VaadinIcon.WARNING);

        HorizontalLayout statsRow1 = new HorizontalLayout(totalCard, presentCard);
        statsRow1.setWidthFull();
        statsRow1.setSpacing(true);

        HorizontalLayout statsRow2 = new HorizontalLayout(missingCard, damagedCard);
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

    private VerticalLayout createIssuesSection() {
        H3 issuesHeader = new H3("Issues Found");
        issuesHeader.addClassName("issues-header");

        VerticalLayout issuesList = new VerticalLayout();
        issuesList.setPadding(false);
        issuesList.setSpacing(true);
        issuesList.addClassName("issues-list");

        List<IssueItem> issues = getMockIssues();

        if (issues.isEmpty()) {
            Span noIssues = new Span("No issues found. All items accounted for!");
            noIssues.addClassName("no-issues");
            issuesList.add(noIssues);
        } else {
            for (IssueItem issue : issues) {
                issuesList.add(createIssueCard(issue));
            }
        }

        VerticalLayout section = new VerticalLayout(issuesHeader, issuesList);
        section.setPadding(true);
        section.setSpacing(true);
        section.addClassName("issues-section");

        return section;
    }

    private Div createIssueCard(IssueItem issue) {
        Div card = new Div();
        card.addClassName("issue-card");
        card.addClassName("issue-" + issue.status().toLowerCase());

        Icon statusIcon = issue.status().equals("Missing")
                ? VaadinIcon.CLOSE.create()
                : VaadinIcon.WARNING.create();
        statusIcon.addClassName("issue-icon");

        Span itemName = new Span(issue.itemName());
        itemName.addClassName("issue-item-name");

        Span statusBadge = new Span(issue.status());
        statusBadge.addClassName("issue-status-badge");
        statusBadge.addClassName("status-" + issue.status().toLowerCase());

        HorizontalLayout topRow = new HorizontalLayout(statusIcon, itemName, statusBadge);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);
        topRow.setWidthFull();

        Span compartment = new Span("Compartment: " + issue.compartment());
        compartment.addClassName("issue-compartment");

        Span notes = new Span(issue.notes());
        notes.addClassName("issue-notes");

        VerticalLayout content = new VerticalLayout(topRow, compartment, notes);
        content.setPadding(false);
        content.setSpacing(false);

        card.add(content);
        return card;
    }

    private HorizontalLayout createFooter() {
        Button continueButton = new Button("Continue Checking", new Icon(VaadinIcon.ARROW_LEFT));
        continueButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        continueButton.addClassName("continue-btn");
        continueButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(InventoryCheckView.class, apparatusId))
        );

        Button completeButton = new Button("Complete Check", new Icon(VaadinIcon.CHECK));
        completeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        completeButton.addClassName("complete-btn");
        completeButton.addClickListener(e -> {
            Notification notification = Notification.show(
                    "Inventory check completed for " + apparatusName,
                    3000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate(ApparatusSelectionView.class));
        });

        HorizontalLayout footer = new HorizontalLayout(continueButton, completeButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        footer.setPadding(true);
        footer.setSpacing(true);
        footer.addClassName("summary-footer");

        return footer;
    }

    private String getApparatusName(String id) {
        return switch (id) {
            case "1" -> "Engine 1";
            case "2" -> "Ladder 2";
            case "3" -> "Rescue 3";
            default -> "Unknown Apparatus";
        };
    }

    private SummaryStats getMockStats() {
        return new SummaryStats(20, 17, 2, 1);
    }

    private List<IssueItem> getMockIssues() {
        return List.of(
                new IssueItem("SCBA Pack #2", "Missing", "Driver Side", "Last seen during last shift change"),
                new IssueItem("Portable Radio #1", "Damaged", "Officer Side", "Cracked display, still functional")
        );
    }

    private record SummaryStats(int total, int present, int missing, int damaged) {}
    private record IssueItem(String itemName, String status, String compartment, String notes) {}
}
