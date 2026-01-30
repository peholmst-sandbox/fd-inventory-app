package com.example.firestock.views.inventorycheck;

import com.example.firestock.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main inventory check view for verifying equipment in compartments.
 * Displays progress, compartment tabs, and item verification list.
 */
@Route(value = "check", layout = MainLayout.class)
@PageTitle("Inventory Check | FireStock")
public class InventoryCheckView extends VerticalLayout implements HasUrlParameter<String> {

    private String apparatusId;
    private String apparatusName;
    private int currentCompartmentIndex = 0;
    private final Map<String, ItemStatus> itemStatuses = new HashMap<>();

    private Span progressText;
    private ProgressBar progressBar;
    private VerticalLayout itemsContainer;
    private Tabs compartmentTabs;

    private List<CompartmentData> compartments;
    private int totalItems;
    private int checkedItems = 0;

    public InventoryCheckView() {
        addClassName("inventory-check-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.apparatusId = parameter;
        this.apparatusName = getApparatusName(parameter);
        this.compartments = getMockCompartments();
        this.totalItems = compartments.stream().mapToInt(c -> c.items().size()).sum();

        buildUI();
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

        Span title = new Span(apparatusName);
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
        progressBar.setValue(0);
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

        for (int i = 0; i < compartments.size(); i++) {
            Tab tab = new Tab(compartments.get(i).name());
            compartmentTabs.add(tab);
        }

        compartmentTabs.addSelectedChangeListener(e -> {
            currentCompartmentIndex = compartmentTabs.getSelectedIndex();
            updateItemsList();
            updateProgress();
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

        CompartmentData currentCompartment = compartments.get(currentCompartmentIndex);

        for (EquipmentItem item : currentCompartment.items()) {
            itemsContainer.add(createItemCard(item));
        }
    }

    private Div createItemCard(EquipmentItem item) {
        Div card = new Div();
        card.addClassName("item-card");

        H3 itemName = new H3(item.name());
        itemName.addClassName("item-name");

        Span itemType = new Span(item.type());
        itemType.addClassName("item-type");

        Span serialNumber = new Span("S/N: " + item.serialNumber());
        serialNumber.addClassName("item-serial");

        ItemStatus status = itemStatuses.getOrDefault(item.id(), ItemStatus.UNCHECKED);
        Span statusBadge = createStatusBadge(status);

        VerticalLayout itemInfo = new VerticalLayout(itemName, itemType, serialNumber, statusBadge);
        itemInfo.setPadding(false);
        itemInfo.setSpacing(false);

        HorizontalLayout actionButtons = createActionButtons(item);

        VerticalLayout cardContent = new VerticalLayout(itemInfo, actionButtons);
        cardContent.setPadding(false);
        cardContent.setSpacing(true);

        card.add(cardContent);

        return card;
    }

    private Span createStatusBadge(ItemStatus status) {
        Span badge = new Span(status.label);
        badge.addClassName("status-badge");
        badge.addClassName("status-" + status.cssClass);
        return badge;
    }

    private HorizontalLayout createActionButtons(EquipmentItem item) {
        Button presentBtn = new Button("Present", new Icon(VaadinIcon.CHECK));
        presentBtn.addClassName("action-btn");
        presentBtn.addClassName("present-btn");
        presentBtn.addClickListener(e -> markItem(item, ItemStatus.PRESENT));

        Button missingBtn = new Button("Missing", new Icon(VaadinIcon.CLOSE));
        missingBtn.addClassName("action-btn");
        missingBtn.addClassName("missing-btn");
        missingBtn.addClickListener(e -> {
            ItemVerificationDialog dialog = new ItemVerificationDialog(
                    item.name(), item.type(), item.serialNumber(), ItemStatus.MISSING,
                    notes -> markItem(item, ItemStatus.MISSING)
            );
            dialog.open();
        });

        Button damagedBtn = new Button("Damaged", new Icon(VaadinIcon.WARNING));
        damagedBtn.addClassName("action-btn");
        damagedBtn.addClassName("damaged-btn");
        damagedBtn.addClickListener(e -> {
            ItemVerificationDialog dialog = new ItemVerificationDialog(
                    item.name(), item.type(), item.serialNumber(), ItemStatus.DAMAGED,
                    notes -> markItem(item, ItemStatus.DAMAGED)
            );
            dialog.open();
        });

        HorizontalLayout buttons = new HorizontalLayout(presentBtn, missingBtn, damagedBtn);
        buttons.addClassName("action-buttons");
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        return buttons;
    }

    private void markItem(EquipmentItem item, ItemStatus status) {
        ItemStatus previousStatus = itemStatuses.get(item.id());
        itemStatuses.put(item.id(), status);

        if (previousStatus == null || previousStatus == ItemStatus.UNCHECKED) {
            checkedItems++;
        }

        updateItemsList();
        updateProgress();
    }

    private void updateProgress() {
        progressText.setText(getProgressText());
        progressBar.setValue((double) checkedItems / totalItems);
    }

    private String getProgressText() {
        return String.format("%d of %d items • Compartment %d of %d",
                checkedItems, totalItems,
                currentCompartmentIndex + 1, compartments.size());
    }

    private HorizontalLayout createFooter() {
        Button summaryButton = new Button("View Summary", new Icon(VaadinIcon.LIST));
        summaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        summaryButton.addClassName("summary-btn");
        summaryButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("check/" + apparatusId + "/summary"))
        );

        HorizontalLayout footer = new HorizontalLayout(summaryButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        footer.setPadding(true);
        footer.addClassName("check-footer");

        return footer;
    }

    private String getApparatusName(String id) {
        return switch (id) {
            case "1" -> "Engine 1";
            case "2" -> "Ladder 2";
            case "3" -> "Rescue 3";
            default -> "Unknown Apparatus";
        };
    }

    private List<CompartmentData> getMockCompartments() {
        return List.of(
                new CompartmentData("1", "Driver Side", List.of(
                        new EquipmentItem("101", "SCBA Pack #1", "SCBA", "SCBA-2024-001"),
                        new EquipmentItem("102", "SCBA Pack #2", "SCBA", "SCBA-2024-002"),
                        new EquipmentItem("103", "Halligan Bar", "Forcible Entry", "HB-2023-015"),
                        new EquipmentItem("104", "Flat Head Axe", "Forcible Entry", "AXE-2023-008"),
                        new EquipmentItem("105", "Pike Pole 6ft", "Overhaul", "PP6-2022-003")
                )),
                new CompartmentData("2", "Officer Side", List.of(
                        new EquipmentItem("201", "Thermal Imaging Camera", "Search & Rescue", "TIC-2024-001"),
                        new EquipmentItem("202", "Portable Radio #1", "Communications", "RAD-2024-101"),
                        new EquipmentItem("203", "Portable Radio #2", "Communications", "RAD-2024-102"),
                        new EquipmentItem("204", "First Aid Kit", "Medical", "FAK-2024-001"),
                        new EquipmentItem("205", "AED Unit", "Medical", "AED-2023-005")
                )),
                new CompartmentData("3", "Rear", List.of(
                        new EquipmentItem("301", "Attack Hose 1.75\"", "Hose", "AH175-2024-001"),
                        new EquipmentItem("302", "Supply Hose 4\"", "Hose", "SH4-2024-001"),
                        new EquipmentItem("303", "Nozzle - Fog", "Nozzle", "NF-2024-001"),
                        new EquipmentItem("304", "Nozzle - Smooth Bore", "Nozzle", "NSB-2024-001"),
                        new EquipmentItem("305", "Portable Pump", "Pump", "PP-2023-002"),
                        new EquipmentItem("306", "Generator", "Power", "GEN-2023-001")
                )),
                new CompartmentData("4", "Top/Roof", List.of(
                        new EquipmentItem("401", "Ground Ladder 14ft", "Ladder", "GL14-2024-001"),
                        new EquipmentItem("402", "Roof Ladder 16ft", "Ladder", "RL16-2024-001"),
                        new EquipmentItem("403", "Extension Ladder 24ft", "Ladder", "EL24-2023-001"),
                        new EquipmentItem("404", "Attic Ladder 10ft", "Ladder", "AL10-2024-001")
                ))
        );
    }

    private record CompartmentData(String id, String name, List<EquipmentItem> items) {}
    private record EquipmentItem(String id, String name, String type, String serialNumber) {}

    enum ItemStatus {
        UNCHECKED("Not Checked", "unchecked"),
        PRESENT("Present", "present"),
        MISSING("Missing", "missing"),
        DAMAGED("Damaged", "damaged");

        final String label;
        final String cssClass;

        ItemStatus(String label, String cssClass) {
            this.label = label;
            this.cssClass = cssClass;
        }
    }
}
