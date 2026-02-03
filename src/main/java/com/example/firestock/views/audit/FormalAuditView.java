package com.example.firestock.views.audit;

import com.example.firestock.audit.AuditDetails;
import com.example.firestock.audit.AuditItemRequest;
import com.example.firestock.audit.AuditSummary;
import com.example.firestock.audit.AuditableItem;
import com.example.firestock.audit.CompartmentWithAuditableItems;
import com.example.firestock.audit.FormalAuditService;
import com.example.firestock.domain.audit.AuditException;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.jooq.enums.AuditItemStatus;
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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main formal audit view for auditing equipment in compartments.
 * Displays progress, compartment tabs, and item audit list.
 * Restricted to maintenance technicians.
 */
@Route(value = "audit", layout = MainLayout.class)
@PageTitle("Formal Audit | FireStock")
@RolesAllowed("MAINTENANCE_TECHNICIAN")
public class FormalAuditView extends VerticalLayout implements HasUrlParameter<String> {

    private final FormalAuditService auditService;

    private ApparatusId apparatusId;
    private AuditDetails auditDetails;
    private AuditSummary currentAudit;
    private int currentCompartmentIndex = 0;
    private final Set<String> auditedItemIds = new HashSet<>();

    private Span progressText;
    private ProgressBar progressBar;
    private VerticalLayout itemsContainer;
    private Tabs compartmentTabs;
    private Span staleWarning;

    public FormalAuditView(FormalAuditService auditService) {
        this.auditService = auditService;

        addClassName("formal-audit-view");
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
            // Check for existing active audit or start a new one
            var existingAudit = auditService.getActiveAudit(apparatusId);
            if (existingAudit.isPresent()) {
                this.currentAudit = existingAudit.get();
            } else {
                // Start a new audit
                var user = getCurrentUser();
                try {
                    this.currentAudit = auditService.startAudit(apparatusId, user.getUserId());
                } catch (AuditException.ActiveAuditExistsException ex) {
                    // Race condition - another user started an audit
                    this.currentAudit = auditService.getActiveAudit(apparatusId)
                            .orElseThrow(() -> new IllegalStateException("Audit disappeared unexpectedly"));
                    Notification.show("Resuming existing audit started by another technician",
                            3000, Notification.Position.TOP_CENTER);
                }
            }

            // Load full audit details
            this.auditDetails = auditService.getAuditDetails(currentAudit.id());

            // Track already audited items for UI
            for (var compartment : auditDetails.compartments()) {
                for (var item : compartment.items()) {
                    if (item.isAudited()) {
                        auditedItemIds.add(item.uniqueId());
                    }
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
        getUI().ifPresent(ui -> ui.navigate(AuditApparatusSelectionView.class));
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
        add(createItemsSection());
        add(createFooter());
    }

    private HorizontalLayout createHeader() {
        Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClassName("back-button");
        backButton.addClickListener(e -> {
            // Confirm before leaving
            if (auditedItemIds.size() > 0 && auditedItemIds.size() < auditDetails.totalItems()) {
                // Auto-save on exit
                auditService.saveAndExit(currentAudit.id());
                Notification.show("Audit saved. You can resume later.",
                        3000, Notification.Position.TOP_CENTER);
            }
            getUI().ifPresent(ui -> ui.navigate(AuditApparatusSelectionView.class));
        });

        Span title = new Span("Audit: " + auditDetails.unitNumber().value());
        title.addClassName("audit-title");

        HorizontalLayout header = new HorizontalLayout(backButton, title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("audit-header");
        header.setWidthFull();
        header.setPadding(true);

        return header;
    }

    private VerticalLayout createProgressSection() {
        progressText = new Span(getProgressText());
        progressText.addClassName("progress-text");

        progressBar = new ProgressBar();
        progressBar.setValue(getProgressValue());
        progressBar.addClassName("audit-progress-bar");

        staleWarning = new Span();
        staleWarning.addClassName("stale-warning");
        if (currentAudit.isStale()) {
            staleWarning.setText("Warning: This audit is more than 7 days old");
            staleWarning.setVisible(true);
        } else {
            staleWarning.setVisible(false);
        }

        VerticalLayout progressSection = new VerticalLayout(progressText, progressBar, staleWarning);
        progressSection.setPadding(true);
        progressSection.setSpacing(true);
        progressSection.addClassName("progress-section");

        return progressSection;
    }

    private Tabs createCompartmentTabs() {
        compartmentTabs = new Tabs();
        compartmentTabs.addClassName("compartment-tabs");
        compartmentTabs.setWidthFull();

        List<CompartmentWithAuditableItems> compartments = auditDetails.compartments();
        for (CompartmentWithAuditableItems compartment : compartments) {
            String tabLabel = compartment.name();
            if (compartment.isComplete()) {
                tabLabel += " \u2713"; // Checkmark
            }
            Tab tab = new Tab(tabLabel);
            compartmentTabs.add(tab);
        }

        compartmentTabs.addSelectedChangeListener(e -> {
            currentCompartmentIndex = compartmentTabs.getSelectedIndex();
            updateItemsList();
        });

        return compartmentTabs;
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

        List<CompartmentWithAuditableItems> compartments = auditDetails.compartments();
        if (compartments.isEmpty() || currentCompartmentIndex >= compartments.size()) {
            Span noItems = new Span("No items in this compartment");
            noItems.addClassName("text-secondary");
            itemsContainer.add(noItems);
            return;
        }

        CompartmentWithAuditableItems currentCompartment = compartments.get(currentCompartmentIndex);

        for (AuditableItem item : currentCompartment.items()) {
            itemsContainer.add(createItemCard(item, currentCompartment.id()));
        }
    }

    private Div createItemCard(AuditableItem item, CompartmentId compartmentId) {
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

        // Show expiry warning
        if (item.hasExpiryWarning()) {
            Span expirySpan = new Span("Expiry: " + item.expiryDate().toString());
            expirySpan.addClassName("item-expiry-warning");
            itemInfo.add(expirySpan);
        }

        // Show testing info
        if (item.requiresTesting()) {
            String testText = item.isTestingOverdue()
                    ? "Testing OVERDUE"
                    : "Requires testing";
            Span testSpan = new Span(testText);
            testSpan.addClassName(item.isTestingOverdue() ? "test-overdue" : "test-required");
            itemInfo.add(testSpan);
        }

        // Critical badge
        if (item.isCritical()) {
            Span criticalBadge = new Span("CRITICAL");
            criticalBadge.addClassName("critical-badge");
            itemInfo.add(criticalBadge);
        }

        String itemUniqueId = item.uniqueId();
        boolean isAudited = auditedItemIds.contains(itemUniqueId);

        Span statusBadge = createStatusBadge(item, isAudited);
        itemInfo.add(statusBadge);

        HorizontalLayout actionButtons = createActionButtons(item, compartmentId);

        VerticalLayout cardContent = new VerticalLayout(itemInfo, actionButtons);
        cardContent.setPadding(false);
        cardContent.setSpacing(true);

        card.add(cardContent);

        if (isAudited) {
            card.addClassName("item-audited");
        }

        return card;
    }

    private Span createStatusBadge(AuditableItem item, boolean isAudited) {
        Span badge = new Span();
        badge.addClassName("status-badge");

        if (isAudited) {
            badge.setText(item.auditStatus() != null ? item.auditStatus().getLiteral() : "Audited");
            badge.addClassName(getStatusClass(item.auditStatus()));
        } else {
            badge.setText("Not Audited");
            badge.addClassName("status-pending");
        }

        return badge;
    }

    private String getStatusClass(AuditItemStatus status) {
        if (status == null) return "status-verified";
        return switch (status) {
            case VERIFIED -> "status-verified";
            case MISSING -> "status-missing";
            case DAMAGED -> "status-damaged";
            case FAILED_INSPECTION -> "status-failed";
            case EXPIRED -> "status-expired";
            default -> "status-pending";
        };
    }

    private HorizontalLayout createActionButtons(AuditableItem item, CompartmentId compartmentId) {
        Button auditBtn = new Button("Audit", new Icon(VaadinIcon.CLIPBOARD_CHECK));
        auditBtn.addClassName("action-btn");
        auditBtn.addClassName("audit-btn");
        auditBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        auditBtn.addClickListener(e -> openAuditDialog(item, compartmentId));

        // Quick verified button
        Button quickVerifiedBtn = new Button("Quick OK", new Icon(VaadinIcon.CHECK));
        quickVerifiedBtn.addClassName("action-btn");
        quickVerifiedBtn.addClassName("quick-btn");
        quickVerifiedBtn.addClickListener(e -> quickVerify(item, compartmentId));

        HorizontalLayout buttons = new HorizontalLayout(auditBtn, quickVerifiedBtn);
        buttons.addClassName("action-buttons");
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        return buttons;
    }

    private void openAuditDialog(AuditableItem item, CompartmentId compartmentId) {
        AuditItemDialog dialog = new AuditItemDialog(item, result -> {
            recordAudit(item, compartmentId, result);
        });
        dialog.open();
    }

    private void quickVerify(AuditableItem item, CompartmentId compartmentId) {
        AuditItemDialog.AuditItemResult result = new AuditItemDialog.AuditItemResult(
                AuditItemStatus.VERIFIED,
                com.example.firestock.jooq.enums.ItemCondition.GOOD,
                item.requiresTesting() ? com.example.firestock.jooq.enums.TestResult.PASSED : null,
                item.expiryDate() != null ? com.example.firestock.jooq.enums.ExpiryStatus.OK : null,
                null,
                null,
                item.currentQuantity() != null ? item.currentQuantity().value() : null
        );
        recordAudit(item, compartmentId, result);
    }

    private void recordAudit(AuditableItem item, CompartmentId compartmentId,
                             AuditItemDialog.AuditItemResult result) {
        try {
            Quantity qtyFound = result.quantityFound() != null
                    ? new Quantity(result.quantityFound())
                    : null;
            Quantity qtyExpected = item.requiredQuantity() != null
                    ? new Quantity(item.requiredQuantity())
                    : null;

            AuditItemRequest request = new AuditItemRequest(
                    currentAudit.id(),
                    item.equipmentItemId(),
                    item.consumableStockId(),
                    compartmentId,
                    item.manifestEntryId(),
                    result.status(),
                    result.condition(),
                    result.testResult(),
                    result.expiryStatus(),
                    result.conditionNotes(),
                    result.testNotes(),
                    qtyFound,
                    qtyExpected,
                    false // not unexpected
            );

            auditService.auditItem(request, getCurrentUser().getUserId());

            // Track locally for UI
            auditedItemIds.add(item.uniqueId());

            // Refresh the audit summary to get updated counts
            currentAudit = auditService.getAudit(currentAudit.id());
            auditDetails = auditService.getAuditDetails(currentAudit.id());

            updateItemsList();
            updateProgress();
            updateCompartmentTabs();

            String statusLabel = result.status().getLiteral().toLowerCase();
            Notification.show(item.name() + " marked as " + statusLabel,
                    2000, Notification.Position.BOTTOM_CENTER);

        } catch (AuditException.ItemAlreadyAuditedException e) {
            Notification.show("This item has already been audited",
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            auditedItemIds.add(item.uniqueId());
            updateItemsList();
        } catch (Exception e) {
            Notification.show("Error auditing item: " + e.getMessage(),
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateProgress() {
        progressText.setText(getProgressText());
        progressBar.setValue(getProgressValue());
    }

    private void updateCompartmentTabs() {
        // Update tab labels to show completion status
        var tabs = compartmentTabs.getChildren().toList();
        var compartments = auditDetails.compartments();
        for (int i = 0; i < compartments.size() && i < tabs.size(); i++) {
            Tab tab = (Tab) tabs.get(i);
            String label = compartments.get(i).name();
            if (compartments.get(i).isComplete()) {
                label += " \u2713";
            }
            tab.setLabel(label);
        }
    }

    private double getProgressValue() {
        if (currentAudit.totalItems() == 0) return 0;
        return (double) currentAudit.auditedCount() / currentAudit.totalItems();
    }

    private String getProgressText() {
        return String.format("%d of %d items audited • Compartment %d of %d",
                currentAudit.auditedCount(), currentAudit.totalItems(),
                currentCompartmentIndex + 1, auditDetails.compartments().size());
    }

    private HorizontalLayout createFooter() {
        Button summaryButton = new Button("View Summary", new Icon(VaadinIcon.LIST));
        summaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        summaryButton.addClassName("summary-btn");
        summaryButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(
                        "audit/" + apparatusId.toString() + "/summary/" + currentAudit.id().toString()))
        );

        Button saveExitButton = new Button("Save & Exit", new Icon(VaadinIcon.DISC));
        saveExitButton.addClassName("save-exit-btn");
        saveExitButton.addClickListener(e -> {
            auditService.saveAndExit(currentAudit.id());
            Notification.show("Audit saved. You can resume later.",
                    3000, Notification.Position.TOP_CENTER);
            getUI().ifPresent(ui -> ui.navigate(AuditApparatusSelectionView.class));
        });

        HorizontalLayout footer = new HorizontalLayout(saveExitButton, summaryButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        footer.setPadding(true);
        footer.addClassName("audit-footer");

        return footer;
    }
}
