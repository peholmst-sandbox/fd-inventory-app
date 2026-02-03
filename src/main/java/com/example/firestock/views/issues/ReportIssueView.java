package com.example.firestock.views.issues;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.issues.EquipmentForReport;
import com.example.firestock.issues.IssueCreatedResult;
import com.example.firestock.issues.IssueSummary;
import com.example.firestock.issues.ReportIssueRequest;
import com.example.firestock.issues.ReportIssueService;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * View for reporting equipment issues.
 * Route: /report-issue or /report-issue/{equipmentId}
 */
@Route(value = "report-issue", layout = MainLayout.class)
@PermitAll
public class ReportIssueView extends VerticalLayout implements BeforeEnterObserver {

    private final transient ReportIssueService reportIssueService;
    private final transient AuthenticationContext authenticationContext;

    private final TextField lookupField;
    private final VerticalLayout equipmentContainer;
    private final VerticalLayout resultContainer;

    private EquipmentForReport currentEquipment;

    public ReportIssueView(ReportIssueService reportIssueService, AuthenticationContext authenticationContext) {
        this.reportIssueService = reportIssueService;
        this.authenticationContext = authenticationContext;

        addClassName("report-issue-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header
        H2 title = new H2("Report Equipment Issue");
        title.addClassName("view-header");

        Span subtitle = new Span("Enter the equipment barcode or serial number to report an issue");
        subtitle.addClassName("view-subtitle");

        // Lookup section
        lookupField = new TextField();
        lookupField.setPlaceholder("Enter barcode or serial number");
        lookupField.setWidthFull();
        lookupField.setClearButtonVisible(true);
        lookupField.addClassName("lookup-field");
        lookupField.setPrefixComponent(VaadinIcon.BARCODE.create());

        Button lookupButton = new Button("Look Up", VaadinIcon.SEARCH.create());
        lookupButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        lookupButton.addClickListener(e -> performLookup());
        lookupButton.addClassName("lookup-btn");

        HorizontalLayout lookupRow = new HorizontalLayout(lookupField, lookupButton);
        lookupRow.setWidthFull();
        lookupRow.setFlexGrow(1, lookupField);
        lookupRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        lookupRow.addClassName("lookup-row");

        // Equipment display container
        equipmentContainer = new VerticalLayout();
        equipmentContainer.setVisible(false);
        equipmentContainer.setPadding(false);
        equipmentContainer.setSpacing(true);
        equipmentContainer.addClassName("equipment-container");

        // Result container (for success messages)
        resultContainer = new VerticalLayout();
        resultContainer.setVisible(false);
        resultContainer.setPadding(false);
        resultContainer.addClassName("result-container");

        // Assemble view
        VerticalLayout content = new VerticalLayout(
                title,
                subtitle,
                lookupRow,
                equipmentContainer,
                resultContainer
        );
        content.addClassName("report-issue-content");
        content.setMaxWidth("600px");
        content.setPadding(false);

        setAlignItems(FlexComponent.Alignment.CENTER);
        add(content);

        // Allow lookup on Enter key
        lookupField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> performLookup());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check for equipment ID in path
        event.getRouteParameters().get("equipmentId").ifPresent(idStr -> {
            try {
                UUID uuid = UUID.fromString(idStr);
                loadEquipmentById(EquipmentItemId.of(uuid));
            } catch (IllegalArgumentException e) {
                showError("Invalid equipment ID");
            }
        });
    }

    private void performLookup() {
        String input = lookupField.getValue();
        if (input == null || input.strip().isEmpty()) {
            lookupField.setInvalid(true);
            lookupField.setErrorMessage("Please enter a barcode or serial number");
            return;
        }

        input = input.strip();
        lookupField.setInvalid(false);

        // Try barcode first
        try {
            Barcode barcode = new Barcode(input);
            Optional<EquipmentForReport> result = reportIssueService.findByBarcode(barcode);
            if (result.isPresent()) {
                displayEquipment(result.get());
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a valid barcode format, try serial number
        }

        // Try serial number
        try {
            SerialNumber serialNumber = new SerialNumber(input);
            Optional<EquipmentForReport> result = reportIssueService.findBySerialNumber(serialNumber);
            if (result.isPresent()) {
                displayEquipment(result.get());
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a valid serial number format either
        }

        // Not found
        showNotFound(input);
    }

    private void loadEquipmentById(EquipmentItemId id) {
        try {
            EquipmentForReport equipment = reportIssueService.getEquipmentForReport(id);
            displayEquipment(equipment);
        } catch (ReportIssueService.EquipmentNotFoundException e) {
            showError("Equipment not found");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            showError("You do not have access to this equipment");
        }
    }

    private void displayEquipment(EquipmentForReport equipment) {
        this.currentEquipment = equipment;
        equipmentContainer.removeAll();
        resultContainer.setVisible(false);

        // Equipment card
        VerticalLayout card = new VerticalLayout();
        card.addClassName("equipment-card");
        card.setPadding(true);
        card.setSpacing(false);

        Span nameSpan = new Span(equipment.name());
        nameSpan.addClassName("equipment-name");

        HorizontalLayout detailsRow = new HorizontalLayout();
        detailsRow.setSpacing(true);

        if (equipment.serialNumber() != null) {
            Span serialSpan = new Span("S/N: " + equipment.serialNumber().value());
            serialSpan.addClassName("equipment-serial");
            detailsRow.add(serialSpan);
        }

        if (equipment.barcode() != null) {
            Span barcodeSpan = new Span("Barcode: " + equipment.barcode().value());
            barcodeSpan.addClassName("equipment-barcode");
            detailsRow.add(barcodeSpan);
        }

        Span locationSpan = new Span(equipment.locationDisplay());
        locationSpan.addClassName("equipment-location");

        Span statusSpan = new Span("Status: " + equipment.statusLabel());
        statusSpan.addClassName("equipment-status");
        statusSpan.addClassName("status-" + equipment.status().getLiteral().toLowerCase());

        // Report button
        Button reportButton = new Button("Report Issue", VaadinIcon.EXCLAMATION_CIRCLE.create());
        reportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        reportButton.addClassName("report-btn");
        reportButton.addClickListener(e -> checkExistingIssuesAndProceed());

        card.add(nameSpan, detailsRow, locationSpan, statusSpan, reportButton);
        equipmentContainer.add(card);
        equipmentContainer.setVisible(true);
    }

    private void showNotFound(String searchTerm) {
        equipmentContainer.removeAll();
        resultContainer.setVisible(false);

        VerticalLayout notFoundCard = new VerticalLayout();
        notFoundCard.addClassName("not-found-card");
        notFoundCard.setPadding(true);
        notFoundCard.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = VaadinIcon.SEARCH_MINUS.create();
        icon.addClassName("not-found-icon");

        Span message = new Span("No equipment found for: " + searchTerm);
        message.addClassName("not-found-message");

        Span hint = new Span("Check the barcode or serial number and try again");
        hint.addClassName("not-found-hint");

        notFoundCard.add(icon, message, hint);
        equipmentContainer.add(notFoundCard);
        equipmentContainer.setVisible(true);
    }

    private void checkExistingIssuesAndProceed() {
        if (currentEquipment == null) return;

        List<IssueSummary> openIssues = reportIssueService.getOpenIssues(currentEquipment.id());

        if (!openIssues.isEmpty()) {
            // Show existing issues dialog
            ExistingIssueDialog dialog = new ExistingIssueDialog(
                    openIssues,
                    this::handleAddToExisting,
                    v -> openReportDialog()
            );
            dialog.open();
        } else {
            openReportDialog();
        }
    }

    private void openReportDialog() {
        if (currentEquipment == null) return;

        ReportIssueDialog dialog = new ReportIssueDialog(
                currentEquipment,
                this::handleReportSubmit
        );
        dialog.open();
    }

    private void handleReportSubmit(ReportIssueRequest request) {
        authenticationContext.getAuthenticatedUser(FirestockUserDetails.class)
                .ifPresent(user -> {
                    try {
                        IssueCreatedResult.WithEquipmentStatus result =
                                reportIssueService.reportIssue(request, user.getUserId());
                        showSuccess(result);
                    } catch (IllegalArgumentException e) {
                        showError(e.getMessage());
                    } catch (org.springframework.security.access.AccessDeniedException e) {
                        showError("You do not have permission to report issues for this equipment");
                    } catch (Exception e) {
                        showError("An error occurred while reporting the issue");
                    }
                });
    }

    private void handleAddToExisting(IssueSummary issue, String notes) {
        authenticationContext.getAuthenticatedUser(FirestockUserDetails.class)
                .ifPresent(user -> {
                    try {
                        reportIssueService.addToExistingIssue(issue.id(), notes, user.getUserId());
                        showAddedToExistingSuccess(issue);
                    } catch (Exception e) {
                        showError("An error occurred while updating the issue");
                    }
                });
    }

    private void showSuccess(IssueCreatedResult.WithEquipmentStatus result) {
        equipmentContainer.setVisible(false);
        resultContainer.removeAll();

        VerticalLayout successCard = new VerticalLayout();
        successCard.addClassName("success-card");
        successCard.setPadding(true);
        successCard.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = VaadinIcon.CHECK_CIRCLE.create();
        icon.addClassName("success-icon");

        Span title = new Span("Issue Reported Successfully");
        title.addClassName("success-title");

        Span refNumber = new Span("Reference: " + result.referenceNumber().value());
        refNumber.addClassName("success-reference");

        if (result.updatedEquipmentStatus() != null) {
            Span statusUpdate = new Span("Equipment status updated to: " + result.updatedEquipmentStatus().getLiteral());
            statusUpdate.addClassName("status-update");
            successCard.add(statusUpdate);
        }

        Button newReportButton = new Button("Report Another Issue", VaadinIcon.PLUS.create());
        newReportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newReportButton.addClickListener(e -> resetView());

        successCard.add(icon, title, refNumber, newReportButton);
        resultContainer.add(successCard);
        resultContainer.setVisible(true);

        Notification.show("Issue " + result.referenceNumber().value() + " created",
                        3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showAddedToExistingSuccess(IssueSummary issue) {
        equipmentContainer.setVisible(false);
        resultContainer.removeAll();

        VerticalLayout successCard = new VerticalLayout();
        successCard.addClassName("success-card");
        successCard.setPadding(true);
        successCard.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = VaadinIcon.COMMENT.create();
        icon.addClassName("success-icon");

        Span title = new Span("Notes Added Successfully");
        title.addClassName("success-title");

        Span refNumber = new Span("Updated issue: " + issue.referenceNumber().value());
        refNumber.addClassName("success-reference");

        Button newReportButton = new Button("Report Another Issue", VaadinIcon.PLUS.create());
        newReportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newReportButton.addClickListener(e -> resetView());

        successCard.add(icon, title, refNumber, newReportButton);
        resultContainer.add(successCard);
        resultContainer.setVisible(true);

        Notification.show("Notes added to " + issue.referenceNumber().value(),
                        3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void resetView() {
        lookupField.clear();
        currentEquipment = null;
        equipmentContainer.removeAll();
        equipmentContainer.setVisible(false);
        resultContainer.removeAll();
        resultContainer.setVisible(false);
        lookupField.focus();
    }
}
