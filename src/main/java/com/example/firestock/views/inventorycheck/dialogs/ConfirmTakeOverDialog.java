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

import java.util.function.Consumer;

/**
 * Dialog for confirming a compartment take-over from another user.
 * Warns the user that they will interrupt another firefighter's work.
 *
 * <p>Layout:
 * <pre>
 * Take Over Compartment?
 * ----------------------
 * | [Warning Icon]                              |
 * | [Checker Name] is currently checking        |
 * | this compartment. Taking over will          |
 * | interrupt their work.                       |
 * ----------------------
 * [CANCEL] [TAKE OVER]
 * </pre>
 */
public class ConfirmTakeOverDialog extends Dialog {

    /**
     * Creates a dialog for confirming a compartment take-over.
     *
     * @param currentCheckerName the display name of the user currently checking
     * @param onConfirm callback when take-over is confirmed
     */
    public ConfirmTakeOverDialog(String currentCheckerName, Runnable onConfirm) {
        addClassName("item-verification-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);

        // Title
        H3 title = new H3("Take Over Compartment?");
        title.addClassName("dialog-title");
        title.getStyle().set("color", "var(--lumo-warning-text-color)");

        // Warning message section
        VerticalLayout messageSection = createMessageSection(currentCheckerName);

        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClassName("ic-touch-btn");

        Button takeOverButton = new Button("Take Over", new Icon(VaadinIcon.HAND));
        takeOverButton.addClassNames("confirm-btn", "ic-touch-btn");
        takeOverButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        takeOverButton.addClickListener(e -> {
            onConfirm.run();
            close();
        });

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, takeOverButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.addClassName("dialog-buttons");

        // Assemble dialog content
        VerticalLayout content = new VerticalLayout(title, messageSection, buttons);
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("dialog-content");

        add(content);
    }

    private VerticalLayout createMessageSection(String currentCheckerName) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.addClassName("dialog-item-details");
        section.getStyle()
            .set("background-color", "var(--lumo-warning-color-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("text-align", "center");

        // Warning icon
        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.getStyle()
            .set("color", "var(--lumo-warning-text-color)")
            .set("width", "48px")
            .set("height", "48px");
        warningIcon.addClassName("dialog-warning-icon");

        // Message
        Span checkerSpan = new Span(currentCheckerName);
        checkerSpan.getStyle().set("font-weight", "bold");

        Span message1 = new Span();
        message1.add(checkerSpan);
        message1.add(" is currently checking this compartment.");
        message1.addClassName("dialog-message");

        Span message2 = new Span("Taking over will interrupt their work.");
        message2.addClassName("dialog-message");
        message2.getStyle().set("color", "var(--lumo-secondary-text-color)");

        section.add(warningIcon, message1, message2);
        section.setAlignItems(FlexComponent.Alignment.CENTER);
        return section;
    }
}
