package com.example.firestock.views.issues;

import com.example.firestock.issues.IssueSummary;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Dialog shown when equipment has existing open issues.
 * Allows user to add to an existing issue or create a new one.
 */
class ExistingIssueDialog extends Dialog {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final List<IssueSummary> existingIssues;
    private final ZoneId zoneId;
    private final RadioButtonGroup<IssueSummary> issueGroup;
    private final TextArea additionalNotes;

    public ExistingIssueDialog(
            List<IssueSummary> existingIssues,
            ZoneId zoneId,
            BiConsumer<IssueSummary, String> onAddToExisting,
            Consumer<Void> onCreateNew
    ) {
        this.existingIssues = existingIssues;
        this.zoneId = Objects.requireNonNull(zoneId, "ZoneId cannot be null");

        addClassName("existing-issue-dialog");
        setDraggable(false);
        setCloseOnOutsideClick(false);
        setModal(true);

        // Title
        H3 title = new H3("Existing Issues Found");
        title.addClassName("dialog-title");

        // Info message
        Span infoMessage = new Span(
                "This equipment already has " + existingIssues.size() +
                " open issue(s). You can add information to an existing issue or create a new one."
        );
        infoMessage.addClassName("info-message");

        // Existing issues list
        issueGroup = new RadioButtonGroup<>();
        issueGroup.setLabel("Select an existing issue to update");
        issueGroup.setItems(existingIssues);
        issueGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        issueGroup.setRenderer(new ComponentRenderer<>(this::createIssueItem));
        issueGroup.addClassName("existing-issues-group");

        // Additional notes
        additionalNotes = new TextArea("Additional Notes");
        additionalNotes.setPlaceholder("Add your observations to the selected issue...");
        additionalNotes.setWidthFull();
        additionalNotes.setMinHeight("100px");
        additionalNotes.setVisible(false);
        additionalNotes.addClassName("additional-notes");

        issueGroup.addValueChangeListener(e -> {
            additionalNotes.setVisible(e.getValue() != null);
        });

        // Buttons
        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button createNewButton = new Button("Create New Issue", VaadinIcon.PLUS.create());
        createNewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createNewButton.addClickListener(e -> {
            close();
            onCreateNew.accept(null);
        });

        Button addToExistingButton = new Button("Add to Selected", VaadinIcon.COMMENT.create());
        addToExistingButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        addToExistingButton.addClickListener(e -> handleAddToExisting(onAddToExisting));

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, createNewButton, addToExistingButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.addClassName("dialog-buttons");

        // Assemble dialog content
        VerticalLayout content = new VerticalLayout(
                title,
                infoMessage,
                issueGroup,
                additionalNotes,
                buttons
        );
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("dialog-content");

        add(content);
    }

    private VerticalLayout createIssueItem(IssueSummary issue) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.addClassName("existing-issue-card");
        layout.addClassName("severity-" + issue.severity().getLiteral().toLowerCase());

        // Header row with reference and severity
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

        Span refSpan = new Span(issue.referenceNumber().value());
        refSpan.addClassName("issue-reference");

        Span severityBadge = new Span(issue.severityLabel());
        severityBadge.addClassName("severity-badge");
        severityBadge.addClassName("severity-badge-" + issue.severity().getLiteral().toLowerCase());

        headerRow.add(refSpan, severityBadge);

        // Title
        Span titleSpan = new Span(issue.title());
        titleSpan.addClassName("issue-title");

        // Details row
        HorizontalLayout detailsRow = new HorizontalLayout();
        detailsRow.setSpacing(true);

        Span categorySpan = new Span(issue.categoryLabel());
        categorySpan.addClassName("issue-category");

        Span dateSpan = new Span("Reported: " + LocalDateTime.ofInstant(issue.reportedAt(), zoneId).format(DATE_FORMATTER));
        dateSpan.addClassName("issue-date");

        Span statusSpan = new Span(issue.statusLabel());
        statusSpan.addClassName("issue-status");

        detailsRow.add(categorySpan, dateSpan, statusSpan);

        layout.add(headerRow, titleSpan, detailsRow);
        return layout;
    }

    private void handleAddToExisting(BiConsumer<IssueSummary, String> onAddToExisting) {
        IssueSummary selected = issueGroup.getValue();
        if (selected == null) {
            issueGroup.setInvalid(true);
            issueGroup.setErrorMessage("Please select an issue to update");
            return;
        }

        String notes = additionalNotes.getValue();
        if (notes == null || notes.strip().isEmpty()) {
            additionalNotes.setInvalid(true);
            additionalNotes.setErrorMessage("Please add some notes");
            return;
        }

        close();
        onAddToExisting.accept(selected, notes.strip());
    }
}
