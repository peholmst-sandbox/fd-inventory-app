package com.example.firestock.views.issues;

import com.example.firestock.issues.EquipmentForReport;
import com.example.firestock.issues.ReportIssueRequest;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.function.Consumer;

/**
 * Dialog for reporting a new equipment issue.
 * Collects issue type, description, and severity from the user.
 */
class ReportIssueDialog extends Dialog {

    private static final int MIN_DESCRIPTION_LENGTH = 10;

    private final EquipmentForReport equipment;
    private final RadioButtonGroup<IssueCategory> categoryGroup;
    private final TextArea descriptionField;
    private final RadioButtonGroup<IssueSeverity> severityGroup;
    private final Checkbox criticalConfirmation;
    private final Span charCountLabel;
    private final Consumer<ReportIssueRequest> onSubmit;

    public ReportIssueDialog(EquipmentForReport equipment, Consumer<ReportIssueRequest> onSubmit) {
        this.equipment = equipment;
        this.onSubmit = onSubmit;

        addClassName("report-issue-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);
        setModal(true);

        // Title
        H3 title = new H3("Report Issue");
        title.addClassName("dialog-title");

        // Equipment header
        VerticalLayout equipmentHeader = createEquipmentHeader();

        // Issue type selection
        categoryGroup = new RadioButtonGroup<>();
        categoryGroup.setLabel("Issue Type");
        categoryGroup.setItems(IssueCategory.DAMAGE, IssueCategory.MISSING, IssueCategory.MALFUNCTION);
        categoryGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        categoryGroup.setRenderer(new ComponentRenderer<>(this::createCategoryItem));
        categoryGroup.setRequired(true);
        categoryGroup.addClassName("issue-type-group");

        // Description
        descriptionField = new TextArea("Description");
        descriptionField.setPlaceholder("Describe the issue in detail...");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("120px");
        descriptionField.setRequired(true);
        descriptionField.setRequiredIndicatorVisible(true);
        descriptionField.addClassName("issue-description");
        descriptionField.setHelperText("Minimum " + MIN_DESCRIPTION_LENGTH + " characters");

        // Character count
        charCountLabel = new Span("0 / " + MIN_DESCRIPTION_LENGTH + " min");
        charCountLabel.addClassName("char-count");
        descriptionField.addValueChangeListener(e -> updateCharCount());

        // Severity selection
        severityGroup = new RadioButtonGroup<>();
        severityGroup.setLabel("Severity");
        severityGroup.setItems(IssueSeverity.CRITICAL, IssueSeverity.HIGH, IssueSeverity.MEDIUM, IssueSeverity.LOW);
        severityGroup.setValue(IssueSeverity.MEDIUM);
        severityGroup.setRequired(true);
        severityGroup.setRenderer(new ComponentRenderer<>(this::createSeverityItem));
        severityGroup.addClassName("severity-group");
        severityGroup.addValueChangeListener(e -> updateCriticalConfirmationVisibility());

        // Critical confirmation checkbox
        criticalConfirmation = new Checkbox("I confirm this is a critical safety issue requiring immediate attention");
        criticalConfirmation.addClassName("critical-confirmation");
        criticalConfirmation.setVisible(false);

        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button submitButton = new Button("Report Issue", VaadinIcon.EXCLAMATION_CIRCLE.create());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        submitButton.addClassName("submit-btn");
        submitButton.addClickListener(e -> handleSubmit());

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, submitButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.addClassName("dialog-buttons");

        // Assemble dialog content
        VerticalLayout content = new VerticalLayout(
                title,
                equipmentHeader,
                categoryGroup,
                descriptionField,
                charCountLabel,
                severityGroup,
                criticalConfirmation,
                buttons
        );
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("dialog-content");

        add(content);
    }

    private VerticalLayout createEquipmentHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(true);
        header.setSpacing(false);
        header.addClassName("equipment-header");

        Span nameSpan = new Span(equipment.name());
        nameSpan.addClassName("equipment-name");

        HorizontalLayout detailsRow = new HorizontalLayout();
        detailsRow.setSpacing(true);

        if (equipment.serialNumber() != null) {
            Span serialSpan = new Span("S/N: " + equipment.serialNumber().value());
            serialSpan.addClassName("equipment-serial");
            detailsRow.add(serialSpan);
        }

        Span locationSpan = new Span(equipment.locationDisplay());
        locationSpan.addClassName("equipment-location");
        detailsRow.add(locationSpan);

        Span statusSpan = new Span(equipment.statusLabel());
        statusSpan.addClassName("equipment-status");
        statusSpan.addClassName("status-" + equipment.status().getLiteral().toLowerCase());

        header.add(nameSpan, detailsRow, statusSpan);
        return header;
    }

    private HorizontalLayout createCategoryItem(IssueCategory category) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.addClassName("issue-type-card");
        layout.addClassName("category-" + category.getLiteral().toLowerCase());

        Icon icon = switch (category) {
            case DAMAGE -> VaadinIcon.WARNING.create();
            case MISSING -> VaadinIcon.CLOSE_CIRCLE.create();
            case MALFUNCTION -> VaadinIcon.COG.create();
            default -> VaadinIcon.QUESTION_CIRCLE.create();
        };
        icon.addClassName("category-icon");

        Span label = new Span(getCategoryLabel(category));
        label.addClassName("category-label");

        layout.add(icon, label);
        return layout;
    }

    private String getCategoryLabel(IssueCategory category) {
        return switch (category) {
            case DAMAGE -> "Damaged";
            case MISSING -> "Missing";
            case MALFUNCTION -> "Malfunctioning";
            case EXPIRED -> "Expired";
            case LOW_STOCK -> "Low Stock";
            case CONTAMINATION -> "Contaminated";
            case CALIBRATION -> "Needs Calibration";
            case OTHER -> "Other";
        };
    }

    private HorizontalLayout createSeverityItem(IssueSeverity severity) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.addClassName("severity-item");
        layout.addClassName("severity-" + severity.getLiteral().toLowerCase());

        Span indicator = new Span();
        indicator.addClassName("severity-indicator");

        Span label = new Span(getSeverityLabel(severity));
        label.addClassName("severity-label");

        Span description = new Span(getSeverityDescription(severity));
        description.addClassName("severity-description");

        VerticalLayout textContent = new VerticalLayout(label, description);
        textContent.setPadding(false);
        textContent.setSpacing(false);

        layout.add(indicator, textContent);
        return layout;
    }

    private String getSeverityLabel(IssueSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "Critical";
            case HIGH -> "High";
            case MEDIUM -> "Medium";
            case LOW -> "Low";
        };
    }

    private String getSeverityDescription(IssueSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "Safety risk, requires immediate action";
            case HIGH -> "Operational impact, needs prompt attention";
            case MEDIUM -> "Should be addressed soon";
            case LOW -> "Minor issue, can be scheduled";
        };
    }

    private void updateCharCount() {
        String text = descriptionField.getValue();
        int length = text != null ? text.length() : 0;
        charCountLabel.setText(length + " / " + MIN_DESCRIPTION_LENGTH + " min");

        if (length >= MIN_DESCRIPTION_LENGTH) {
            charCountLabel.removeClassName("char-count-invalid");
            charCountLabel.addClassName("char-count-valid");
        } else {
            charCountLabel.removeClassName("char-count-valid");
            charCountLabel.addClassName("char-count-invalid");
        }
    }

    private void updateCriticalConfirmationVisibility() {
        IssueSeverity selected = severityGroup.getValue();
        criticalConfirmation.setVisible(selected == IssueSeverity.CRITICAL);
        if (selected != IssueSeverity.CRITICAL) {
            criticalConfirmation.setValue(false);
        }
    }

    private void handleSubmit() {
        // Validate category
        if (categoryGroup.getValue() == null) {
            categoryGroup.setInvalid(true);
            categoryGroup.setErrorMessage("Please select an issue type");
            return;
        }

        // Validate description
        String description = descriptionField.getValue();
        if (description == null || description.strip().length() < MIN_DESCRIPTION_LENGTH) {
            descriptionField.setInvalid(true);
            descriptionField.setErrorMessage("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
            return;
        }

        // Validate critical confirmation
        IssueSeverity severity = severityGroup.getValue();
        if (severity == IssueSeverity.CRITICAL && !criticalConfirmation.getValue()) {
            criticalConfirmation.setInvalid(true);
            return;
        }

        // Build request and submit
        ReportIssueRequest request = new ReportIssueRequest(
                equipment.id(),
                categoryGroup.getValue(),
                description.strip(),
                severity,
                criticalConfirmation.getValue()
        );

        onSubmit.accept(request);
        close();
    }
}
