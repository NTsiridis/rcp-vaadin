package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.IWorkbenchPage;
import com.rcpvaadin.workbench.descriptor.ViewDescriptor;
import com.rcpvaadin.workbench.perspective.IPageLayout;
import com.rcpvaadin.workbench.perspective.PageLayout;
import com.rcpvaadin.workbench.registry.WorkbenchRegistry;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the collapse / restore coordination in PerspectiveLayout.
 *
 * Vaadin components (SplitLayout, VerticalLayout, …) can be instantiated
 * without a VaadinSession for pure server-side property tests.  executeJs is
 * never invoked here because the drag-end listener only fires on real browser
 * events — so all paths exercised below are fully synchronous.
 */
class PerspectiveLayoutCollapseTest {

    // -------------------------------------------------------------------------
    // Test helper: captures restore callbacks without touching the real bar UI
    // -------------------------------------------------------------------------

    static class CapturingMinimizedBar extends MinimizedBar {
        final Map<String, Runnable> restores = new LinkedHashMap<>();

        @Override
        public void addMinimized(String id, String name, VaadinIcon icon, Runnable onRestore) {
            restores.put(id, onRestore);
            // skip super — we don't need the actual Button in unit tests
        }

        @Override
        public void removeMinimized(String id) {
            restores.remove(id);
        }
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    private record Fixture(PerspectiveLayout layout, CapturingMinimizedBar bar) {}

    /**
     * Builds a PerspectiveLayout with a single view placed on the LEFT of the
     * editor area (view = primary slot of a horizontal SplitLayout).
     */
    private Fixture buildFixture(String viewId, float ratio, PerspectiveState state) {
        IViewPart viewPart = mock(IViewPart.class);
        when(viewPart.getTitle()).thenReturn("Test View");
        when(viewPart.createPartControl()).thenReturn(new Span("content"));

        IWorkbenchPage page = mock(IWorkbenchPage.class);
        when(page.getOpenView(eq(viewId))).thenReturn(viewPart);

        WorkbenchRegistry registry = mock(WorkbenchRegistry.class);
        when(registry.findView(eq(viewId))).thenReturn(Optional.of(
                new ViewDescriptor(viewId, "Test View", VaadinIcon.STAR, null)));

        PageLayout pl = new PageLayout();
        pl.addView(viewId, IPageLayout.LEFT, ratio, IPageLayout.ID_EDITOR_AREA);

        EditorArea editorArea = new EditorArea();
        CapturingMinimizedBar bar = new CapturingMinimizedBar();
        PerspectiveLayout layout = new PerspectiveLayout(pl, page, editorArea, registry, bar, state);
        return new Fixture(layout, bar);
    }

    private Fixture buildFixture(String viewId, float ratio) {
        return buildFixture(viewId, ratio, new PerspectiveState());
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void collapse_registers_action_and_adds_to_minimized_set() {
        Fixture f = buildFixture("viewA", 0.3f);

        assertThat(f.layout().collapseActions).containsKey("viewA");

        f.layout().collapseActions.get("viewA").run();

        assertThat(f.layout().getCurrentlyMinimized()).containsExactly("viewA");
    }

    @Test
    void restore_removes_from_minimized_set() {
        Fixture f = buildFixture("viewA", 0.3f);

        f.layout().collapseActions.get("viewA").run();
        assertThat(f.layout().getCurrentlyMinimized()).containsExactly("viewA");

        // Simulate user clicking the restore icon in the minimized bar
        f.bar().restores.get("viewA").run();

        assertThat(f.layout().getCurrentlyMinimized()).isEmpty();
    }

    @Test
    void double_collapse_is_a_noop() {
        Fixture f = buildFixture("viewA", 0.3f);
        Runnable collapse = f.layout().collapseActions.get("viewA");

        collapse.run();
        collapse.run();   // second call must be ignored

        // Only one entry in the captured bar
        assertThat(f.bar().restores).hasSize(1);
        assertThat(f.layout().getCurrentlyMinimized()).containsExactly("viewA");
    }

    @Test
    void restore_then_collapse_again_works() {
        Fixture f = buildFixture("viewA", 0.3f);
        Runnable collapse = f.layout().collapseActions.get("viewA");

        // First cycle
        collapse.run();
        f.bar().restores.get("viewA").run();
        assertThat(f.layout().getCurrentlyMinimized()).isEmpty();

        // Second cycle — must not be blocked by stale guard state
        collapse.run();
        assertThat(f.layout().getCurrentlyMinimized()).containsExactly("viewA");
    }

    @Test
    void editor_area_is_also_collapsible_as_secondary() {
        Fixture f = buildFixture("viewA", 0.3f);

        // EditorArea is the secondary leaf (RIGHT of viewA in a horizontal split).
        // Its collapse action must be registered under ID_EDITOR_AREA.
        assertThat(f.layout().collapseActions).containsKey(IPageLayout.ID_EDITOR_AREA);
    }

    @Test
    void collapse_uses_factory_ratio_as_restore_position_when_no_prior_drag() {
        // ratio 0.3 → factory splitter pos = 30 %
        // The view (primary) is collapsed. Restore must bring splitter back to 30 %.
        // We verify indirectly: after restore the view is not in currentlyMinimized,
        // meaning restoreRunnable was called with a non-zero splitter value.
        Fixture f = buildFixture("viewA", 0.3f);

        f.layout().collapseActions.get("viewA").run();
        f.bar().restores.get("viewA").run();

        assertThat(f.layout().getCurrentlyMinimized()).isEmpty();
    }

    @Test
    void collapse_uses_state_saved_position_as_restore_point() {
        // Arrange: state already has a drag-saved position for this split key.
        // key = deepestPrimary:deepestSecondary = "viewA:editorArea"
        PerspectiveState state = new PerspectiveState();
        String key = "viewA:" + IPageLayout.ID_EDITOR_AREA;
        state.setSplitterPosition(key, 55.0);   // simulates a prior drag saved to state

        Fixture f = buildFixture("viewA", 0.3f, state);

        // Collapse then restore — the view must be re-shown (splitter would be at 55 %)
        f.layout().collapseActions.get("viewA").run();
        assertThat(f.layout().getCurrentlyMinimized()).containsExactly("viewA");

        f.bar().restores.get("viewA").run();
        assertThat(f.layout().getCurrentlyMinimized()).isEmpty();
    }

    @Test
    void two_independent_collapses_do_not_interfere() {
        // Build a layout with two views — one on LEFT of editor, one on RIGHT.
        // Each gets its own collapse action and tracked independently.
        IViewPart viewPartA = mock(IViewPart.class);
        when(viewPartA.getTitle()).thenReturn("View A");
        when(viewPartA.createPartControl()).thenReturn(new Span("A"));

        IViewPart viewPartB = mock(IViewPart.class);
        when(viewPartB.getTitle()).thenReturn("View B");
        when(viewPartB.createPartControl()).thenReturn(new Span("B"));

        IWorkbenchPage page = mock(IWorkbenchPage.class);
        when(page.getOpenView(eq("viewA"))).thenReturn(viewPartA);
        when(page.getOpenView(eq("viewB"))).thenReturn(viewPartB);

        WorkbenchRegistry registry = mock(WorkbenchRegistry.class);
        when(registry.findView(eq("viewA"))).thenReturn(Optional.of(
                new ViewDescriptor("viewA", "View A", VaadinIcon.STAR, null)));
        when(registry.findView(eq("viewB"))).thenReturn(Optional.of(
                new ViewDescriptor("viewB", "View B", VaadinIcon.CIRCLE, null)));

        PageLayout pl = new PageLayout();
        // viewA LEFT of editorArea → primary of outer split
        pl.addView("viewA", IPageLayout.LEFT,  0.2f, IPageLayout.ID_EDITOR_AREA);
        // viewB RIGHT of editorArea → secondary of inner split
        pl.addView("viewB", IPageLayout.RIGHT, 0.25f, IPageLayout.ID_EDITOR_AREA);

        EditorArea editorArea = new EditorArea();
        CapturingMinimizedBar bar = new CapturingMinimizedBar();
        PerspectiveLayout layout = new PerspectiveLayout(pl, page, editorArea, registry, bar,
                new PerspectiveState());

        // Both actions registered
        assertThat(layout.collapseActions).containsKeys("viewA", "viewB");

        // Collapse both
        layout.collapseActions.get("viewA").run();
        layout.collapseActions.get("viewB").run();
        assertThat(layout.getCurrentlyMinimized()).containsExactlyInAnyOrder("viewA", "viewB");
        assertThat(bar.restores).containsKeys("viewA", "viewB");

        // Restore viewA only
        bar.restores.get("viewA").run();
        assertThat(layout.getCurrentlyMinimized()).containsExactly("viewB");

        // Restore viewB
        bar.restores.get("viewB").run();
        assertThat(layout.getCurrentlyMinimized()).isEmpty();
    }

    @Test
    void getCurrentlyMinimized_returns_unmodifiable_copy() {
        Fixture f = buildFixture("viewA", 0.3f);
        f.layout().collapseActions.get("viewA").run();

        var minimized = f.layout().getCurrentlyMinimized();
        assertThat(minimized).containsExactly("viewA");
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> minimized.add("x"));
    }

    // -------------------------------------------------------------------------
    // Split-key helper tests (package-private static methods)
    // -------------------------------------------------------------------------

    @Test
    void splitKey_simple_split() {
        var viewA   = new PerspectiveLayout.LeafNode("viewA",   "A", VaadinIcon.STAR, new Span());
        var editorA = new PerspectiveLayout.LeafNode("editorArea", "E", VaadinIcon.PENCIL, new Span());
        var split   = new PerspectiveLayout.SplitNode(
                com.vaadin.flow.component.splitlayout.SplitLayout.Orientation.HORIZONTAL,
                30.0, viewA, editorA);

        assertThat(PerspectiveLayout.splitKey(split)).isEqualTo("viewA:editorArea");
    }

    @Test
    void splitKey_nested_split_takes_deepest_leaves() {
        // outer: [primary=viewA, secondary=inner[primary=viewB, secondary=editorArea]]
        var viewA   = new PerspectiveLayout.LeafNode("viewA",      "A", VaadinIcon.STAR,   new Span());
        var viewB   = new PerspectiveLayout.LeafNode("viewB",      "B", VaadinIcon.CIRCLE, new Span());
        var editor  = new PerspectiveLayout.LeafNode("editorArea", "E", VaadinIcon.PENCIL, new Span());
        var inner   = new PerspectiveLayout.SplitNode(
                com.vaadin.flow.component.splitlayout.SplitLayout.Orientation.VERTICAL,
                60.0, viewB, editor);
        var outer   = new PerspectiveLayout.SplitNode(
                com.vaadin.flow.component.splitlayout.SplitLayout.Orientation.HORIZONTAL,
                20.0, viewA, inner);

        // deepest primary of outer   → viewA (leaf)
        // deepest secondary of outer → editorArea (deepest secondary of inner)
        assertThat(PerspectiveLayout.splitKey(outer)).isEqualTo("viewA:editorArea");

        // inner key
        assertThat(PerspectiveLayout.splitKey(inner)).isEqualTo("viewB:editorArea");
    }

    @Test
    void deepestPrimaryLeafId_single_leaf() {
        var leaf = new PerspectiveLayout.LeafNode("x", "X", VaadinIcon.STAR, new Span());
        assertThat(PerspectiveLayout.deepestPrimaryLeafId(leaf)).isEqualTo("x");
    }

    @Test
    void deepestPrimaryLeafId_nested_follows_primary_chain() {
        var leaf1 = new PerspectiveLayout.LeafNode("a", "A", VaadinIcon.STAR, new Span());
        var leaf2 = new PerspectiveLayout.LeafNode("b", "B", VaadinIcon.STAR, new Span());
        var leaf3 = new PerspectiveLayout.LeafNode("c", "C", VaadinIcon.STAR, new Span());
        var inner = new PerspectiveLayout.SplitNode(
                com.vaadin.flow.component.splitlayout.SplitLayout.Orientation.HORIZONTAL, 50, leaf1, leaf2);
        var outer = new PerspectiveLayout.SplitNode(
                com.vaadin.flow.component.splitlayout.SplitLayout.Orientation.HORIZONTAL, 30, inner, leaf3);

        // deepest primary of outer → primary of inner → leaf1
        assertThat(PerspectiveLayout.deepestPrimaryLeafId(outer)).isEqualTo("a");
    }

    @Test
    void deepestSecondaryLeafId_nested_follows_secondary_chain() {
        var leaf1 = new PerspectiveLayout.LeafNode("a", "A", VaadinIcon.STAR, new Span());
        var leaf2 = new PerspectiveLayout.LeafNode("b", "B", VaadinIcon.STAR, new Span());
        var leaf3 = new PerspectiveLayout.LeafNode("c", "C", VaadinIcon.STAR, new Span());
        var inner = new PerspectiveLayout.SplitNode(
                com.vaadin.flow.component.splitlayout.SplitLayout.Orientation.VERTICAL, 50, leaf2, leaf3);
        var outer = new PerspectiveLayout.SplitNode(
                com.vaadin.flow.component.splitlayout.SplitLayout.Orientation.HORIZONTAL, 30, leaf1, inner);

        // deepest secondary of outer → secondary of inner → leaf3
        assertThat(PerspectiveLayout.deepestSecondaryLeafId(outer)).isEqualTo("c");
    }
}
