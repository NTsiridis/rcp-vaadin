package com.rcpvaadin.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.LinkedHashMap;
import java.util.Map;

public class MinimizedBar extends VerticalLayout {

    private final Map<String, Button> idToButton = new LinkedHashMap<>();

    public MinimizedBar() {
        addClassName("minimized-bar");
        setWidth("52px");
        setHeightFull();
        setPadding(false);
        setSpacing(false);
    }

    public void addMinimized(String id, String name, VaadinIcon icon, Runnable onRestore) {
        Button btn = new Button(new Icon(icon));
        btn.addClassName("minimized-btn");
        btn.setTooltipText(name);
        btn.addClickListener(e -> onRestore.run());
        idToButton.put(id, btn);
        add(btn);
    }

    public void removeMinimized(String id) {
        Button btn = idToButton.remove(id);
        if (btn != null) remove(btn);
    }
}
