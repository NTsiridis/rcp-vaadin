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
import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.rcpvaadin.workbench.perspective.PageLayout;
import com.rcpvaadin.workbench.registry.WorkbenchRegistry;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

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

    private final Map<String, PerspectiveState> perspectiveStates = new HashMap<>();
    private String currentPerspectiveId = null;

    private PerspectiveLayout perspectiveLayout;
    private PerspectiveBar    perspectiveBar;

    @Autowired
    public WorkbenchView(Workbench workbench, WorkbenchRegistry registry, ApplicationContext ctx) {
        this.registry = registry;
        this.ctx      = ctx;
        this.page     = workbench.createPage();

        ((WorkbenchPage) page).addListener(this);

        // --- Workbench header (navbar) ---
        addToNavbar(buildHeader());

        // --- Sidebar ---
        perspectiveBar = new PerspectiveBar(
                registry.getAllPerspectives(),
                this::onPerspectiveSelected,
                this::onSystemAction);

        // contentWrapper holds [perspectiveLayout (flex:1), statusBar] permanently.
        contentWrapper.setSizeFull();
        contentWrapper.setPadding(false);
        contentWrapper.setSpacing(false);
        contentWrapper.add(statusBar);   // statusBar added first; perspectiveLayout prepended later

        HorizontalLayout outerLayout = new HorizontalLayout(perspectiveBar, contentWrapper, minimizedBar);
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
        logo.setSize("22px");
        logo.getStyle().set("color", "#f59e0b");   /* amber — visible on light navbar */

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
    // Layout rebuild
    // -------------------------------------------------------------------------

    private void rebuildLayout() {
        String newPerspId = page.getActivePerspectiveId();
        if (newPerspId == null) return;

        // 1. Save outgoing minimized state (splitter positions are saved live via drag listener)
        if (perspectiveLayout != null && currentPerspectiveId != null) {
            perspectiveStates
                    .computeIfAbsent(currentPerspectiveId, k -> new PerspectiveState())
                    .setMinimizedIds(perspectiveLayout.getCurrentlyMinimized());
        }

        PerspectiveDescriptor pd = registry.findPerspective(newPerspId).orElse(null);
        if (pd == null) return;

        IPerspectiveFactory factory = ctx.getBean(pd.factoryClass());
        PageLayout pl = new PageLayout();
        factory.createInitialLayout(pl);

        EditorArea editorArea = new EditorArea();
        editorArea.setPage(page);

        minimizedBar.removeAll();

        if (perspectiveLayout != null) {
            contentWrapper.remove(perspectiveLayout);
        }

        // 2. Retrieve or create incoming state
        PerspectiveState incomingState =
                perspectiveStates.computeIfAbsent(newPerspId, k -> new PerspectiveState());

        perspectiveLayout = new PerspectiveLayout(pl, page, editorArea, registry, minimizedBar, incomingState);
        contentWrapper.addComponentAtIndex(0, perspectiveLayout);
        contentWrapper.setFlexGrow(1, perspectiveLayout);

        perspectiveBar.selectPerspective(newPerspId);
        statusBar.setPerspective(pd.name());
        statusBar.setStatus("Ready");

        currentPerspectiveId = newPerspId;   // must be last
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

    private void onSystemAction(String actionId) {
        String name = switch (actionId) {
            case "userProfile"        -> "User Profile";
            case "systemPreferences"  -> "Preferences";
            default                   -> actionId;
        };
        page.openEditor(new SystemEditorInput(actionId, name), actionId);
    }
}
