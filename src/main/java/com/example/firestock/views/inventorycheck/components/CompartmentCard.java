package com.example.firestock.views.inventorycheck.components;

import com.example.firestock.inventorycheck.CompartmentCheckProgress;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;

/**
 * Card component displaying compartment information with check progress.
 * Shows compartment code, name, item count, progress bar, and current checker (if any).
 *
 * <p>Visual states:
 * <ul>
 *   <li>Default: Standard card appearance</li>
 *   <li>Complete: Green left border indicating all items verified</li>
 *   <li>In Progress: Blue left border indicating someone is checking</li>
 *   <li>Locked: Slightly dimmed, showing checker name</li>
 * </ul>
 */
public class CompartmentCard extends Div {

    private final CompartmentCheckProgress compartment;

    /**
     * Creates a compartment card for the given progress data.
     *
     * @param compartment the compartment progress data
     */
    public CompartmentCard(CompartmentCheckProgress compartment) {
        this.compartment = compartment;

        addClassName("compartment-card");

        // Apply state-based styling
        if (compartment.isFullyChecked()) {
            addClassName("complete");
        } else if (compartment.currentCheckerName() != null) {
            addClassName("in-progress");
            addClassName("locked");
        }

        // Header: Code + Name + Checker badge (if someone is checking)
        Div header = createHeader();

        // Progress: "X of Y items" with progress bar
        Div progressSection = createProgressSection();

        add(header, progressSection);
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassName("compartment-card-header");

        // Compartment code (e.g., "A1")
        Span codeSpan = new Span(compartment.code());
        codeSpan.addClassName("compartment-code");

        // Compartment name (e.g., "Driver Side Front")
        Span nameSpan = new Span(compartment.name());
        nameSpan.addClassName("compartment-name");

        header.add(codeSpan, nameSpan);

        // Checker badge if someone is checking
        if (compartment.currentCheckerName() != null) {
            Span checkerBadge = new Span();
            checkerBadge.addClassName("compartment-checker");
            checkerBadge.setText(compartment.currentCheckerName());
            header.add(checkerBadge);
        }

        // Checkmark icon if complete
        if (compartment.isFullyChecked()) {
            Icon checkIcon = VaadinIcon.CHECK_CIRCLE.create();
            checkIcon.getStyle()
                .set("color", "var(--firestock-present)")
                .set("flex-shrink", "0");
            header.add(checkIcon);
        }

        return header;
    }

    private Div createProgressSection() {
        Div progressSection = new Div();
        progressSection.addClassName("compartment-progress");

        // Progress text: "X of Y"
        Span progressText = new Span(
            String.format("%d of %d", compartment.verifiedCount(), compartment.totalItems())
        );
        progressText.addClassName("compartment-progress-text");

        // Progress bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.addClassName("ic-progress-bar");
        progressBar.getStyle().set("flex-grow", "1");

        double progress = compartment.totalItems() > 0
            ? (double) compartment.verifiedCount() / compartment.totalItems()
            : 1.0;
        progressBar.setValue(progress);

        if (compartment.isFullyChecked()) {
            progressBar.addClassName("complete");
        }

        progressSection.add(progressText, progressBar);

        return progressSection;
    }

    /**
     * Returns the compartment data associated with this card.
     */
    public CompartmentCheckProgress getCompartment() {
        return compartment;
    }

    /**
     * Adds a click listener to the card.
     *
     * @param listener the click listener
     */
    public void addCardClickListener(ComponentEventListener<ClickEvent<Div>> listener) {
        addClickListener(listener);
    }
}
