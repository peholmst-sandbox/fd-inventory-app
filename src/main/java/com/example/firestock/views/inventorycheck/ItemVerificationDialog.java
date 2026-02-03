package com.example.firestock.views.inventorycheck;

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

import java.util.function.Consumer;

/**
 * Dialog for marking item status with optional notes.
 * Used when marking items as Missing or Damaged.
 */
class ItemVerificationDialog extends Dialog {

    private final TextArea notesField;

    public ItemVerificationDialog(
            String itemName,
            String itemType,
            String serialNumber,
            InventoryCheckView.ItemStatus status,
            Consumer<String> onConfirm
    ) {
        addClassName("item-verification-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);

        H3 title = new H3("Mark as " + status.label);
        title.addClassName("dialog-title");
        title.addClassName("status-" + status.cssClass);

        Span nameSpan = new Span(itemName);
        nameSpan.addClassName("dialog-item-name");

        Span typeSpan = new Span(itemType);
        typeSpan.addClassName("dialog-item-type");

        Span serialSpan = new Span("S/N: " + serialNumber);
        serialSpan.addClassName("dialog-item-serial");

        VerticalLayout itemDetails = new VerticalLayout(nameSpan, typeSpan, serialSpan);
        itemDetails.setPadding(false);
        itemDetails.setSpacing(false);
        itemDetails.addClassName("dialog-item-details");

        notesField = new TextArea("Notes (optional)");
        notesField.setPlaceholder(getNotesPlaceholder(status));
        notesField.setWidthFull();
        notesField.setMinHeight("100px");
        notesField.addClassName("dialog-notes");

        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button confirmButton = new Button("Confirm " + status.label, getStatusIcon(status));
        confirmButton.addClassName("confirm-btn");
        confirmButton.addClassName("confirm-" + status.cssClass);
        confirmButton.addClickListener(e -> {
            onConfirm.accept(notesField.getValue());
            close();
        });

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, confirmButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.addClassName("dialog-buttons");

        VerticalLayout content = new VerticalLayout(title, itemDetails, notesField, buttons);
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("dialog-content");

        add(content);
    }

    private String getNotesPlaceholder(InventoryCheckView.ItemStatus status) {
        return switch (status) {
            case MISSING -> "Where was it last seen? Any details about the missing item...";
            case DAMAGED -> "Describe the damage, severity, and if the item is still usable...";
            default -> "Add any relevant notes...";
        };
    }

    private Icon getStatusIcon(InventoryCheckView.ItemStatus status) {
        return switch (status) {
            case MISSING -> new Icon(VaadinIcon.CLOSE);
            case DAMAGED -> new Icon(VaadinIcon.WARNING);
            default -> new Icon(VaadinIcon.CHECK);
        };
    }
}
