package com.example.firestock.views.inventorycheck.components;

import com.example.firestock.inventorycheck.ActiveCheckInfo;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.time.Duration;
import java.time.Instant;

/**
 * Banner component for resuming an in-progress inventory check.
 * Displays the apparatus being checked, progress, and elapsed time.
 */
public class ResumeBanner extends Div {

    private final ActiveCheckInfo checkInfo;

    public ResumeBanner(ActiveCheckInfo checkInfo) {
        this.checkInfo = checkInfo;
        addClassName("resume-banner");

        // Icon
        Icon playIcon = VaadinIcon.PLAY_CIRCLE.create();
        playIcon.addClassName("resume-banner-icon");

        // Text content
        Div textContent = new Div();
        textContent.addClassName("resume-banner-text");

        Span title = new Span("Resume your check on " + checkInfo.apparatusUnitNumber());
        title.addClassName("resume-banner-title");

        Span subtitle = new Span(buildSubtitleText());
        subtitle.addClassName("resume-banner-subtitle");

        textContent.add(title, subtitle);

        // Arrow icon
        Icon arrowIcon = VaadinIcon.ANGLE_RIGHT.create();
        arrowIcon.addClassName("resume-banner-action");

        add(playIcon, textContent, arrowIcon);
    }

    private String buildSubtitleText() {
        int progress = checkInfo.progressPercentage();
        String elapsedTime = formatElapsedTime(checkInfo.startedAt());
        return String.format("%d%% complete - Started %s ago", progress, elapsedTime);
    }

    private String formatElapsedTime(Instant startedAt) {
        Duration elapsed = Duration.between(startedAt, Instant.now());
        long hours = elapsed.toHours();
        long minutes = elapsed.toMinutesPart();

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm", minutes);
        } else {
            return "just now";
        }
    }

    /**
     * Returns the active check info associated with this banner.
     */
    public ActiveCheckInfo getCheckInfo() {
        return checkInfo;
    }

    /**
     * Adds a click listener to the banner.
     */
    public void addResumeClickListener(ComponentEventListener<ClickEvent<Div>> listener) {
        addClickListener(listener);
    }
}
