package com.example.firestock.views.audit;

import com.example.firestock.audit.AuditableItem;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.ExpiryStatus;
import com.example.firestock.jooq.enums.ItemCondition;
import com.example.firestock.jooq.enums.TestResult;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Dialog for recording the audit result of an individual item.
 * Supports condition assessment, functional testing, and expiry status.
 */
public class AuditItemDialog extends Dialog {

    private final AuditableItem item;
    private final Consumer<AuditItemResult> onSave;

    private RadioButtonGroup<AuditItemStatus> statusGroup;
    private RadioButtonGroup<ItemCondition> conditionGroup;
    private RadioButtonGroup<TestResult> testResultGroup;
    private RadioButtonGroup<ExpiryStatus> expiryStatusGroup;
    private TextArea conditionNotes;
    private TextArea testNotes;
    private NumberField quantityFoundField;

    public AuditItemDialog(AuditableItem item, Consumer<AuditItemResult> onSave) {
        this.item = item;
        this.onSave = onSave;

        setHeaderTitle("Audit Item");
        setWidth("500px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        buildContent();
        buildFooter();
    }

    private void buildContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        // Item info header
        H3 itemName = new H3(item.name());
        itemName.addClassName("item-name");

        Span typeName = new Span(item.typeName());
        typeName.addClassName("text-secondary");

        content.add(itemName, typeName);

        if (item.serialNumber() != null) {
            Span serialNumber = new Span("S/N: " + item.serialNumber().value());
            serialNumber.addClassName("text-secondary");
            content.add(serialNumber);
        }

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Status selection
        statusGroup = new RadioButtonGroup<>();
        statusGroup.setLabel("Audit Status");
        statusGroup.setItems(
                AuditItemStatus.VERIFIED,
                AuditItemStatus.MISSING,
                AuditItemStatus.DAMAGED,
                AuditItemStatus.FAILED_INSPECTION,
                AuditItemStatus.EXPIRED
        );
        statusGroup.setItemLabelGenerator(status -> switch (status) {
            case VERIFIED -> "Verified (Present & OK)";
            case MISSING -> "Missing";
            case DAMAGED -> "Damaged";
            case FAILED_INSPECTION -> "Failed Inspection";
            case EXPIRED -> "Expired";
            default -> status.getLiteral();
        });
        statusGroup.setValue(AuditItemStatus.VERIFIED);
        form.add(statusGroup);

        // Condition selection
        conditionGroup = new RadioButtonGroup<>();
        conditionGroup.setLabel("Condition");
        conditionGroup.setItems(ItemCondition.values());
        conditionGroup.setItemLabelGenerator(ItemCondition::getLiteral);
        conditionGroup.setValue(ItemCondition.GOOD);
        form.add(conditionGroup);

        // Test result (only show if item requires testing)
        if (item.requiresTesting()) {
            testResultGroup = new RadioButtonGroup<>();
            testResultGroup.setLabel("Functional Test Result");
            testResultGroup.setItems(TestResult.values());
            testResultGroup.setItemLabelGenerator(TestResult::getLiteral);
            testResultGroup.setValue(TestResult.PASSED);
            form.add(testResultGroup);

            testNotes = new TextArea("Test Notes");
            testNotes.setPlaceholder("Notes about test results...");
            testNotes.setMaxLength(1000);
            form.add(testNotes);
        }

        // Expiry status (only show if item has expiry)
        if (item.expiryDate() != null) {
            expiryStatusGroup = new RadioButtonGroup<>();
            expiryStatusGroup.setLabel("Expiry Status");
            expiryStatusGroup.setItems(ExpiryStatus.values());
            expiryStatusGroup.setItemLabelGenerator(ExpiryStatus::getLiteral);

            // Auto-select based on actual expiry date
            if (item.hasExpiryWarning()) {
                expiryStatusGroup.setValue(item.expiryDate().isBefore(java.time.LocalDate.now())
                        ? ExpiryStatus.EXPIRED
                        : ExpiryStatus.EXPIRING_SOON);
            } else {
                expiryStatusGroup.setValue(ExpiryStatus.OK);
            }
            form.add(expiryStatusGroup);
        }

        // Quantity field for consumables
        if (item.isConsumable()) {
            quantityFoundField = new NumberField("Quantity Found");
            quantityFoundField.setMin(0);
            quantityFoundField.setStep(0.5);
            if (item.currentQuantity() != null) {
                quantityFoundField.setValue(item.currentQuantity().value().doubleValue());
            }
            if (item.requiredQuantity() != null) {
                quantityFoundField.setHelperText("Expected: " + item.requiredQuantity());
            }
            form.add(quantityFoundField);
        }

        // Condition notes
        conditionNotes = new TextArea("Condition Notes");
        conditionNotes.setPlaceholder("Notes about the item's condition...");
        conditionNotes.setMaxLength(1000);
        form.add(conditionNotes);

        content.add(form);
        add(content);
    }

    private void buildFooter() {
        Button saveButton = new Button("Save", e -> saveAndClose());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> close());

        // Quick action buttons
        Button quickVerified = new Button("Quick: Verified + Good", e -> {
            statusGroup.setValue(AuditItemStatus.VERIFIED);
            conditionGroup.setValue(ItemCondition.GOOD);
            if (testResultGroup != null) {
                testResultGroup.setValue(TestResult.PASSED);
            }
            if (expiryStatusGroup != null && !item.hasExpiryWarning()) {
                expiryStatusGroup.setValue(ExpiryStatus.OK);
            }
            saveAndClose();
        });
        quickVerified.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout footer = new HorizontalLayout(quickVerified, saveButton, cancelButton);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();

        getFooter().add(footer);
    }

    private void saveAndClose() {
        AuditItemResult result = new AuditItemResult(
                statusGroup.getValue(),
                conditionGroup.getValue(),
                testResultGroup != null ? testResultGroup.getValue() : null,
                expiryStatusGroup != null ? expiryStatusGroup.getValue() : null,
                conditionNotes.getValue(),
                testNotes != null ? testNotes.getValue() : null,
                quantityFoundField != null && quantityFoundField.getValue() != null
                        ? BigDecimal.valueOf(quantityFoundField.getValue())
                        : null
        );

        close();
        onSave.accept(result);
    }

    /**
     * Result of auditing an item in the dialog.
     */
    public record AuditItemResult(
            AuditItemStatus status,
            ItemCondition condition,
            TestResult testResult,
            ExpiryStatus expiryStatus,
            String conditionNotes,
            String testNotes,
            BigDecimal quantityFound
    ) {}
}
