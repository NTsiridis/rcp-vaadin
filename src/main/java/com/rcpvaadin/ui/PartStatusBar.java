package com.rcpvaadin.ui;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class PartStatusBar extends HorizontalLayout {

    private final Span messageSpan = new Span();
    private final Span systemSpan  = new Span();

    public PartStatusBar() {
        addClassName("part-status-bar");
        messageSpan.addClassName("part-status-message");
        systemSpan.addClassName("part-status-system");
        setWidthFull();
        add(messageSpan, systemSpan);
        setFlexGrow(1, messageSpan);
    }

    public void setMessage(String text)    { messageSpan.setText(text != null ? text : ""); }
    public void setSystemInfo(String text) { systemSpan.setText(text != null ? text : ""); }
}
