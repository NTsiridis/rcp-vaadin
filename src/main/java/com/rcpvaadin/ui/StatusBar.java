package com.rcpvaadin.ui;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class StatusBar extends HorizontalLayout {

    private final Span perspectiveSpan = new Span();
    private final Span statusSpan      = new Span("Ready");

    public StatusBar() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setPadding(false);
        getStyle()
                .set("height", "22px")
                .set("min-height", "22px")
                .set("padding", "0 8px")
                .set("border-top", "2px solid var(--perspective-color)")
                .set("background", "var(--flat-bg-subtle)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("flex-shrink", "0");

        perspectiveSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--perspective-color)");

        Span spacer = new Span();
        add(perspectiveSpan, spacer, statusSpan);
        setFlexGrow(1, spacer);
    }

    public void setPerspective(String name) {
        perspectiveSpan.setText(name);
    }

    public void setStatus(String message) {
        statusSpan.setText(message);
    }
}
