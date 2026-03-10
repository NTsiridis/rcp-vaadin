package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.PartSite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ViewContainer extends VerticalLayout implements Collapsible {

    private Runnable collapseCallback   = null;
    private Runnable expandCallback     = null;
    private Runnable maximizeCallback   = null;
    private Runnable unmaximizeCallback = null;
    private boolean  maximized = false;

    private final Button maximizeBtn;
    private final PartStatusBar partStatusBar = new PartStatusBar();

    public ViewContainer(IViewPart viewPart, VaadinIcon icon) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Icon collapseIcon = new Icon(VaadinIcon.ANGLE_DOWN);
        collapseIcon.setSize("18px");
        Button collapseBtn = new Button(collapseIcon);
        collapseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        collapseBtn.getStyle().set("min-width", "22px");
        collapseBtn.setTooltipText("Minimize");

        Icon maxIcon = new Icon(VaadinIcon.EXPAND_SQUARE);
        maxIcon.setSize("18px");
        maximizeBtn = new Button(maxIcon);
        maximizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        maximizeBtn.getStyle().set("min-width", "22px").set("margin-left", "auto");
        maximizeBtn.setTooltipText("Maximize");

        Icon iconComp = new Icon(icon);
        iconComp.getStyle().set("width", "14px").set("height", "14px");

        Span title = new Span(viewPart.getTitle());
        title.addClassName("view-title");

        HorizontalLayout titleBar = new HorizontalLayout(iconComp, title, maximizeBtn, collapseBtn);
        titleBar.addClassName("view-title-bar");
        titleBar.setWidthFull();
        titleBar.setPadding(false);
        titleBar.setAlignItems(FlexComponent.Alignment.CENTER);

        com.vaadin.flow.component.Component content = viewPart.createPartControl();
        add(titleBar, content, partStatusBar);
        setFlexGrow(1, content);

        collapseBtn.addClickListener(e -> {
            setVisible(false);
            if (collapseCallback != null) collapseCallback.run();
        });

        maximizeBtn.addClickListener(e -> {
            if (!maximized) { if (maximizeCallback   != null) maximizeCallback.run(); }
            else             { if (unmaximizeCallback != null) unmaximizeCallback.run(); }
        });
    }

    @Override
    public void setCollapseSupport(Runnable onCollapse, Runnable onExpand) {
        this.collapseCallback = onCollapse;
        this.expandCallback   = onExpand;
    }

    @Override
    public void setMaximizeSupport(Runnable onMaximize, Runnable onUnmaximize) {
        this.maximizeCallback   = onMaximize;
        this.unmaximizeCallback = onUnmaximize;
    }

    @Override
    public void setMaximizedState(boolean m) {
        this.maximized = m;
        Icon icon = new Icon(m ? VaadinIcon.COMPRESS : VaadinIcon.EXPAND_SQUARE);
        icon.setSize("18px");
        maximizeBtn.setIcon(icon);
        maximizeBtn.getStyle().set("color", m ? "var(--lumo-primary-color)" : "");
        maximizeBtn.setTooltipText(m ? "Restore" : "Maximize");
    }

    public void bindStatusBar(PartSite site) {
        site.bindStatusBar(partStatusBar::setMessage, partStatusBar::setSystemInfo);
    }
}
