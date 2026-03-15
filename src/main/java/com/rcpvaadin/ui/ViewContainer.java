package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.PartSite;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.search.IQuickFilterable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ViewContainer extends VerticalLayout implements Collapsible {

    private Runnable collapseCallback   = null;
    private Runnable expandCallback     = null;
    private Runnable maximizeCallback   = null;
    private Runnable unmaximizeCallback = null;
    private boolean  maximized = false;

    private final Button maximizeBtn;
    private final HorizontalLayout titleBar;
    private final PartStatusBar partStatusBar = new PartStatusBar();
    private BiConsumer<String, String> dropHandler;

    // Non-null only when viewPart implements IQuickFilterable
    private QuickFilterBar quickFilterBar;

    public ViewContainer(IViewPart viewPart, VaadinIcon icon, String viewId) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Icon collapseIcon = new Icon(VaadinIcon.ANGLE_DOWN);
        collapseIcon.setSize("18px");
        Button collapseBtn = new Button(collapseIcon);
        collapseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        collapseBtn.getStyle().set("min-width", "30px").set("width", "30px").set("height", "30px");
        collapseBtn.setTooltipText("Minimize");

        Icon maxIcon = new Icon(VaadinIcon.EXPAND_SQUARE);
        maxIcon.setSize("18px");
        maximizeBtn = new Button(maxIcon);
        maximizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        maximizeBtn.getStyle().set("min-width", "30px").set("width", "30px").set("height", "30px");
        maximizeBtn.setTooltipText("Maximize");

        Icon iconComp = new Icon(icon);
        iconComp.getStyle().set("width", "18px").set("height", "18px");

        Span title = new Span(viewPart.getTitle());
        title.addClassName("view-title");

        // Title bar: icon | title | [flex spacer] | maximize | collapse
        titleBar = new HorizontalLayout(iconComp, title, maximizeBtn, collapseBtn);
        titleBar.addClassName("view-title-bar");
        titleBar.setWidthFull();
        titleBar.setPadding(false);
        titleBar.setAlignItems(FlexComponent.Alignment.CENTER);
        titleBar.setFlexGrow(1, title);

        // Drag source — title bar only
        DragSource<HorizontalLayout> ds = DragSource.create(titleBar);
        ds.setDragData(viewId);

        // Drop target — entire container
        DropTarget<ViewContainer> dt = DropTarget.create(this);
        dt.setDropEffect(DropEffect.MOVE);
        getElement().addEventListener("dragenter", e -> addClassName("drop-target-active"));
        getElement().addEventListener("dragleave", e -> removeClassName("drop-target-active"));
        dt.addDropListener(e -> {
            removeClassName("drop-target-active");
            e.getDragData().ifPresent(data -> {
                if (dropHandler != null) dropHandler.accept((String) data, viewId);
            });
        });

        com.vaadin.flow.component.Component content = viewPart.createPartControl();

        // Toolbar strip + optional QuickFilterBar
        List<ToolbarItem> toolbarItems = viewPart.getToolbarItems();

        if (viewPart instanceof IQuickFilterable filterable) {
            boolean[] filterBarVisible = { false };

            // quickFilterBar is an instance field so the lambda can safely reference it
            quickFilterBar = new QuickFilterBar(
                    filterable::applyQuickFilter,
                    () -> {
                        filterBarVisible[0] = false;
                        quickFilterBar.setVisible(false);
                        quickFilterBar.reset();
                        filterable.applyQuickFilter("");
                    });
            quickFilterBar.setVisible(false);

            // Replace the SEARCH toolbar item's action with a toggle
            List<ToolbarItem> items = new ArrayList<>(toolbarItems);
            for (int i = 0; i < items.size(); i++) {
                ToolbarItem item = items.get(i);
                if (item.icon() == VaadinIcon.SEARCH) {
                    items.set(i, new ToolbarItem(item.icon(), item.tooltip(), () -> {
                        filterBarVisible[0] = !filterBarVisible[0];
                        quickFilterBar.setVisible(filterBarVisible[0]);
                        if (filterBarVisible[0]) {
                            quickFilterBar.focus();
                        } else {
                            quickFilterBar.reset();
                            filterable.applyQuickFilter("");
                        }
                    }));
                    break;
                }
            }

            HorizontalLayout toolbarBar = buildToolbarBar(items);
            add(titleBar, toolbarBar, quickFilterBar, content, partStatusBar);
        } else if (toolbarItems.isEmpty()) {
            add(titleBar, content, partStatusBar);
        } else {
            HorizontalLayout toolbarBar = buildToolbarBar(toolbarItems);
            add(titleBar, toolbarBar, content, partStatusBar);
        }

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

    // -------------------------------------------------------------------------
    // Toolbar strip rendering
    // -------------------------------------------------------------------------

    static HorizontalLayout buildToolbarBar(List<ToolbarItem> items) {
        HorizontalLayout bar = new HorizontalLayout();
        bar.addClassName("part-toolbar-bar");
        bar.setPadding(false);
        bar.setSpacing(false);
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        for (ToolbarItem item : items) {
            Icon icon = new Icon(item.icon());
            icon.setSize("18px");
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            btn.addClassName("toolbar-btn");
            btn.setTooltipText(item.tooltip());
            btn.addClickListener(e -> item.action().run());
            bar.add(btn);
        }
        return bar;
    }

    // -------------------------------------------------------------------------
    // Drop handler
    // -------------------------------------------------------------------------

    public void setDropHandler(BiConsumer<String, String> handler) { this.dropHandler = handler; }

    public void setTitleBarVisible(boolean visible) { titleBar.setVisible(visible); }

    // -------------------------------------------------------------------------
    // Collapsible
    // -------------------------------------------------------------------------

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
