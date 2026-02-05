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
 * Dialog for confirming completion of an inventory check.
 * Shows a summary and asks the user to confirm.
 *
 * <p>Layout:
 * <pre>
 * Complete Check?
 * ----------------------
 * | [Success Icon]                              |
 * | All items have been verified.               |
 * | X items verified, Y issues found.           |
 * ----------------------
 * [CANCEL] [COMPLETE CHECK]
 * </pre>
 */
public class ConfirmCompleteDialog extends Dialog {

    /**
     * Creates a dialog for confirming an inventory check completion.
     *
     * @param verifiedCount the number of items verified
     * @param issuesCount the number of issues found
     * @param onConfirm callback when completion is confirmed
     */
    public ConfirmCompleteDialog(int verifiedCount, int issuesCount, Runnable onConfirm) {
        addClassName("ic-confirm-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);

        // Title
        H3 title = new H3("Complete Check?");
        title.addClassName("dialog-title");
        title.getStyle().set("color", "var(--firestock-present)");

        // Message section
        VerticalLayout messageSection = createMessageSection(verifiedCount, issuesCount);

        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClassName("ic-touch-btn");

        Button completeButton = new Button("Complete Check", new Icon(VaadinIcon.CHECK));
        completeButton.addClassNames("ic-touch-btn");
        completeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        completeButton.addClickListener(e -> {
            onConfirm.run();
            close();
        });

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, completeButton);
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

    private VerticalLayout createMessageSection(int verifiedCount, int issuesCount) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle()
            .set("background-color", "var(--firestock-present-bg, var(--lumo-success-color-10pct))")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("text-align", "center");

        // Success icon
        Icon successIcon = VaadinIcon.CHECK_CIRCLE.create();
        successIcon.getStyle()
            .set("color", "var(--firestock-present)")
            .set("width", "48px")
            .set("height", "48px");

        // Message
        Span message1 = new Span("All items have been verified.");
        message1.addClassName("ic-confirm-dialog-message");

        // Summary
        String summaryText;
        if (issuesCount == 0) {
            summaryText = String.format("%d items verified. No issues found.", verifiedCount);
        } else if (issuesCount == 1) {
            summaryText = String.format("%d items verified. 1 issue found.", verifiedCount);
        } else {
            summaryText = String.format("%d items verified. %d issues found.", verifiedCount, issuesCount);
        }

        Span message2 = new Span(summaryText);
        message2.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)");

        section.add(successIcon, message1, message2);
        section.setAlignItems(FlexComponent.Alignment.CENTER);
        return section;
    }
}
