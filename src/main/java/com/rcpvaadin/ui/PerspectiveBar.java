package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.descriptor.PerspectiveDescriptor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PerspectiveBar extends VerticalLayout {

    private final Map<String, Button> idToButton = new LinkedHashMap<>();

    public PerspectiveBar(Collection<PerspectiveDescriptor> perspectives,
                          Consumer<String> onSelected,
                          Consumer<String> onSystemAction) {
        addClassName("perspective-bar");
        setWidth("52px");
        setSizeUndefined();
        setHeightFull();
        setPadding(false);
        setSpacing(false);

        // --- Perspective buttons (top section) ---
        perspectives.forEach(pd -> {
            Button btn = new Button(new Icon(pd.icon()));
            btn.addClassName("perspective-btn");
            btn.setTooltipText(pd.name());
            btn.addClickListener(e -> onSelected.accept(pd.id()));
            idToButton.put(pd.id(), btn);
            add(btn);
        });

        // --- Spacer pushes system buttons to the bottom ---
        Span spacer = new Span();
        setFlexGrow(1, spacer);
        add(spacer);

        // --- Divider ---
        Span divider = new Span();
        divider.addClassName("perspective-bar-divider");
        add(divider);

        // --- System buttons (bottom section) ---
        add(makeSystemBtn(VaadinIcon.COG,  "Preferences", () -> onSystemAction.accept("systemPreferences")));
        add(makeSystemBtn(VaadinIcon.USER, "User Profile", () -> onSystemAction.accept("userProfile")));
    }

    public void selectPerspective(String id) {
        idToButton.values().forEach(b -> b.removeClassName("perspective-btn--active"));
        Button btn = idToButton.get(id);
        if (btn != null) btn.addClassName("perspective-btn--active");
    }

    private Button makeSystemBtn(VaadinIcon icon, String tooltip, Runnable onClick) {
        Button btn = new Button(new Icon(icon));
        btn.addClassName("perspective-btn");
        btn.addClassName("system-btn");
        btn.setTooltipText(tooltip);
        btn.addClickListener(e -> onClick.run());
        return btn;
    }
}
