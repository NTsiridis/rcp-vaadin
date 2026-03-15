package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IWorkbenchPage;
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
import com.vaadin.flow.component.tabs.Tabs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorArea extends VerticalLayout implements Collapsible {

    private record MinimizedEditorEntry(
            IEditorPart editor, IEditorInput input, VaadinIcon icon, EditorContainer container) {}

    private final Tabs                              tabs             = new Tabs();
    private final HorizontalLayout                 tabHeader        = new HorizontalLayout();
    private final HorizontalLayout                 toolbarBar       = new HorizontalLayout();
    private final VerticalLayout                   contentArea      = new VerticalLayout();
    private final Map<IEditorInput, Tab>            inputToTab       = new HashMap<>();
    private final Map<Tab, IEditorInput>            tabToInput       = new HashMap<>();
    private final Map<Tab, EditorContainer>         tabToContainer   = new HashMap<>();
    private final Map<Tab, IEditorPart>             tabToEditor      = new HashMap<>();
    private final Map<String, MinimizedEditorEntry> minimizedEditors = new LinkedHashMap<>();

    private IWorkbenchPage page;
    private MinimizedBar   minimizedBar;
    private Runnable collapseCallback   = null;
    private Runnable expandCallback     = null;
    private Runnable maximizeCallback   = null;
    private Runnable unmaximizeCallback = null;
    private boolean  maximized          = false;
    private int      minimizeCounter    = 0;
    private Button   maximizeBtn;

    public EditorArea() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // ── Tab header row (uses .view-title-bar height via .editor-tab-header) ──
        tabHeader.addClassName("view-title-bar");
        tabHeader.addClassName("editor-tab-header");
        tabHeader.setPadding(false);
        tabHeader.setSpacing(false);
        tabHeader.setWidthFull();
        tabHeader.setAlignItems(FlexComponent.Alignment.CENTER);

        tabs.setWidthFull();
        tabs.getStyle().set("flex-grow", "1").set("min-width", "0").set("background", "transparent");

        // Suffix buttons: maximize + collapse
        Icon collapseIcon = new Icon(VaadinIcon.ANGLE_DOWN);
        collapseIcon.setSize("18px");
        Button collapseBtn = new Button(collapseIcon);
        collapseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        collapseBtn.addClassName("tab-strip-btn");
        collapseBtn.setTooltipText("Minimize editors");
        collapseBtn.addClickListener(e -> { if (collapseCallback != null) collapseCallback.run(); });

        Icon maxIcon = new Icon(VaadinIcon.EXPAND_SQUARE);
        maxIcon.setSize("18px");
        maximizeBtn = new Button(maxIcon);
        maximizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        maximizeBtn.addClassName("tab-strip-btn");
        maximizeBtn.setTooltipText("Maximize editors");
        maximizeBtn.addClickListener(e -> {
            if (!maximized) doMaximize();
            else            doUnmaximize();
        });

        HorizontalLayout suffixLayout = new HorizontalLayout(maximizeBtn, collapseBtn);
        suffixLayout.setPadding(false);
        suffixLayout.setSpacing(false);
        suffixLayout.getStyle()
                .set("gap", "2px")
                .set("align-items", "center")
                .set("padding-right", "4px")
                .set("flex-shrink", "0");

        tabHeader.add(tabs, suffixLayout);
        tabHeader.setFlexGrow(1, tabs);

        // ── Toolbar bar (mirrors ViewContainer.buildToolbarBar height) ──
        toolbarBar.addClassName("part-toolbar-bar");
        toolbarBar.setPadding(false);
        toolbarBar.setSpacing(false);
        toolbarBar.setWidthFull();
        toolbarBar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbarBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        // ── Content area ──
        contentArea.setSizeFull();
        contentArea.setPadding(false);
        contentArea.setSpacing(false);
        contentArea.getStyle().set("overflow", "hidden").set("position", "relative");

        add(tabHeader, toolbarBar, contentArea);
        setFlexGrow(1, contentArea);

        // Tab selection → rebind UI
        tabs.addSelectedChangeListener(e -> rebindActiveTabUI());
    }

    // -------------------------------------------------------------------------
    // Active-tab binding
    // -------------------------------------------------------------------------

    private void rebindActiveTabUI() {
        Tab selectedTab = tabs.getSelectedTab();

        // Show only the selected editor container
        tabToContainer.forEach((tab, container) ->
                container.setVisible(tab.equals(selectedTab)));

        // Repopulate toolbar with active editor's items
        toolbarBar.removeAll();
        if (selectedTab != null) {
            IEditorPart editor = tabToEditor.get(selectedTab);
            if (editor != null) {
                List<ToolbarItem> items = editor.getToolbarItems();
                for (ToolbarItem item : items) {
                    Icon icon = new Icon(item.icon());
                    icon.setSize("18px");
                    Button btn = new Button(icon);
                    btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                    btn.addClassName("toolbar-btn");
                    btn.setTooltipText(item.tooltip());
                    btn.addClickListener(ev -> item.action().run());
                    toolbarBar.add(btn);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Page / MinimizedBar injection
    // -------------------------------------------------------------------------

    public void setPage(IWorkbenchPage page) {
        this.page = page;
    }

    public void setMinimizedBar(MinimizedBar minimizedBar) {
        this.minimizedBar = minimizedBar;
    }

    // -------------------------------------------------------------------------
    // Open / close
    // -------------------------------------------------------------------------

    public void openTab(IEditorPart editor, IEditorInput input, VaadinIcon icon) {
        // Case 1: already visible in a tab
        if (inputToTab.containsKey(input)) {
            tabs.setSelectedTab(inputToTab.get(input));
            return;
        }

        // Case 2: currently minimized — restore instead of creating a duplicate
        for (Map.Entry<String, MinimizedEditorEntry> e : minimizedEditors.entrySet()) {
            if (e.getValue().input().equals(input)) {
                restoreEditorTab(e.getKey());
                return;
            }
        }

        Tab tab = buildTab(editor, input, icon);
        EditorContainer container = new EditorContainer(editor, icon);
        container.setVisible(false);  // hidden until selected
        tabToContainer.put(tab, container);
        tabToEditor.put(tab, editor);
        tabs.add(tab);
        contentArea.add(container);
        tabs.setSelectedTab(tab);  // triggers rebindActiveTabUI
    }

    public void closeTab(IEditorInput input) {
        Tab tab = inputToTab.remove(input);
        if (tab != null) {
            tabToInput.remove(tab);
            EditorContainer container = tabToContainer.remove(tab);
            tabToEditor.remove(tab);
            tabs.remove(tab);
            if (container != null) contentArea.remove(container);
        }
    }

    // -------------------------------------------------------------------------
    // Per-tab minimize / restore
    // -------------------------------------------------------------------------

    private void minimizeEditorTab(IEditorInput input, IEditorPart editor, VaadinIcon icon) {
        Tab tab = inputToTab.remove(input);
        if (tab == null) return;
        EditorContainer container = tabToContainer.remove(tab);
        tabToInput.remove(tab);
        tabToEditor.remove(tab);
        tabs.remove(tab);
        if (container != null) contentArea.remove(container);

        if (minimizedBar != null) {
            String minId = "editor_" + (minimizeCounter++);
            minimizedEditors.put(minId, new MinimizedEditorEntry(editor, input, icon, container));
            minimizedBar.addMinimized(minId, editor.getTitle(), icon, () -> restoreEditorTab(minId));
        }
    }

    private void restoreEditorTab(String minId) {
        MinimizedEditorEntry entry = minimizedEditors.remove(minId);
        if (entry == null) return;
        if (minimizedBar != null) minimizedBar.removeMinimized(minId);

        Tab newTab = buildTab(entry.editor(), entry.input(), entry.icon());
        EditorContainer container = entry.container();
        container.setVisible(false);
        tabToContainer.put(newTab, container);
        tabToEditor.put(newTab, entry.editor());
        tabs.add(newTab);
        contentArea.add(container);
        tabs.setSelectedTab(newTab);
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private Tab buildTab(IEditorPart editor, IEditorInput input, VaadinIcon icon) {
        Icon tabIcon = new Icon(icon);
        tabIcon.getStyle().set("width", "18px").set("height", "18px");

        Span titleSpan = new Span(editor.getTitle());
        titleSpan.addClassName("view-title");

        Button closeBtn = new Button("×");
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        closeBtn.addClickListener(e -> { if (page != null) page.closeEditor(input); });

        HorizontalLayout header = new HorizontalLayout(tabIcon, titleSpan, closeBtn);
        header.setSpacing(false);
        header.getStyle().set("gap", "4px").set("align-items", "center");

        Tab tab = new Tab(header);
        inputToTab.put(input, tab);
        tabToInput.put(tab, input);
        return tab;
    }

    private void doMaximize() {
        if (maximizeCallback != null) maximizeCallback.run();
    }

    private void doUnmaximize() {
        if (unmaximizeCallback != null) unmaximizeCallback.run();
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
        if (maximizeBtn != null) {
            Icon icon = new Icon(m ? VaadinIcon.COMPRESS : VaadinIcon.EXPAND_SQUARE);
            icon.setSize("18px");
            maximizeBtn.setIcon(icon);
            maximizeBtn.setTooltipText(m ? "Restore editors" : "Maximize editors");
        }
    }
}
