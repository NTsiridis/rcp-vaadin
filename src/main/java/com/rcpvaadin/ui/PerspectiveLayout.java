package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.IWorkbenchPage;
import com.rcpvaadin.workbench.PartSite;
import com.rcpvaadin.workbench.descriptor.ViewDescriptor;
import com.rcpvaadin.workbench.perspective.IPageLayout;
import com.rcpvaadin.workbench.perspective.PageLayout;
import com.rcpvaadin.workbench.registry.WorkbenchRegistry;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders a PageLayout as a tree of nested SplitLayouts.
 *
 * The algorithm builds a sealed LayoutNode tree first, then recurses to produce
 * the actual Vaadin component hierarchy. This keeps the placement logic separate
 * from the rendering logic.
 *
 * Splitter position tracking uses a server-side double[] per SplitLayout (not async JS)
 * because ViewContainer/EditorArea call setVisible(false) before the collapse callback —
 * so by the time async JS would run, getBoundingClientRect() returns 0 for the hidden
 * component, yielding a wrong (0) restore position.
 * Drag-end tracking is the only async JS path, since the server has no other way
 * to observe a user drag.
 */
public class PerspectiveLayout extends VerticalLayout {

    // -------------------------------------------------------------------------
    // Sealed node tree
    // -------------------------------------------------------------------------

    sealed interface LayoutNode permits PerspectiveLayout.LeafNode, PerspectiveLayout.SplitNode {}

    record LeafNode(String id, String name, VaadinIcon icon, Component component) implements LayoutNode {}

    record SplitNode(
            SplitLayout.Orientation orientation,
            double splitterPos,
            LayoutNode primary,
            LayoutNode secondary) implements LayoutNode {}

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final EditorArea             editorArea;
    private final MinimizedBar           minimizedBar;
    private final PerspectiveState       state;
    private final IWorkbenchPage         page;

    // Package-private for tests — keyed by part id
    final Map<String, Runnable>  collapseActions    = new LinkedHashMap<>();
    final Set<String>            currentlyMinimized = new LinkedHashSet<>();

    // Tree root (stored after build so maximize can walk it)
    private LayoutNode rootNode;

    // Live SplitLayouts keyed by splitKey
    private final Map<String, SplitLayout> splitLayouts   = new LinkedHashMap<>();

    // Effective (server-tracked) splitter positions keyed by splitKey
    private final Map<String, double[]>    splitPositions = new LinkedHashMap<>();

    // Maximize state
    private String      maximizedPartId      = null;
    private Collapsible maximizedCollapsible = null;
    private Map<String, Double> maximizeSnapshot = null;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public PerspectiveLayout(PageLayout pageLayout, IWorkbenchPage page, EditorArea editorArea,
                             WorkbenchRegistry registry, MinimizedBar minimizedBar,
                             PerspectiveState state) {
        this.editorArea   = editorArea;
        this.minimizedBar = minimizedBar;
        this.state        = state;
        this.page         = page;
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // 1. Open all views through the model so state & UI are in sync
        for (PageLayout.ViewPlacement p : pageLayout.getPlacements()) {
            page.showView(p.viewId());
        }

        // 2. Build LayoutNode tree starting from editor area as root
        LayoutNode root = new LeafNode(IPageLayout.ID_EDITOR_AREA, "Editors", VaadinIcon.PENCIL, editorArea);
        for (PageLayout.ViewPlacement p : pageLayout.getPlacements()) {
            IViewPart viewPart = page.getOpenView(p.viewId());
            if (viewPart == null) continue;

            ViewDescriptor vd = registry.findView(p.viewId()).orElse(null);
            VaadinIcon icon = (vd != null) ? vd.icon() : VaadinIcon.SQUARE_SHADOW;
            String name     = (vd != null) ? vd.name() : p.viewId();

            Component viewComponent = new ViewContainer(viewPart, icon);
            LeafNode viewLeaf = new LeafNode(p.viewId(), name, icon, viewComponent);
            root = insertAt(root, p.refPartId(), p, viewLeaf);
        }

        // 3. Store root for maximize tree-walk
        this.rootNode = root;

        // 4. Render the tree into nested SplitLayouts (also wires collapseActions)
        Component rendered = renderNode(root);
        add(rendered);
        setFlexGrow(1, rendered);

        // 5. Re-apply panels that were minimized before the perspective was switched away
        restoreMinimizedState();
    }

    public EditorArea getEditorArea() {
        return editorArea;
    }

    public Set<String> getCurrentlyMinimized() {
        return Set.copyOf(currentlyMinimized);
    }

    // -------------------------------------------------------------------------
    // Tree manipulation
    // -------------------------------------------------------------------------

    /**
     * Recursively finds the node with {@code refId} and wraps it in a SplitNode
     * together with the new view leaf. Returns the (possibly unchanged) node.
     */
    private LayoutNode insertAt(LayoutNode node, String refId,
                                PageLayout.ViewPlacement p, LeafNode viewLeaf) {
        return switch (node) {
            case LeafNode leaf when leaf.id().equals(refId) -> {
                SplitLayout.Orientation orientation = switch (p.relationship()) {
                    case IPageLayout.LEFT, IPageLayout.RIGHT -> SplitLayout.Orientation.HORIZONTAL;
                    default -> SplitLayout.Orientation.VERTICAL;
                };
                boolean viewIsPrimary = p.relationship() == IPageLayout.LEFT
                        || p.relationship() == IPageLayout.TOP;
                double splitterPos = viewIsPrimary
                        ? p.ratio() * 100.0
                        : (1.0 - p.ratio()) * 100.0;
                yield viewIsPrimary
                        ? new SplitNode(orientation, splitterPos, viewLeaf, leaf)
                        : new SplitNode(orientation, splitterPos, leaf, viewLeaf);
            }
            case LeafNode leaf -> leaf;   // no match — return unchanged
            case SplitNode split -> new SplitNode(
                    split.orientation(),
                    split.splitterPos(),
                    insertAt(split.primary(), refId, p, viewLeaf),
                    insertAt(split.secondary(), refId, p, viewLeaf));
        };
    }

    // -------------------------------------------------------------------------
    // Split key helpers (package-private for tests)
    // -------------------------------------------------------------------------

    static String splitKey(SplitNode split) {
        return deepestPrimaryLeafId(split.primary()) + ":" + deepestSecondaryLeafId(split.secondary());
    }

    static String deepestPrimaryLeafId(LayoutNode n) {
        return switch (n) {
            case LeafNode l  -> l.id();
            case SplitNode s -> deepestPrimaryLeafId(s.primary());
        };
    }

    static String deepestSecondaryLeafId(LayoutNode n) {
        return switch (n) {
            case LeafNode l  -> l.id();
            case SplitNode s -> deepestSecondaryLeafId(s.secondary());
        };
    }

    // -------------------------------------------------------------------------
    // Path-finding for maximize
    // -------------------------------------------------------------------------

    private record PathStep(SplitNode node, boolean targetIsPrimary) {}

    /**
     * Returns the list of PathSteps from root down to the leaf with targetId,
     * or null if not found.
     */
    private static List<PathStep> pathTo(LayoutNode node, String targetId) {
        return switch (node) {
            case LeafNode l -> l.id().equals(targetId) ? new ArrayList<>() : null;
            case SplitNode s -> {
                List<PathStep> pPath = pathTo(s.primary(), targetId);
                if (pPath != null) { pPath.add(0, new PathStep(s, true));  yield pPath; }
                List<PathStep> sPath = pathTo(s.secondary(), targetId);
                if (sPath != null) { sPath.add(0, new PathStep(s, false)); yield sPath; }
                yield null;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Maximize / Unmaximize
    // -------------------------------------------------------------------------

    private void maximizePart(String partId, Collapsible source) {
        if (maximizedPartId != null) unmaximizePart();

        maximizeSnapshot = new HashMap<>();
        splitPositions.forEach((k, arr) -> maximizeSnapshot.put(k, arr[0]));

        List<PathStep> path = pathTo(rootNode, partId);
        if (path == null) return;
        for (PathStep step : path) {
            SplitLayout sl = splitLayouts.get(splitKey(step.node()));
            if (sl == null) continue;
            sl.setSplitterPosition(step.targetIsPrimary() ? 100.0 : 0.0);
        }

        maximizedPartId      = partId;
        maximizedCollapsible = source;
        source.setMaximizedState(true);
    }

    private void unmaximizePart() {
        if (maximizedPartId == null) return;

        if (maximizeSnapshot != null) {
            maximizeSnapshot.forEach((key, pos) -> {
                SplitLayout sl = splitLayouts.get(key);
                if (sl != null) sl.setSplitterPosition(pos);
            });
        }

        if (maximizedCollapsible != null) maximizedCollapsible.setMaximizedState(false);
        maximizedPartId      = null;
        maximizedCollapsible = null;
        maximizeSnapshot     = null;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private Component renderNode(LayoutNode node) {
        return switch (node) {
            case LeafNode leaf -> leaf.component();
            case SplitNode split -> {
                Component primaryComp   = renderNode(split.primary());
                Component secondaryComp = renderNode(split.secondary());

                SplitLayout layout = new SplitLayout(primaryComp, secondaryComp);
                layout.setOrientation(split.orientation());
                layout.setSizeFull();

                String key = splitKey(split);
                String dim = (split.orientation() == SplitLayout.Orientation.HORIZONTAL) ? "width" : "height";

                double[] currentPos = splitPositions.computeIfAbsent(
                        key, k -> new double[]{ state.getSplitterPosition(k, split.splitterPos()) });
                layout.setSplitterPosition(currentPos[0]);

                splitLayouts.put(key, layout);

                String readPosJs = """
                        var p = this.querySelector('[slot="primary"]');
                        if (!p) return -1;
                        var total = this.getBoundingClientRect()['%s'];
                        return total > 0 ? Math.round(p.getBoundingClientRect()['%s'] / total * 100) : -1;
                        """.formatted(dim, dim);
                layout.addSplitterDragendListener(e ->
                        layout.getElement().executeJs(readPosJs).then(Double.class, pos -> {
                            if (pos >= 0) {
                                currentPos[0] = pos;
                                state.setSplitterPosition(key, pos);
                            }
                        }));

                // ---- Primary leaf wiring ----
                if (split.primary() instanceof LeafNode ln && primaryComp instanceof Collapsible c) {
                    Runnable collapse = () -> {
                        if (ln.id().equals(maximizedPartId)) unmaximizePart();
                        if (currentlyMinimized.contains(ln.id())) return;
                        double restorePos = currentPos[0];
                        currentlyMinimized.add(ln.id());
                        primaryComp.setVisible(false);
                        layout.setSplitterPosition(0);
                        minimizedBar.addMinimized(ln.id(), ln.name(), ln.icon(), () -> {
                            primaryComp.setVisible(true);
                            layout.setSplitterPosition(restorePos);
                            currentlyMinimized.remove(ln.id());
                            minimizedBar.removeMinimized(ln.id());
                        });
                    };
                    collapseActions.put(ln.id(), collapse);
                    c.setCollapseSupport(collapse, () -> {});
                    c.setMaximizeSupport(
                            () -> maximizePart(ln.id(), c),
                            this::unmaximizePart
                    );

                    if (primaryComp instanceof ViewContainer vc && !ln.id().equals(IPageLayout.ID_EDITOR_AREA)) {
                        IViewPart vp = page.getOpenView(ln.id());
                        if (vp != null && vp.getSite() instanceof PartSite ps) {
                            vc.bindStatusBar(ps);
                        }
                    }
                }

                // ---- Secondary leaf wiring ----
                if (split.secondary() instanceof LeafNode ln && secondaryComp instanceof Collapsible c) {
                    Runnable collapse = () -> {
                        if (ln.id().equals(maximizedPartId)) unmaximizePart();
                        if (currentlyMinimized.contains(ln.id())) return;
                        double restorePos = currentPos[0];
                        currentlyMinimized.add(ln.id());
                        secondaryComp.setVisible(false);
                        layout.setSplitterPosition(100);
                        minimizedBar.addMinimized(ln.id(), ln.name(), ln.icon(), () -> {
                            secondaryComp.setVisible(true);
                            layout.setSplitterPosition(restorePos);
                            currentlyMinimized.remove(ln.id());
                            minimizedBar.removeMinimized(ln.id());
                        });
                    };
                    collapseActions.put(ln.id(), collapse);
                    c.setCollapseSupport(collapse, () -> {});
                    c.setMaximizeSupport(
                            () -> maximizePart(ln.id(), c),
                            this::unmaximizePart
                    );

                    if (secondaryComp instanceof ViewContainer vc && !ln.id().equals(IPageLayout.ID_EDITOR_AREA)) {
                        IViewPart vp = page.getOpenView(ln.id());
                        if (vp != null && vp.getSite() instanceof PartSite ps) {
                            vc.bindStatusBar(ps);
                        }
                    }
                }

                yield layout;
            }
        };
    }

    // -------------------------------------------------------------------------
    // State restore
    // -------------------------------------------------------------------------

    private void restoreMinimizedState() {
        for (String id : state.getMinimizedIds()) {
            Runnable action = collapseActions.get(id);
            if (action != null) action.run();
        }
    }
}
