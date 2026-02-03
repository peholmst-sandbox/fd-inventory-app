package com.example.firestock.views.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.inventorycheck.ApparatusDetails;
import com.example.firestock.inventorycheck.CheckableItem;
import com.example.firestock.inventorycheck.CompartmentWithItems;
import com.example.firestock.inventorycheck.InventoryCheckSummary;
import com.example.firestock.inventorycheck.ItemVerificationRequest;
import com.example.firestock.inventorycheck.ShiftInventoryCheckService;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main inventory check view for verifying equipment in compartments.
 * Displays progress, compartment tabs, and item verification list.
 */
@Route(value = "check", layout = MainLayout.class)
@PageTitle("Inventory Check | FireStock")
@PermitAll
public class InventoryCheckView extends VerticalLayout implements HasUrlParameter<String> {

    private final ShiftInventoryCheckService inventoryCheckService;

    private ApparatusId apparatusId;
    private ApparatusDetails apparatusDetails;
    private InventoryCheckSummary currentCheck;
    private int currentCompartmentIndex = 0;
    private final Set<String> verifiedItemIds = new HashSet<>();

    private Span progressText;
    private ProgressBar progressBar;
    private VerticalLayout itemsContainer;
    private Tabs compartmentTabs;

    public InventoryCheckView(ShiftInventoryCheckService inventoryCheckService) {
        this.inventoryCheckService = inventoryCheckService;

        addClassName("inventory-check-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            this.apparatusId = new ApparatusId(UUID.fromString(parameter));
        } catch (IllegalArgumentException e) {
            showErrorAndNavigateBack("Invalid apparatus ID");
            return;
        }

        try {
            // Load apparatus details
            this.apparatusDetails = inventoryCheckService.getApparatusDetails(apparatusId);

            // Check for existing active check or start a new one
            var existingCheck = inventoryCheckService.getActiveCheck(apparatusId);
            if (existingCheck.isPresent()) {
                this.currentCheck = existingCheck.get();
                // Restore verified items count from the existing check
                // Note: We track verified items locally for UI purposes
            } else {
                // Start a new check
                var user = getCurrentUser();
                try {
                    this.currentCheck = inventoryCheckService.startCheck(apparatusId, user.getUserId());
                } catch (ShiftInventoryCheckService.ActiveCheckExistsException ex) {
                    // Race condition - another user started a check
                    this.currentCheck = inventoryCheckService.getActiveCheck(apparatusId)
                            .orElseThrow(() -> new IllegalStateException("Check disappeared unexpectedly"));
                    Notification.show("Resuming existing check started by another user",
                            3000, Notification.Position.TOP_CENTER);
                }
            }

            buildUI();
        } catch (AccessDeniedException e) {
            showErrorAndNavigateBack("You don't have access to this apparatus");
        } catch (IllegalArgumentException e) {
            showErrorAndNavigateBack("Apparatus not found");
        }
    }

    private void showErrorAndNavigateBack(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        getUI().ifPresent(ui -> ui.navigate(ApparatusSelectionView.class));
    }

    private FirestockUserDetails getCurrentUser() {
        return (FirestockUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    private void buildUI() {
        removeAll();

        add(createHeader());
        add(createProgressSection());
        add(createCompartmentTabs());
        add(createBarcodeSection());
        add(createItemsSection());
        add(createFooter());
    }

    private HorizontalLayout createHeader() {
        Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassName("back-button");
        backButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(ApparatusSelectionView.class))
        );

        Span title = new Span(apparatusDetails.unitNumber().value());
        title.addClassName("check-title");

        HorizontalLayout header = new HorizontalLayout(backButton, title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("check-header");
        header.setWidthFull();
        header.setPadding(true);

        return header;
    }

    private VerticalLayout createProgressSection() {
        progressText = new Span(getProgressText());
        progressText.addClassName("progress-text");

        progressBar = new ProgressBar();
        progressBar.setValue(getProgressValue());
        progressBar.addClassName("check-progress-bar");

        VerticalLayout progressSection = new VerticalLayout(progressText, progressBar);
        progressSection.setPadding(true);
        progressSection.setSpacing(true);
        progressSection.addClassName("progress-section");

        return progressSection;
    }

    private Tabs createCompartmentTabs() {
        compartmentTabs = new Tabs();
        compartmentTabs.addClassName("compartment-tabs");
        compartmentTabs.setWidthFull();

        List<CompartmentWithItems> compartments = apparatusDetails.compartments();
        for (CompartmentWithItems compartment : compartments) {
            Tab tab = new Tab(compartment.name());
            compartmentTabs.add(tab);
        }

        compartmentTabs.addSelectedChangeListener(e -> {
            currentCompartmentIndex = compartmentTabs.getSelectedIndex();
            updateItemsList();
        });

        return compartmentTabs;
    }

    private HorizontalLayout createBarcodeSection() {
        TextField barcodeField = new TextField();
        barcodeField.setPlaceholder("Enter barcode or serial number");
        barcodeField.addClassName("barcode-field");
        barcodeField.setWidthFull();

        Button scanButton = new Button("Scan", new Icon(VaadinIcon.BARCODE));
        scanButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        scanButton.addClassName("scan-button");
        // TODO: Implement barcode scanning functionality

        HorizontalLayout barcodeSection = new HorizontalLayout(barcodeField, scanButton);
        barcodeSection.setWidthFull();
        barcodeSection.setPadding(true);
        barcodeSection.addClassName("barcode-section");
        barcodeSection.setFlexGrow(1, barcodeField);

        return barcodeSection;
    }

    private VerticalLayout createItemsSection() {
        itemsContainer = new VerticalLayout();
        itemsContainer.setPadding(true);
        itemsContainer.setSpacing(true);
        itemsContainer.addClassName("items-container");

        updateItemsList();

        return itemsContainer;
    }

    private void updateItemsList() {
        itemsContainer.removeAll();

        List<CompartmentWithItems> compartments = apparatusDetails.compartments();
        if (compartments.isEmpty() || currentCompartmentIndex >= compartments.size()) {
            Span noItems = new Span("No items in this compartment");
            noItems.addClassName("text-secondary");
            itemsContainer.add(noItems);
            return;
        }

        CompartmentWithItems currentCompartment = compartments.get(currentCompartmentIndex);

        for (CheckableItem item : currentCompartment.items()) {
            itemsContainer.add(createItemCard(item, currentCompartment.id()));
        }
    }

    private Div createItemCard(CheckableItem item, CompartmentId compartmentId) {
        Div card = new Div();
        card.addClassName("item-card");

        H3 itemName = new H3(item.name());
        itemName.addClassName("item-name");

        Span itemType = new Span(item.typeName());
        itemType.addClassName("item-type");

        VerticalLayout itemInfo = new VerticalLayout(itemName, itemType);
        itemInfo.setPadding(false);
        itemInfo.setSpacing(false);

        // Add serial number for equipment
        if (item.serialNumber() != null) {
            Span serialNumber = new Span("S/N: " + item.serialNumber().value());
            serialNumber.addClassName("item-serial");
            itemInfo.add(serialNumber);
        }

        // Add quantity info for consumables
        if (item.isConsumable() && item.requiredQuantity() != null) {
            String qtyText = item.currentQuantity() != null
                    ? String.format("Qty: %s / %s", item.currentQuantity().value(), item.requiredQuantity())
                    : String.format("Expected: %s", item.requiredQuantity());
            Span quantitySpan = new Span(qtyText);
            quantitySpan.addClassName("item-quantity");
            itemInfo.add(quantitySpan);
        }

        // Show expiry date if applicable
        if (item.expiryDate() != null) {
            Span expirySpan = new Span("Expires: " + item.expiryDate().toString());
            expirySpan.addClassName("item-expiry");
            itemInfo.add(expirySpan);
        }

        String itemUniqueId = getItemUniqueId(item);
        boolean isVerified = verifiedItemIds.contains(itemUniqueId);

        Span statusBadge = createStatusBadge(isVerified);
        itemInfo.add(statusBadge);

        HorizontalLayout actionButtons = createActionButtons(item, compartmentId);

        VerticalLayout cardContent = new VerticalLayout(itemInfo, actionButtons);
        cardContent.setPadding(false);
        cardContent.setSpacing(true);

        card.add(cardContent);

        if (isVerified) {
            card.addClassName("item-verified");
        }

        return card;
    }

    private String getItemUniqueId(CheckableItem item) {
        if (item.equipmentItemId() != null) {
            return "eq-" + item.equipmentItemId().toString();
        } else {
            return "cs-" + item.consumableStockId().toString();
        }
    }

    private Span createStatusBadge(boolean isVerified) {
        Span badge = new Span(isVerified ? "Checked" : "Not Checked");
        badge.addClassName("status-badge");
        badge.addClassName(isVerified ? "status-present" : "status-unchecked");
        return badge;
    }

    private HorizontalLayout createActionButtons(CheckableItem item, CompartmentId compartmentId) {
        Button presentBtn = new Button("Present", new Icon(VaadinIcon.CHECK));
        presentBtn.addClassName("action-btn");
        presentBtn.addClassName("present-btn");
        presentBtn.addClickListener(e -> markItem(item, compartmentId, VerificationStatus.PRESENT, null, null));

        Button missingBtn = new Button("Missing", new Icon(VaadinIcon.CLOSE));
        missingBtn.addClassName("action-btn");
        missingBtn.addClassName("missing-btn");
        missingBtn.addClickListener(e -> {
            ItemVerificationDialog dialog = new ItemVerificationDialog(
                    item, VerificationStatus.MISSING,
                    result -> markItem(item, compartmentId, result.status(), result.notes(), result.quantityFound())
            );
            dialog.open();
        });

        Button damagedBtn = new Button("Damaged", new Icon(VaadinIcon.WARNING));
        damagedBtn.addClassName("action-btn");
        damagedBtn.addClassName("damaged-btn");
        damagedBtn.addClickListener(e -> {
            ItemVerificationDialog dialog = new ItemVerificationDialog(
                    item, VerificationStatus.PRESENT_DAMAGED,
                    result -> markItem(item, compartmentId, result.status(), result.notes(), result.quantityFound())
            );
            dialog.open();
        });

        HorizontalLayout buttons = new HorizontalLayout(presentBtn, missingBtn, damagedBtn);
        buttons.addClassName("action-buttons");
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        return buttons;
    }

    private void markItem(CheckableItem item, CompartmentId compartmentId,
                          VerificationStatus status, String notes, BigDecimal quantityFound) {
        try {
            Quantity qtyFound = quantityFound != null ? new Quantity(quantityFound) : null;
            Quantity qtyExpected = item.requiredQuantity() != null
                    ? new Quantity(item.requiredQuantity())
                    : null;

            ItemVerificationRequest request = new ItemVerificationRequest(
                    currentCheck.id(),
                    item.equipmentItemId(),
                    item.consumableStockId(),
                    compartmentId,
                    null, // manifestEntryId - not used in basic flow
                    status,
                    notes,
                    qtyFound,
                    qtyExpected
            );

            inventoryCheckService.verifyItem(request, getCurrentUser().getUserId());

            // Track locally for UI
            String itemUniqueId = getItemUniqueId(item);
            verifiedItemIds.add(itemUniqueId);

            // Refresh the check summary to get updated counts
            currentCheck = inventoryCheckService.getCheck(currentCheck.id());

            updateItemsList();
            updateProgress();

            String statusLabel = switch (status) {
                case PRESENT -> "present";
                case MISSING -> "missing";
                case PRESENT_DAMAGED -> "damaged";
                default -> status.getLiteral().toLowerCase();
            };
            Notification.show(item.name() + " marked as " + statusLabel,
                    2000, Notification.Position.BOTTOM_CENTER);

        } catch (ShiftInventoryCheckService.ItemAlreadyVerifiedException e) {
            Notification.show("This item has already been verified",
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            verifiedItemIds.add(getItemUniqueId(item));
            updateItemsList();
        } catch (ShiftInventoryCheckService.QuantityDiscrepancyRequiresNotesException e) {
            Notification.show("Notes are required for quantity discrepancies greater than 20%",
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show("Error verifying item: " + e.getMessage(),
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateProgress() {
        progressText.setText(getProgressText());
        progressBar.setValue(getProgressValue());
    }

    private double getProgressValue() {
        if (currentCheck.totalItems() == 0) return 0;
        return (double) currentCheck.verifiedCount() / currentCheck.totalItems();
    }

    private String getProgressText() {
        return String.format("%d of %d items • Compartment %d of %d",
                currentCheck.verifiedCount(), currentCheck.totalItems(),
                currentCompartmentIndex + 1, apparatusDetails.compartments().size());
    }

    private HorizontalLayout createFooter() {
        Button summaryButton = new Button("View Summary", new Icon(VaadinIcon.LIST));
        summaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        summaryButton.addClassName("summary-btn");
        summaryButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(
                        "check/" + apparatusId.toString() + "/summary/" + currentCheck.id().toString()))
        );

        HorizontalLayout footer = new HorizontalLayout(summaryButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        footer.setPadding(true);
        footer.addClassName("check-footer");

        return footer;
    }
}
