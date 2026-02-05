package com.example.firestock.views.inventorycheck.dialogs;

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

/**
 * Dialog for confirming abandonment of an inventory check.
 * Warns the user that all progress will be lost.
 *
 * <p>Layout:
 * <pre>
 * Abandon Check?
 * ----------------------
 * | [Warning Icon]                              |
 * | Are you sure you want to abandon this       |
 * | inventory check? All progress will be lost. |
 * ----------------------
 * [CANCEL] [ABANDON CHECK]
 * </pre>
 */
public class ConfirmAbandonDialog extends Dialog {

    /**
     * Creates a dialog for confirming an inventory check abandonment.
     *
     * @param onConfirm callback when abandonment is confirmed
     */
    public ConfirmAbandonDialog(Runnable onConfirm) {
        addClassName("ic-confirm-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);

        // Title
        H3 title = new H3("Abandon Check?");
        title.addClassName("dialog-title");
        title.getStyle().set("color", "var(--lumo-error-text-color)");

        // Warning message section
        VerticalLayout messageSection = createMessageSection();

        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClassName("ic-touch-btn");

        Button abandonButton = new Button("Abandon Check", new Icon(VaadinIcon.TRASH));
        abandonButton.addClassNames("ic-touch-btn");
        abandonButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        abandonButton.addClickListener(e -> {
            onConfirm.run();
            close();
        });

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, abandonButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.addClassName("ic-confirm-dialog-buttons");

        // Assemble dialog content
        VerticalLayout content = new VerticalLayout(title, messageSection, buttons);
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("ic-confirm-dialog-content");

        add(content);
    }

    private VerticalLayout createMessageSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle()
            .set("background-color", "var(--lumo-error-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("text-align", "center");

        // Warning icon
        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.getStyle()
            .set("color", "var(--lumo-error-text-color)")
            .set("width", "48px")
            .set("height", "48px");

        // Message
        Span message1 = new Span("Are you sure you want to abandon this inventory check?");
        message1.addClassName("ic-confirm-dialog-message");

        Span message2 = new Span("All progress will be lost.");
        message2.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)");

        section.add(warningIcon, message1, message2);
        section.setAlignItems(FlexComponent.Alignment.CENTER);
        return section;
    }
}
