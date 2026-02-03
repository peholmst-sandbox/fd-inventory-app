package com.example.firestock.views.inventorycheck;

import com.example.firestock.inventorycheck.CheckableItem;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Dialog for marking item status with optional notes.
 * Used when marking items as Missing or Damaged.
 */
class ItemVerificationDialog extends Dialog {

    private final TextArea notesField;
    private final NumberField quantityField;
    private final CheckableItem item;

    /**
     * Result of the verification dialog.
     */
    public record VerificationResult(
            VerificationStatus status,
            String notes,
            BigDecimal quantityFound
    ) {}

    public ItemVerificationDialog(
            CheckableItem item,
            VerificationStatus status,
            Consumer<VerificationResult> onConfirm
    ) {
        this.item = item;
        addClassName("item-verification-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);

        H3 title = new H3("Mark as " + getStatusLabel(status));
        title.addClassName("dialog-title");
        title.addClassName("status-" + getStatusCssClass(status));

        Span nameSpan = new Span(item.name());
        nameSpan.addClassName("dialog-item-name");

        Span typeSpan = new Span(item.typeName());
        typeSpan.addClassName("dialog-item-type");

        VerticalLayout itemDetails = new VerticalLayout(nameSpan, typeSpan);
        itemDetails.setPadding(false);
        itemDetails.setSpacing(false);
        itemDetails.addClassName("dialog-item-details");

        // Add serial number for equipment
        if (item.serialNumber() != null) {
            Span serialSpan = new Span("S/N: " + item.serialNumber().value());
            serialSpan.addClassName("dialog-item-serial");
            itemDetails.add(serialSpan);
        }

        // Add quantity field for consumables
        quantityField = new NumberField("Quantity Found");
        quantityField.setVisible(item.isConsumable());
        if (item.isConsumable()) {
            quantityField.setMin(0);
            quantityField.setStep(0.01);
            if (item.currentQuantity() != null) {
                quantityField.setValue(item.currentQuantity().value().doubleValue());
            }
            String expectedLabel = item.requiredQuantity() != null
                    ? String.format(" (Expected: %s)", item.requiredQuantity().toPlainString())
                    : "";
            quantityField.setHelperText("Enter the actual quantity found" + expectedLabel);
            quantityField.setWidthFull();
        }

        notesField = new TextArea("Notes" + (requiresNotes(status) ? "" : " (optional)"));
        notesField.setPlaceholder(getNotesPlaceholder(status));
        notesField.setWidthFull();
        notesField.setMinHeight("100px");
        notesField.addClassName("dialog-notes");
        if (requiresNotes(status)) {
            notesField.setRequired(true);
            notesField.setRequiredIndicatorVisible(true);
        }

        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button confirmButton = new Button("Confirm " + getStatusLabel(status), getStatusIcon(status));
        confirmButton.addClassName("confirm-btn");
        confirmButton.addClassName("confirm-" + getStatusCssClass(status));
        confirmButton.addClickListener(e -> {
            // Validate required notes
            if (requiresNotes(status) && (notesField.getValue() == null || notesField.getValue().isBlank())) {
                notesField.setErrorMessage("Notes are required for this status");
                notesField.setInvalid(true);
                return;
            }

            BigDecimal qty = null;
            if (item.isConsumable() && quantityField.getValue() != null) {
                qty = BigDecimal.valueOf(quantityField.getValue());
            }

            onConfirm.accept(new VerificationResult(
                    status,
                    notesField.getValue(),
                    qty
            ));
            close();
        });

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, confirmButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.addClassName("dialog-buttons");

        VerticalLayout content = new VerticalLayout(title, itemDetails);
        if (item.isConsumable()) {
            content.add(quantityField);
        }
        content.add(notesField, buttons);
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("dialog-content");

        add(content);
    }

    private boolean requiresNotes(VerificationStatus status) {
        return status == VerificationStatus.MISSING || status == VerificationStatus.PRESENT_DAMAGED;
    }

    private String getNotesPlaceholder(VerificationStatus status) {
        return switch (status) {
            case MISSING -> "Where was it last seen? Any details about the missing item...";
            case PRESENT_DAMAGED -> "Describe the damage, severity, and if the item is still usable...";
            case EXPIRED -> "Describe the expiry details...";
            case LOW_QUANTITY -> "Describe the quantity discrepancy...";
            default -> "Add any relevant notes...";
        };
    }

    private Icon getStatusIcon(VerificationStatus status) {
        return switch (status) {
            case MISSING -> new Icon(VaadinIcon.CLOSE);
            case PRESENT_DAMAGED, EXPIRED, LOW_QUANTITY -> new Icon(VaadinIcon.WARNING);
            default -> new Icon(VaadinIcon.CHECK);
        };
    }

    private String getStatusLabel(VerificationStatus status) {
        return switch (status) {
            case PRESENT -> "Present";
            case PRESENT_DAMAGED -> "Damaged";
            case MISSING -> "Missing";
            case EXPIRED -> "Expired";
            case LOW_QUANTITY -> "Low Quantity";
            case SKIPPED -> "Skipped";
        };
    }

    private String getStatusCssClass(VerificationStatus status) {
        return switch (status) {
            case PRESENT -> "present";
            case PRESENT_DAMAGED -> "damaged";
            case MISSING -> "missing";
            case EXPIRED -> "expired";
            case LOW_QUANTITY -> "low-quantity";
            case SKIPPED -> "skipped";
        };
    }
}
