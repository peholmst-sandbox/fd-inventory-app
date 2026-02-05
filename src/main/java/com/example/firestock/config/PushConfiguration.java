package com.example.firestock.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Configures Vaadin Push settings including the reconnect dialog messages
 * displayed when the connection to the server is lost.
 */
@Component
public class PushConfiguration implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            var dialog = uiInitEvent.getUI().getReconnectDialogConfiguration();
            dialog.setDialogText("Connection lost. Trying to reconnect...");
            dialog.setDialogTextGaveUp("Unable to connect. Please check your network connection.");
        });
    }
}
