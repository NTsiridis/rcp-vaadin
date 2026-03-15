package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IWorkbenchPage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EditorArea extends VerticalLayout implements Collapsible {

    private record MinimizedEditorEntry(
            IEditorPart editor, IEditorInput input, VaadinIcon icon, EditorContainer container) {}

    private final TabSheet                          tabSheet         = new TabSheet();
    private final Map<IEditorInput, Tab>            inputToTab       = new HashMap<>();
    private final Map<Tab, IEditorInput>            tabToInput       = new HashMap<>();
    private final Map<Tab, EditorContainer>         tabToContainer   = new HashMap<>();
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
        tabSheet.setSizeFull();
        add(tabSheet);
        setFlexGrow(1, tabSheet);

        // ── Tab strip suffix: maximize + collapse ──
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
        suffixLayout.getStyle().set("gap", "2px").set("align-items", "center").set("padding-right", "4px");
        tabSheet.setSuffixComponent(suffixLayout);
    }

    public void setPage(IWorkbenchPage page) {
        this.page = page;
    }

    public void setMinimizedBar(MinimizedBar minimizedBar) {
        this.minimizedBar = minimizedBar;
    }

    public void openTab(IEditorPart editor, IEditorInput input, VaadinIcon icon) {
        // Case 1: already visible in a tab
        if (inputToTab.containsKey(input)) {
            tabSheet.setSelectedTab(inputToTab.get(input));
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
        EditorContainer content = new EditorContainer(editor, icon);
        tabToContainer.put(tab, content);
        tabSheet.add(tab, content);
        tabSheet.setSelectedTab(tab);
    }

    public void closeTab(IEditorInput input) {
        Tab tab = inputToTab.remove(input);
        if (tab != null) {
            tabToInput.remove(tab);
            tabToContainer.remove(tab);
            tabSheet.remove(tab);
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
        tabSheet.remove(tab);

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
        tabToContainer.put(newTab, entry.container());
        tabSheet.add(newTab, entry.container());
        tabSheet.setSelectedTab(newTab);
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
