package com.example.firestock.views;

import com.example.firestock.views.inventorycheck.ApparatusSelectionView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Main application layout with navigation drawer.
 * Mobile-responsive with drawer toggle for small screens.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("FireStock");
        logo.addClassNames("text-l", "m-0", "font-semibold");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames("p-m", "navbar");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        SideNavItem inventoryCheck = new SideNavItem(
                "Inventory Check",
                ApparatusSelectionView.class,
                VaadinIcon.CHECK_SQUARE_O.create()
        );
        nav.addItem(inventoryCheck);

        addToDrawer(nav);
    }
}
