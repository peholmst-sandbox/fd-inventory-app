package com.example.firestock.views.inventorycheck.components;

import com.example.firestock.inventorycheck.CheckableItem;
import com.example.firestock.inventorycheck.CheckableItemWithStatus;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.function.Consumer;

/**
 * Card component for displaying equipment items in the Check Compartment view.
 * Shows item details and action buttons for marking status (Present/Missing/Damaged).
 *
 * <p>Layout:
 * <pre>
 * [Display Name]                     [CREW badge if crew-owned]
 * [Type Name]
 * [S/N: Serial Number]
 * [Status Badge: NOT CHECKED / PRESENT / MISSING / DAMAGED]
 * [PRESENT] [MISSING] [DAMAGED] buttons
 * </pre>
 */
public class EquipmentItemCard extends Div {

    private final CheckableItemWithStatus itemWithStatus;
    private final Span statusBadge;
    private final HorizontalLayout actionButtons;

    /**
     * Creates an equipment item card.
     *
     * @param itemWithStatus the item with its verification status
     * @param onPresent callback when Present button is clicked
     * @param onMissing callback when Missing button is clicked
     * @param onDamaged callback when Damaged button is clicked
     */
    public EquipmentItemCard(
            CheckableItemWithStatus itemWithStatus,
            Consumer<CheckableItem> onPresent,
            Consumer<CheckableItem> onMissing,
            Consumer<CheckableItem> onDamaged
    ) {
        this.itemWithStatus = itemWithStatus;
        CheckableItem item = itemWithStatus.item();

        addClassName("ic-item-card");

        // Apply state-based styling
        if (itemWithStatus.isVerified()) {
            addClassName("verified");
            if (itemWithStatus.hasIssue()) {
                removeClassName("verified");
                addClassName("issue");
            }
        }

        // Header with name and CREW badge
        Div header = createHeader(item);

        // Type name
        Span typeSpan = new Span(item.typeName());
        typeSpan.addClassName("ic-item-type");

        // Serial number
        Div serialDiv = new Div();
        if (item.serialNumber() != null) {
            Span serialSpan = new Span("S/N: " + item.serialNumber().value());
            serialSpan.addClassName("ic-item-serial");
            serialDiv.add(serialSpan);
        }

        // Status badge
        statusBadge = createStatusBadge(itemWithStatus.verificationStatus());

        // Action buttons (only show if not verified)
        actionButtons = createActionButtons(item, onPresent, onMissing, onDamaged);
        if (itemWithStatus.isVerified()) {
            actionButtons.setVisible(false);
        }

        add(header, typeSpan, serialDiv, statusBadge, actionButtons);
    }

    private Div createHeader(CheckableItem item) {
        Div header = new Div();
        header.addClassName("ic-item-header");

        Span nameSpan = new Span(item.name());
        nameSpan.addClassName("ic-item-name");

        header.add(nameSpan);

        // TODO: Add CREW badge when crew-owned equipment is supported
        // For now, we can check if there's a way to determine crew ownership
        // if (item.isCrewOwned()) {
        //     Span crewBadge = new Span("CREW");
        //     crewBadge.addClassName("ic-item-badge-crew");
        //     header.add(crewBadge);
        // }

        return header;
    }

    private Span createStatusBadge(VerificationStatus status) {
        Span badge = new Span();
        badge.addClassName("status-badge");

        if (status == null) {
            badge.setText("NOT CHECKED");
            badge.addClassName("status-unchecked");
        } else {
            switch (status) {
                case PRESENT -> {
                    badge.setText("PRESENT");
                    badge.addClassName("status-present");
                }
                case MISSING -> {
                    badge.setText("MISSING");
                    badge.addClassName("status-missing");
                }
                case PRESENT_DAMAGED -> {
                    badge.setText("DAMAGED");
                    badge.addClassName("status-damaged");
                }
                case EXPIRED -> {
                    badge.setText("EXPIRED");
                    badge.addClassName("status-damaged");
                }
                case LOW_QUANTITY -> {
                    badge.setText("LOW QUANTITY");
                    badge.addClassName("status-damaged");
                }
                case SKIPPED -> {
                    badge.setText("SKIPPED");
                    badge.addClassName("status-unchecked");
                }
            }
        }

        return badge;
    }

    private HorizontalLayout createActionButtons(
            CheckableItem item,
            Consumer<CheckableItem> onPresent,
            Consumer<CheckableItem> onMissing,
            Consumer<CheckableItem> onDamaged
    ) {
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addClassName("ic-item-actions");
        buttons.setWidthFull();

        Button presentBtn = new Button("PRESENT", new Icon(VaadinIcon.CHECK));
        presentBtn.addClassNames("action-btn", "present-btn");
        presentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        presentBtn.addClickListener(e -> onPresent.accept(item));

        Button missingBtn = new Button("MISSING", new Icon(VaadinIcon.CLOSE));
        missingBtn.addClassNames("action-btn", "missing-btn");
        missingBtn.addClickListener(e -> onMissing.accept(item));

        Button damagedBtn = new Button("DAMAGED", new Icon(VaadinIcon.WARNING));
        damagedBtn.addClassNames("action-btn", "damaged-btn");
        damagedBtn.addClickListener(e -> onDamaged.accept(item));

        buttons.add(presentBtn, missingBtn, damagedBtn);
        return buttons;
    }

    /**
     * Returns the item with status associated with this card.
     */
    public CheckableItemWithStatus getItemWithStatus() {
        return itemWithStatus;
    }

    /**
     * Updates the card to show the item as verified with the given status.
     *
     * @param status the new verification status
     */
    public void updateStatus(VerificationStatus status) {
        // Update status badge
        statusBadge.removeClassNames("status-unchecked", "status-present", "status-missing", "status-damaged");

        if (status == null) {
            statusBadge.setText("NOT CHECKED");
            statusBadge.addClassName("status-unchecked");
            actionButtons.setVisible(true);
            removeClassName("verified");
            removeClassName("issue");
        } else {
            switch (status) {
                case PRESENT -> {
                    statusBadge.setText("PRESENT");
                    statusBadge.addClassName("status-present");
                    addClassName("verified");
                    removeClassName("issue");
                }
                case MISSING -> {
                    statusBadge.setText("MISSING");
                    statusBadge.addClassName("status-missing");
                    addClassName("issue");
                    removeClassName("verified");
                }
                case PRESENT_DAMAGED -> {
                    statusBadge.setText("DAMAGED");
                    statusBadge.addClassName("status-damaged");
                    addClassName("issue");
                    removeClassName("verified");
                }
                case EXPIRED -> {
                    statusBadge.setText("EXPIRED");
                    statusBadge.addClassName("status-damaged");
                    addClassName("issue");
                    removeClassName("verified");
                }
                case LOW_QUANTITY -> {
                    statusBadge.setText("LOW QUANTITY");
                    statusBadge.addClassName("status-damaged");
                    addClassName("issue");
                    removeClassName("verified");
                }
                case SKIPPED -> {
                    statusBadge.setText("SKIPPED");
                    statusBadge.addClassName("status-unchecked");
                    addClassName("verified");
                    removeClassName("issue");
                }
            }
            actionButtons.setVisible(false);
        }
    }
}
