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
import com.vaadin.flow.component.textfield.TextArea;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiConsumer;

/**
 * Card component for displaying consumable items in the Check Compartment view.
 * Shows expected vs actual quantity with stepper controls.
 *
 * <p>Layout:
 * <pre>
 * [Display Name]
 * [Consumable]
 * [Expected: X | Actual: [-] Y [+]] stepper
 * [Status Badge: OK / DISCREPANCY]
 * [Notes field - shown when discrepancy >20%]
 * </pre>
 */
public class ConsumableItemCard extends Div {

    private static final BigDecimal DISCREPANCY_THRESHOLD = new BigDecimal("0.20");

    private final CheckableItemWithStatus itemWithStatus;
    private Span statusBadge;
    private Span actualValueSpan;
    private TextArea notesField;
    private Div notesContainer;
    private BigDecimal actualQuantity;
    private final BigDecimal expectedQuantity;
    private final BiConsumer<CheckableItem, BigDecimal> onQuantityConfirmed;

    /**
     * Creates a consumable item card.
     *
     * @param itemWithStatus the item with its verification status
     * @param onQuantityConfirmed callback when quantity is confirmed (item, actualQuantity)
     */
    public ConsumableItemCard(
            CheckableItemWithStatus itemWithStatus,
            BiConsumer<CheckableItem, BigDecimal> onQuantityConfirmed
    ) {
        this.itemWithStatus = itemWithStatus;
        this.onQuantityConfirmed = onQuantityConfirmed;
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

        // Initialize quantities
        this.expectedQuantity = item.requiredQuantity() != null ? item.requiredQuantity() : BigDecimal.ZERO;
        this.actualQuantity = item.currentQuantity() != null
                ? item.currentQuantity().value()
                : expectedQuantity;

        // Header with name
        Div header = createHeader(item);

        // Type name
        Span typeSpan = new Span("Consumable");
        typeSpan.addClassName("ic-item-type");

        // Quantity section with stepper
        Div quantitySection = createQuantitySection();

        // Status badge
        statusBadge = createStatusBadge();
        updateStatusBadge();

        // Notes field (visible when discrepancy >20%)
        notesContainer = new Div();
        notesContainer.addClassName("notes-container");
        notesField = new TextArea("Notes");
        notesField.setPlaceholder("Explain the quantity discrepancy...");
        notesField.setWidthFull();
        notesField.setMinHeight("80px");
        notesContainer.add(notesField);
        notesContainer.setVisible(isSignificantDiscrepancy());

        // Confirm button for consumables
        Button confirmBtn = new Button("CONFIRM", new Icon(VaadinIcon.CHECK));
        confirmBtn.addClassNames("action-btn", "present-btn");
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmBtn.setWidthFull();
        confirmBtn.addClickListener(e -> handleConfirm());

        // Hide confirm if already verified
        if (itemWithStatus.isVerified()) {
            confirmBtn.setVisible(false);
        }

        add(header, typeSpan, quantitySection, statusBadge, notesContainer, confirmBtn);
    }

    private Div createHeader(CheckableItem item) {
        Div header = new Div();
        header.addClassName("ic-item-header");

        Span nameSpan = new Span(item.name());
        nameSpan.addClassName("ic-item-name");

        header.add(nameSpan);
        return header;
    }

    private Div createQuantitySection() {
        Div section = new Div();
        section.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)")
                .set("margin", "var(--lumo-space-s) 0");

        // Expected quantity display
        Span expectedSpan = new Span("Expected: " + formatQuantity(expectedQuantity));
        expectedSpan.addClassName("quantity-expected");

        // Stepper for actual quantity
        Div stepper = new Div();
        stepper.addClassName("quantity-stepper");

        Button decreaseBtn = new Button(new Icon(VaadinIcon.MINUS));
        decreaseBtn.addClassName("quantity-stepper-btn");
        decreaseBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        decreaseBtn.addClickListener(e -> adjustQuantity(-1));

        actualValueSpan = new Span(formatQuantity(actualQuantity));
        actualValueSpan.addClassName("quantity-stepper-value");

        Button increaseBtn = new Button(new Icon(VaadinIcon.PLUS));
        increaseBtn.addClassName("quantity-stepper-btn");
        increaseBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        increaseBtn.addClickListener(e -> adjustQuantity(1));

        stepper.add(decreaseBtn, actualValueSpan, increaseBtn);

        section.add(expectedSpan, stepper);
        return section;
    }

    private Span createStatusBadge() {
        Span badge = new Span();
        badge.addClassName("status-badge");
        return badge;
    }

    private void updateStatusBadge() {
        statusBadge.removeClassNames("status-present", "status-damaged", "status-unchecked");

        if (itemWithStatus.isVerified()) {
            if (hasDiscrepancy()) {
                statusBadge.setText("DISCREPANCY");
                statusBadge.addClassName("status-damaged");
            } else {
                statusBadge.setText("OK");
                statusBadge.addClassName("status-present");
            }
        } else {
            if (hasDiscrepancy()) {
                statusBadge.setText("DISCREPANCY");
                statusBadge.addClassName("status-damaged");
            } else {
                statusBadge.setText("OK");
                statusBadge.addClassName("status-present");
            }
        }
    }

    private void adjustQuantity(int delta) {
        BigDecimal newQuantity = actualQuantity.add(BigDecimal.valueOf(delta));
        if (newQuantity.compareTo(BigDecimal.ZERO) >= 0) {
            actualQuantity = newQuantity;
            actualValueSpan.setText(formatQuantity(actualQuantity));
            updateStatusBadge();
            notesContainer.setVisible(isSignificantDiscrepancy());
        }
    }

    private boolean hasDiscrepancy() {
        return actualQuantity.compareTo(expectedQuantity) != 0;
    }

    private boolean isSignificantDiscrepancy() {
        if (expectedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return actualQuantity.compareTo(BigDecimal.ZERO) != 0;
        }
        BigDecimal difference = expectedQuantity.subtract(actualQuantity).abs();
        BigDecimal discrepancyPercent = difference.divide(expectedQuantity, 2, RoundingMode.HALF_UP);
        return discrepancyPercent.compareTo(DISCREPANCY_THRESHOLD) > 0;
    }

    private void handleConfirm() {
        // BR-05: Check if notes are required for >20% discrepancy
        if (isSignificantDiscrepancy() &&
                (notesField.getValue() == null || notesField.getValue().isBlank())) {
            notesField.setInvalid(true);
            notesField.setErrorMessage("Notes are required for discrepancies greater than 20%");
            return;
        }

        onQuantityConfirmed.accept(itemWithStatus.item(), actualQuantity);
    }

    /**
     * Returns the item with status associated with this card.
     */
    public CheckableItemWithStatus getItemWithStatus() {
        return itemWithStatus;
    }

    /**
     * Returns the current actual quantity entered by the user.
     */
    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    /**
     * Returns the notes entered by the user.
     */
    public String getNotes() {
        return notesField.getValue();
    }

    private String formatQuantity(BigDecimal qty) {
        if (qty == null) return "0";
        // Remove trailing zeros
        return qty.stripTrailingZeros().toPlainString();
    }

    /**
     * Updates the card to show as verified.
     *
     * @param status the verification status
     */
    public void updateStatus(VerificationStatus status) {
        if (status != null) {
            addClassName("verified");
            // Hide the confirm button by finding and hiding all action buttons
            getChildren()
                    .filter(c -> c instanceof Button)
                    .forEach(c -> c.setVisible(false));
        }
        updateStatusBadge();
    }
}
