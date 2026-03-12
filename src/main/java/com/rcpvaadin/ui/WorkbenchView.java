package com.rcpvaadin.ui;

import com.rcpvaadin.sample.system.SystemEditorInput;
import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IWorkbenchPage;
import com.rcpvaadin.workbench.Workbench;
import com.rcpvaadin.workbench.WorkbenchPage;
import com.rcpvaadin.workbench.annotation.RcpEditor;
import com.rcpvaadin.workbench.descriptor.EditorDescriptor;
import com.rcpvaadin.workbench.descriptor.PerspectiveDescriptor;
import com.rcpvaadin.workbench.descriptor.PerspectiveNavItem;
import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.rcpvaadin.workbench.perspective.PageLayout;
import com.rcpvaadin.workbench.registry.WorkbenchRegistry;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Route;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * Top-level application shell. One instance per browser tab.
 * Owns a WorkbenchPage and listens to its events to keep the UI in sync.
 */
@Route("")
public class WorkbenchView extends AppLayout implements WorkbenchPage.WorkbenchPageListener {

    private final IWorkbenchPage     page;
    private final WorkbenchRegistry  registry;
    private final ApplicationContext ctx;

    // Stable wrapper — lives for the full lifetime of this view; only
    // perspectiveLayout is swapped inside it on perspective changes.
    private final VerticalLayout contentWrapper = new VerticalLayout();
    private final StatusBar      statusBar      = new StatusBar();
    private final MinimizedBar   minimizedBar   = new MinimizedBar();

    // Nav panel (inline, right of perspectiveBar). Always in outerLayout;
    // setVisible(false) when the active perspective has no nav items.
    private final VerticalLayout navPanel = new VerticalLayout();

    // References to the toggle button and SideNav inside navPanel —
    // rebuilt on each perspective switch by updateNavDrawer().
    private Button  navPanelToggle  = null;
    private SideNav navSideNav      = null;
    private boolean navPanelExpanded = true;

    private final Map<String, SideNavItem>  navLeafItems = new LinkedHashMap<>();

    // Compound keys: "perspId" (no nav) or "perspId::navItemId" (with nav)
    private final Map<String, PerspectiveState> allStates = new HashMap<>();

    private String           currentPerspectiveId = null;
    private String           currentNavItemId     = null;

    private PerspectiveLayout perspectiveLayout;
    private PerspectiveBar    perspectiveBar;

    @Autowired
    public WorkbenchView(Workbench workbench, WorkbenchRegistry registry, ApplicationContext ctx) {
        this.registry = registry;
        this.ctx      = ctx;
        this.page     = workbench.createPage();

        ((WorkbenchPage) page).addListener(this);

        // Navbar: header only — the nav toggle lives inside navPanel itself
        addToNavbar(buildHeader());

        // Nav panel: inline between perspectiveBar and contentWrapper.
        // Width is controlled by setNavPanelExpanded(); VerticalLayout default
        // width:100% is overridden here so it doesn't swallow the flex row.
        navPanel.setWidth("220px");
        navPanel.setHeightFull();
        navPanel.addClassName("perspective-nav-panel");
        navPanel.setPadding(false);
        navPanel.setSpacing(false);
        navPanel.setVisible(false);   // hidden until a perspective with nav items is active

        // Perspective icon sidebar
        perspectiveBar = new PerspectiveBar(
                registry.getAllPerspectives(),
                this::onPerspectiveSelected,
                this::onSystemAction);

        // contentWrapper holds [perspectiveLayout (flex:1), statusBar] permanently.
        contentWrapper.setSizeFull();
        contentWrapper.setPadding(false);
        contentWrapper.setSpacing(false);
        contentWrapper.add(statusBar);

        // Order: perspectiveBar | navPanel | contentWrapper | minimizedBar
        HorizontalLayout outerLayout = new HorizontalLayout(
                perspectiveBar, navPanel, contentWrapper, minimizedBar);
        outerLayout.setSizeFull();
        outerLayout.setPadding(false);
        outerLayout.setSpacing(false);
        outerLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        outerLayout.setFlexGrow(1, contentWrapper);
        setContent(outerLayout);

        page.setPerspective("javaPerspective");   // → perspectiveChanged → rebuildLayout()
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    private HorizontalLayout buildHeader() {
        Icon logo = new Icon(VaadinIcon.CUBES);
        logo.setSize("20px");
        logo.getStyle().set("color", "#f59e0b");

        Span appName = new Span("RCP Workbench");
        appName.addClassName("workbench-header-title");

        Span version = new Span("v1.0");
        version.addClassName("workbench-header-version");

        HorizontalLayout header = new HorizontalLayout(logo, appName, version);
        header.addClassName("workbench-header");
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(false);
        return header;
    }

    // -------------------------------------------------------------------------
    // State key helper
    // -------------------------------------------------------------------------

    private static String stateKey(String perspId, String navItemId) {
        return navItemId != null ? perspId + "::" + navItemId : perspId;
    }

    // -------------------------------------------------------------------------
    // Layout rebuild (called on perspective switch)
    // -------------------------------------------------------------------------

    private void rebuildLayout() {
        String newPerspId = page.getActivePerspectiveId();
        if (newPerspId == null) return;

        saveOutgoingState(currentPerspectiveId, currentNavItemId);

        PerspectiveDescriptor pd = registry.findPerspective(newPerspId).orElse(null);
        if (pd == null) return;

        List<PerspectiveNavItem> navItemList = registry.getNavItems(newPerspId);
        PerspectiveNavItem activeNavItem = resolveDefaultNavItem(navItemList);
        currentNavItemId = activeNavItem != null ? activeNavItem.id() : null;

        updateNavDrawer(navItemList, currentNavItemId);

        Class<? extends IPerspectiveFactory> factoryClass =
                (activeNavItem != null) ? activeNavItem.layoutFactory() : pd.factoryClass();

        buildPerspectiveLayout(newPerspId, currentNavItemId, factoryClass);

        perspectiveBar.selectPerspective(newPerspId);
        statusBar.setPerspective(pd.name());
        statusBar.setStatus("Ready");

        currentPerspectiveId = newPerspId;
    }

    /**
     * Called on nav item click — swaps layout without rebuilding the nav panel.
     */
    private void rebuildNavItemLayout() {
        String perspId = currentPerspectiveId;
        if (perspId == null) return;

        PerspectiveDescriptor pd = registry.findPerspective(perspId).orElse(null);
        if (pd == null) return;

        List<PerspectiveNavItem> navItemList = registry.getNavItems(perspId);
        PerspectiveNavItem item = navItemList.stream()
                .filter(n -> n.id().equals(currentNavItemId))
                .findFirst().orElse(null);

        Class<? extends IPerspectiveFactory> factoryClass =
                (item != null && item.isLeaf()) ? item.layoutFactory() : pd.factoryClass();

        buildPerspectiveLayout(perspId, currentNavItemId, factoryClass);
        selectNavItem(currentNavItemId);
        statusBar.setStatus("Ready");
    }

    /**
     * Core layout construction — creates factory layout, wires PerspectiveLayout,
     * swaps into contentWrapper.
     */
    private void buildPerspectiveLayout(String perspId, String navItemId,
                                        Class<? extends IPerspectiveFactory> factoryClass) {
        IPerspectiveFactory factory = ctx.getBean(factoryClass);
        PageLayout pl = new PageLayout();
        factory.createInitialLayout(pl);

        EditorArea editorArea = new EditorArea();
        editorArea.setPage(page);
        editorArea.setMinimizedBar(minimizedBar);

        minimizedBar.removeAll();

        if (perspectiveLayout != null) {
            contentWrapper.remove(perspectiveLayout);
        }

        String key = stateKey(perspId, navItemId);
        PerspectiveState incomingState = allStates.computeIfAbsent(key, k -> new PerspectiveState());

        perspectiveLayout = new PerspectiveLayout(pl, page, editorArea, registry, minimizedBar, incomingState);
        contentWrapper.addComponentAtIndex(0, perspectiveLayout);
        contentWrapper.setFlexGrow(1, perspectiveLayout);

        // Let the perspective open its default editors (e.g. a data grid editor).
        // perspectiveLayout is now set, so editorOpened callbacks will work.
        factory.createInitialEditors(page);
    }

    private void saveOutgoingState(String perspId, String navItemId) {
        if (perspectiveLayout != null && perspId != null) {
            allStates.computeIfAbsent(stateKey(perspId, navItemId), k -> new PerspectiveState())
                     .setMinimizedIds(perspectiveLayout.getCurrentlyMinimized());
        }
    }

    private PerspectiveNavItem resolveDefaultNavItem(List<PerspectiveNavItem> items) {
        List<PerspectiveNavItem> leaves = items.stream().filter(PerspectiveNavItem::isLeaf).toList();
        if (leaves.isEmpty()) return null;
        return leaves.stream().filter(PerspectiveNavItem::defaultItem).findFirst()
                .orElse(leaves.get(0));
    }

    // -------------------------------------------------------------------------
    // Nav panel (toggle lives inside the panel itself)
    // -------------------------------------------------------------------------

    /**
     * Repopulate the nav panel for the incoming perspective.
     * Always resets to the expanded state on perspective switch.
     */
    private void updateNavDrawer(List<PerspectiveNavItem> items, String selectedId) {
        navPanel.removeAll();
        navLeafItems.clear();
        navPanelToggle = null;
        navSideNav     = null;

        boolean hasNav = items.size() > 1;
        navPanel.setVisible(hasNav);

        if (!hasNav) return;

        // ---- Toggle row (always visible, even when panel is collapsed) ----
        Icon toggleInitIcon = new Icon(VaadinIcon.CHEVRON_LEFT);
        toggleInitIcon.setSize("18px");
        navPanelToggle = new Button(toggleInitIcon);
        navPanelToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        navPanelToggle.addClassName("nav-panel-toggle-btn");
        navPanelToggle.setTooltipText("Collapse");
        navPanelToggle.addClickListener(e -> setNavPanelExpanded(!navPanelExpanded));

        HorizontalLayout toggleRow = new HorizontalLayout(navPanelToggle);
        toggleRow.addClassName("nav-panel-toggle-row");
        toggleRow.setWidthFull();
        toggleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toggleRow.setPadding(false);
        toggleRow.setSpacing(false);

        // ---- SideNav ----
        navSideNav = buildPerspectiveSideNav(items);

        navPanel.add(toggleRow, navSideNav);
        navPanel.setFlexGrow(1, navSideNav);

        // Reset to expanded on perspective switch
        navPanelExpanded = true;
        applyNavPanelExpansion();

        selectNavItem(selectedId);
    }

    /**
     * Expand or collapse the nav panel between 220 px (full) and 28 px (toggle-only).
     */
    private void setNavPanelExpanded(boolean expanded) {
        navPanelExpanded = expanded;
        applyNavPanelExpansion();
    }

    private void applyNavPanelExpansion() {
        navPanel.setWidth(navPanelExpanded ? "220px" : "36px");
        if (navSideNav != null) {
            navSideNav.setVisible(navPanelExpanded);
        }
        if (navPanelToggle != null) {
            Icon icon = new Icon(navPanelExpanded ? VaadinIcon.CHEVRON_LEFT : VaadinIcon.CHEVRON_RIGHT);
            icon.setSize("18px");
            navPanelToggle.setIcon(icon);
            navPanelToggle.setTooltipText(navPanelExpanded ? "Collapse" : "Expand");
        }
    }

    // -------------------------------------------------------------------------
    // SideNav construction
    // -------------------------------------------------------------------------

    private SideNav buildPerspectiveSideNav(List<PerspectiveNavItem> items) {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        List<PerspectiveNavItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(PerspectiveNavItem::order));

        List<PerspectiveNavItem> topLevel = new ArrayList<>();
        Map<String, List<PerspectiveNavItem>> childrenByParent = new LinkedHashMap<>();

        for (PerspectiveNavItem item : sorted) {
            if (item.parentId() == null) {
                topLevel.add(item);
            } else {
                childrenByParent
                        .computeIfAbsent(item.parentId(), k -> new ArrayList<>())
                        .add(item);
            }
        }

        for (PerspectiveNavItem item : topLevel) {
            if (item.isGroup()) {
                SideNavItem groupItem = createGroupNavItem(item);
                childrenByParent.getOrDefault(item.id(), List.of())
                        .forEach(child -> groupItem.addItem(createLeafNavItem(child)));
                groupItem.setExpanded(true);
                nav.addItem(groupItem);
            } else {
                nav.addItem(createLeafNavItem(item));
            }
        }

        return nav;
    }

    private SideNavItem createGroupNavItem(PerspectiveNavItem item) {
        SideNavItem navItem = new SideNavItem(item.label());
        if (item.icon() != null) {
            navItem.setPrefixComponent(new Icon(item.icon()));
        }
        return navItem;
    }

    private SideNavItem createLeafNavItem(PerspectiveNavItem item) {
        SideNavItem navItem = new SideNavItem(item.label());
        if (item.icon() != null) {
            navItem.setPrefixComponent(new Icon(item.icon()));
        }
        navItem.getElement().addEventListener("click", e -> onNavItemSelected(item.id()));
        navLeafItems.put(item.id(), navItem);
        return navItem;
    }

    private void selectNavItem(String id) {
        navLeafItems.forEach((itemId, navItem) ->
                navItem.getElement().setProperty("active", itemId.equals(id)));
    }

    // -------------------------------------------------------------------------
    // WorkbenchPageListener
    // -------------------------------------------------------------------------

    @Override
    public void editorOpened(IEditorPart editor, IEditorInput input) {
        if (perspectiveLayout != null) {
            Class<?> cls = AopUtils.getTargetClass(editor);
            RcpEditor ann = cls.getAnnotation(RcpEditor.class);
            VaadinIcon icon = (ann != null)
                    ? registry.findEditor(ann.id()).map(EditorDescriptor::icon).orElse(VaadinIcon.PENCIL)
                    : VaadinIcon.PENCIL;
            perspectiveLayout.getEditorArea().openTab(editor, input, icon);
        }
        statusBar.setStatus("Editing: " + input.getName());
    }

    @Override
    public void editorClosed(IEditorInput input) {
        if (perspectiveLayout != null) {
            perspectiveLayout.getEditorArea().closeTab(input);
        }
        statusBar.setStatus(page.getOpenEditors().isEmpty() ? "Ready" : "Editing");
    }

    @Override
    public void perspectiveChanged(String perspectiveId) {
        currentNavItemId = null;
        rebuildLayout();
    }

    @Override
    public void viewVisibilityChanged(String viewId, boolean visible) {
        // Views are wired during rebuildLayout; no incremental update needed
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void onPerspectiveSelected(String perspectiveId) {
        if (!perspectiveId.equals(page.getActivePerspectiveId())) {
            page.setPerspective(perspectiveId);
        }
    }

    private void onNavItemSelected(String navItemId) {
        if (!navItemId.equals(currentNavItemId)) {
            saveOutgoingState(currentPerspectiveId, currentNavItemId);
            currentNavItemId = navItemId;
            rebuildNavItemLayout();
        }
    }

    private void onSystemAction(String actionId) {
        String name = switch (actionId) {
            case "userProfile"        -> "User Profile";
            case "systemPreferences"  -> "Preferences";
            default                   -> actionId;
        };
        page.openEditor(new SystemEditorInput(actionId, name), actionId);
    }
}
