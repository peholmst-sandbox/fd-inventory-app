package com.example.firestock.views.inventorycheck.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;

/**
 * Reusable progress bar component showing verification progress.
 * Displays "X of Y items" text and a visual progress bar.
 *
 * <p>The progress bar changes color when complete (100%).
 */
public class CheckProgressBar extends Div {

    private final Span progressText;
    private final ProgressBar progressBar;
    private final String itemLabel;

    /**
     * Creates a progress bar with custom item label.
     *
     * @param verifiedCount number of verified items
     * @param totalItems total number of items
     * @param itemLabel the label for items (e.g., "items", "compartments")
     */
    public CheckProgressBar(int verifiedCount, int totalItems, String itemLabel) {
        this.itemLabel = itemLabel;
        addClassName("ic-progress-section");

        // Progress text: "Checked: X of Y items"
        progressText = new Span();
        progressText.addClassName("ic-progress-text");

        // Progress bar
        progressBar = new ProgressBar();
        progressBar.addClassName("ic-progress-bar");

        add(progressText, progressBar);

        updateProgress(verifiedCount, totalItems);
    }

    /**
     * Creates a progress bar with "items" as the default label.
     *
     * @param verifiedCount number of verified items
     * @param totalItems total number of items
     */
    public CheckProgressBar(int verifiedCount, int totalItems) {
        this(verifiedCount, totalItems, "items");
    }

    /**
     * Updates the progress display.
     *
     * @param verifiedCount number of verified items
     * @param totalItems total number of items
     */
    public void updateProgress(int verifiedCount, int totalItems) {
        progressText.setText(String.format("Checked: %d of %d %s", verifiedCount, totalItems, itemLabel));

        double progress = totalItems > 0 ? (double) verifiedCount / totalItems : 1.0;
        progressBar.setValue(progress);

        // Add complete class when fully checked
        if (verifiedCount >= totalItems) {
            progressBar.addClassName("complete");
        } else {
            progressBar.removeClassName("complete");
        }
    }

    /**
     * Updates the progress using a percentage (0-100).
     *
     * @param percentage the progress percentage
     */
    public void updateProgress(int percentage) {
        double progress = Math.min(100, Math.max(0, percentage)) / 100.0;
        progressBar.setValue(progress);

        if (percentage >= 100) {
            progressBar.addClassName("complete");
        } else {
            progressBar.removeClassName("complete");
        }
    }
}
