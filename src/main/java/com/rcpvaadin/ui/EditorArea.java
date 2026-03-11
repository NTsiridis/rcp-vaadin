package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IWorkbenchPage;
import com.rcpvaadin.workbench.PartSite;
import com.rcpvaadin.workbench.ToolbarItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorArea extends VerticalLayout implements Collapsible {

    private final TabSheet              tabSheet   = new TabSheet();
    private final Map<IEditorInput, Tab> inputToTab = new HashMap<>();
    private final Map<Tab, IEditorInput> tabToInput = new HashMap<>();
    private final Map<Tab, PartSite>    tabToSite  = new HashMap<>();
    private final Map<Tab, List<ToolbarItem>> tabToToolbarItems = new HashMap<>();
    private final PartStatusBar         partStatusBar = new PartStatusBar();

    // Persistent toolbar strip below the title bar; shown/hidden per active tab
    private final HorizontalLayout toolbarBar = new HorizontalLayout();

    private IWorkbenchPage page;
    private Runnable collapseCallback   = null;
    private Runnable expandCallback     = null;
    private Runnable maximizeCallback   = null;
    private Runnable unmaximizeCallback = null;
    private boolean  maximized = false;

    private final Button maximizeBtn;

    public EditorArea() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Icon collapseIcon = new Icon(VaadinIcon.ANGLE_DOWN);
        collapseIcon.setSize("22px");
        Button collapseBtn = new Button(collapseIcon);
        collapseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        collapseBtn.getStyle().set("min-width", "30px").set("width", "30px").set("height", "30px");
        collapseBtn.setTooltipText("Minimize");

        Icon maxIcon = new Icon(VaadinIcon.EXPAND_SQUARE);
        maxIcon.setSize("22px");
        maximizeBtn = new Button(maxIcon);
        maximizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        maximizeBtn.getStyle().set("min-width", "30px").set("width", "30px").set("height", "30px");
        maximizeBtn.setTooltipText("Maximize");

        Icon iconComp = new Icon(VaadinIcon.PENCIL);
        iconComp.getStyle().set("width", "18px").set("height", "18px");

        Span title = new Span("Editors");
        title.addClassName("view-title");

        // Title bar: icon | title | [flex spacer] | maximize | collapse
        HorizontalLayout titleBar = new HorizontalLayout(iconComp, title, maximizeBtn, collapseBtn);
        titleBar.addClassName("view-title-bar");
        titleBar.setWidthFull();
        titleBar.setPadding(false);
        titleBar.setAlignItems(FlexComponent.Alignment.CENTER);
        titleBar.setFlexGrow(1, title);

        // Persistent toolbar strip below title bar
        toolbarBar.addClassName("part-toolbar-bar");
        toolbarBar.setPadding(false);
        toolbarBar.setSpacing(false);
        toolbarBar.setWidthFull();
        toolbarBar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbarBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbarBar.setVisible(false);

        tabSheet.setSizeFull();
        add(titleBar, toolbarBar, tabSheet, partStatusBar);
        setFlexGrow(1, tabSheet);

        collapseBtn.addClickListener(e -> {
            setVisible(false);
            if (collapseCallback != null) collapseCallback.run();
        });

        maximizeBtn.addClickListener(e -> {
            if (!maximized) { if (maximizeCallback   != null) maximizeCallback.run(); }
            else             { if (unmaximizeCallback != null) unmaximizeCallback.run(); }
        });

        tabSheet.addSelectedChangeListener(e -> {
            rebindToolbarForActiveTab();
            rebindStatusBarToActiveTab();
        });
    }

    public void setPage(IWorkbenchPage page) {
        this.page = page;
    }

    public void openTab(IEditorPart editor, IEditorInput input, VaadinIcon icon) {
        if (inputToTab.containsKey(input)) {
            tabSheet.setSelectedTab(inputToTab.get(input));
            return;
        }

        Icon tabIcon = new Icon(icon);
        tabIcon.getStyle().set("width", "12px").set("height", "12px");

        Span titleSpan = new Span(editor.getTitle());
        Button closeBtn = new Button("×");
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        closeBtn.getStyle().set("margin-left", "4px");

        HorizontalLayout header = new HorizontalLayout(tabIcon, titleSpan, closeBtn);
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setSpacing(false);

        Tab tab = new Tab(header);
        closeBtn.addClickListener(e -> {
            if (page != null) page.closeEditor(input);
        });

        // Populate maps BEFORE tabSheet.add() — adding the first tab auto-selects
        // it and fires selectedChangeListener, which must already find the maps populated.
        inputToTab.put(input, tab);
        tabToInput.put(tab, input);
        tabToToolbarItems.put(tab, editor.getToolbarItems());
        if (editor.getSite() instanceof PartSite ps) {
            tabToSite.put(tab, ps);
        }

        com.vaadin.flow.component.Component content = editor.createPartControl();
        tabSheet.add(tab, content);
        tabSheet.setSelectedTab(tab);
        // selectedChangeListener handles toolbar + status bar refresh
    }

    public void closeTab(IEditorInput input) {
        Tab tab = inputToTab.remove(input);
        if (tab != null) {
            tabToInput.remove(tab);
            tabToToolbarItems.remove(tab);
            PartSite ps = tabToSite.remove(tab);
            if (ps != null) ps.bindStatusBar(null, null);
            tabSheet.remove(tab);
            // selectedChangeListener handles toolbar + status bar refresh
        }
    }

    // -------------------------------------------------------------------------
    // Per-tab UI refresh
    // -------------------------------------------------------------------------

    private void rebindToolbarForActiveTab() {
        toolbarBar.removeAll();
        Tab selected = tabSheet.getSelectedTab();
        List<ToolbarItem> items = tabToToolbarItems.getOrDefault(selected, List.of());
        toolbarBar.setVisible(!items.isEmpty());
        for (ToolbarItem item : items) {
            Icon icon = new Icon(item.icon());
            icon.setSize("18px");
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            btn.addClassName("toolbar-btn");
            btn.setTooltipText(item.tooltip());
            btn.addClickListener(e -> item.action().run());
            toolbarBar.add(btn);
        }
    }

    private void rebindStatusBarToActiveTab() {
        Tab selected = tabSheet.getSelectedTab();
        PartSite ps = (selected != null) ? tabToSite.get(selected) : null;
        if (ps != null) {
            ps.bindStatusBar(partStatusBar::setMessage, partStatusBar::setSystemInfo);
        } else {
            partStatusBar.setMessage("");
            partStatusBar.setSystemInfo("");
        }
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
        icon.setSize("22px");
        maximizeBtn.setIcon(icon);
        maximizeBtn.getStyle().set("color", m ? "var(--lumo-primary-color)" : "");
        maximizeBtn.setTooltipText(m ? "Restore" : "Maximize");
    }
}
