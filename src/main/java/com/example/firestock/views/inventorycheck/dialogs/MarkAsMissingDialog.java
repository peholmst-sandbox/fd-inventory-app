package com.example.firestock.views.inventorycheck.dialogs;

import com.example.firestock.inventorycheck.CheckableItem;
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
import com.vaadin.flow.component.textfield.TextArea;

import java.util.function.BiConsumer;

/**
 * Dialog for marking an item as missing during inventory check.
 * Requires the user to enter notes about the missing item.
 *
 * <p>Layout:
 * <pre>
 * Mark as Missing
 * ----------------
 * | SCBA 1                                 |
 * | Self-contained breathing apparatus     |
 * | S/N: SCBA-2024-001                     |
 * ----------------
 * Notes*
 * |--------------------------------------|
 * | Where was it last seen? Any details  |
 * | about the missing item...            |
 * |--------------------------------------|
 * [CANCEL] [CONFIRM MISSING]
 * </pre>
 */
public class MarkAsMissingDialog extends Dialog {

    private final TextArea notesField;

    /**
     * Creates a dialog for marking an item as missing.
     *
     * @param item the item to mark as missing
     * @param onConfirm callback when confirmed (item, notes)
     */
    public MarkAsMissingDialog(CheckableItem item, BiConsumer<CheckableItem, String> onConfirm) {
        addClassName("item-verification-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);

        // Title
        H3 title = new H3("Mark as Missing");
        title.addClassName("dialog-title");
        title.addClassName("status-missing");
        title.getStyle().set("color", "var(--firestock-missing)");

        // Item details section
        VerticalLayout itemDetails = createItemDetails(item);

        // Notes field (required)
        notesField = new TextArea("Notes");
        notesField.setPlaceholder("Where was it last seen? Any details about the missing item...");
        notesField.setWidthFull();
        notesField.setMinHeight("100px");
        notesField.setRequired(true);
        notesField.setRequiredIndicatorVisible(true);
        notesField.addClassName("dialog-notes");

        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClassName("ic-touch-btn");

        Button confirmButton = new Button("Confirm Missing", new Icon(VaadinIcon.CLOSE));
        confirmButton.addClassNames("confirm-btn", "confirm-missing", "ic-touch-btn");
        confirmButton.addClickListener(e -> {
            // Validate notes
            if (notesField.getValue() == null || notesField.getValue().isBlank()) {
                notesField.setErrorMessage("Notes are required when marking an item as missing");
                notesField.setInvalid(true);
                return;
            }

            onConfirm.accept(item, notesField.getValue());
            close();
        });

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, confirmButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.addClassName("dialog-buttons");

        // Assemble dialog content
        VerticalLayout content = new VerticalLayout(title, itemDetails, notesField, buttons);
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("dialog-content");

        add(content);
    }

    private VerticalLayout createItemDetails(CheckableItem item) {
        VerticalLayout details = new VerticalLayout();
        details.setPadding(true);
        details.setSpacing(false);
        details.addClassName("dialog-item-details");

        // Item name
        Span nameSpan = new Span(item.name());
        nameSpan.addClassName("dialog-item-name");
        details.add(nameSpan);

        // Type name
        Span typeSpan = new Span(item.typeName());
        typeSpan.addClassName("dialog-item-type");
        details.add(typeSpan);

        // Serial number (if equipment)
        if (item.serialNumber() != null) {
            Span serialSpan = new Span("S/N: " + item.serialNumber().value());
            serialSpan.addClassName("dialog-item-serial");
            details.add(serialSpan);
        }

        return details;
    }
}
