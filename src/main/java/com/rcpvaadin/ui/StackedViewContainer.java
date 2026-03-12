package com.rcpvaadin.ui;

import com.rcpvaadin.ui.PerspectiveLayout.LeafNode;
import com.rcpvaadin.ui.PerspectiveLayout.StackNode;
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

public class StackedViewContainer extends VerticalLayout implements Collapsible {

    private final String stackId;
    private final List<LeafNode> leaves;
    private final List<ViewContainer> views;
    private final VerticalLayout contentSlot = new VerticalLayout();
    private final List<HorizontalLayout> tabHeaders = new ArrayList<>();
    private int selectedIndex = 0;

    private Button maximizeBtn;
    private boolean maximized = false;
    private Runnable collapseCallback, expandCallback, maximizeCallback, unmaximizeCallback;
    private BiConsumer<String, String> dropHandler;

    public StackedViewContainer(StackNode node, List<ViewContainer> views) {
        this.stackId = node.id();
        this.leaves  = node.leaves();
        this.views   = views;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // ---- Tab strip ----
        HorizontalLayout tabStrip = new HorizontalLayout();
        tabStrip.addClassName("stack-tab-strip");
        tabStrip.setWidthFull();
        tabStrip.setPadding(false);
        tabStrip.setSpacing(false);
        tabStrip.setAlignItems(FlexComponent.Alignment.CENTER);

        for (int i = 0; i < leaves.size(); i++) {
            LeafNode leaf = leaves.get(i);

            Icon tabIcon = new Icon(leaf.icon());
            tabIcon.getStyle().set("width", "14px").set("height", "14px");

            Span tabLabel = new Span(leaf.name());

            HorizontalLayout tabHeader = new HorizontalLayout(tabIcon, tabLabel);
            tabHeader.addClassName("stack-tab-header");
            tabHeader.setPadding(false);
            tabHeader.setAlignItems(FlexComponent.Alignment.CENTER);

            // DragSource on each tab header so the view can be dragged out
            DragSource<HorizontalLayout> ds = DragSource.create(tabHeader);
            ds.setDragData(leaf.id());

            final int idx = i;
            tabHeader.addClickListener(e -> selectView(idx));
            tabHeaders.add(tabHeader);
            tabStrip.add(tabHeader);
        }

        Span spacer = new Span();
        tabStrip.add(spacer);
        tabStrip.setFlexGrow(1, spacer);

        // Maximize button
        Icon maxIcon = new Icon(VaadinIcon.EXPAND_SQUARE);
        maxIcon.setSize("18px");
        maximizeBtn = new Button(maxIcon);
        maximizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        maximizeBtn.getStyle().set("min-width", "26px").set("width", "26px").set("height", "26px");
        maximizeBtn.setTooltipText("Maximize");
        maximizeBtn.addClickListener(e -> {
            if (!maximized) { if (maximizeCallback   != null) maximizeCallback.run(); }
            else            { if (unmaximizeCallback != null) unmaximizeCallback.run(); }
        });

        // Collapse button
        Icon collapseIcon = new Icon(VaadinIcon.ANGLE_DOWN);
        collapseIcon.setSize("18px");
        Button collapseBtn = new Button(collapseIcon);
        collapseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        collapseBtn.getStyle().set("min-width", "26px").set("width", "26px").set("height", "26px");
        collapseBtn.setTooltipText("Minimize");
        collapseBtn.addClickListener(e -> {
            setVisible(false);
            if (collapseCallback != null) collapseCallback.run();
        });

        tabStrip.add(maximizeBtn, collapseBtn);

        // ---- Content slot ----
        contentSlot.setSizeFull();
        contentSlot.setPadding(false);
        contentSlot.setSpacing(false);

        add(tabStrip, contentSlot);
        setFlexGrow(1, contentSlot);

        // Add all views upfront (hidden); selectView will flip visibility
        for (int i = 0; i < views.size(); i++) {
            ViewContainer vc = views.get(i);
            contentSlot.add(vc);
            contentSlot.setFlexGrow(1, vc);
            vc.setVisible(false);
        }

        // ---- Drop target on entire container ----
        DropTarget<StackedViewContainer> dt = DropTarget.create(this);
        dt.setDropEffect(DropEffect.MOVE);
        getElement().addEventListener("dragenter", e -> addClassName("drop-target-active"));
        getElement().addEventListener("dragleave", e -> removeClassName("drop-target-active"));
        dt.addDropListener(e -> {
            removeClassName("drop-target-active");
            e.getDragData().ifPresent(data -> {
                if (dropHandler != null) dropHandler.accept((String) data, stackId);
            });
        });

        // Select the initial view
        selectView(node.selected());
    }

    private void selectView(int index) {
        if (index < 0 || index >= views.size()) return;
        selectedIndex = index;

        for (int i = 0; i < tabHeaders.size(); i++) {
            tabHeaders.get(i).removeClassName("stack-tab-header--active");
            if (i == index) tabHeaders.get(i).addClassName("stack-tab-header--active");
        }

        for (int i = 0; i < views.size(); i++) {
            views.get(i).setVisible(i == index);
        }
    }

    public void setDropHandler(BiConsumer<String, String> h) {
        this.dropHandler = h;
    }

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
}
