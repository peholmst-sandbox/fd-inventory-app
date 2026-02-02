package com.example.firestock.views;

import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.views.inventorycheck.ApparatusSelectionView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/**
 * Main application layout with navigation drawer.
 * Mobile-responsive with drawer toggle for small screens.
 */
@PermitAll
public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("FireStock");
        logo.addClassNames("text-l", "m-0", "font-semibold");

        HorizontalLayout header = authenticationContext.getAuthenticatedUser(FirestockUserDetails.class)
            .map(user -> {
                Span userName = new Span(user.getFullName());
                userName.addClassNames("text-s", "text-secondary");

                Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create(),
                    click -> authenticationContext.logout());
                logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

                HorizontalLayout userSection = new HorizontalLayout(userName, logoutButton);
                userSection.setAlignItems(FlexComponent.Alignment.CENTER);
                userSection.setSpacing(true);

                HorizontalLayout headerLayout = new HorizontalLayout(new DrawerToggle(), logo, userSection);
                headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
                headerLayout.expand(logo);
                headerLayout.setWidthFull();
                headerLayout.addClassNames("p-m", "navbar");
                return headerLayout;
            })
            .orElseGet(() -> {
                HorizontalLayout headerLayout = new HorizontalLayout(new DrawerToggle(), logo);
                headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
                headerLayout.setWidthFull();
                headerLayout.addClassNames("p-m", "navbar");
                return headerLayout;
            });

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
