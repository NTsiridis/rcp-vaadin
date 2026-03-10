package com.rcpvaadin.workbench.perspective;

import com.rcpvaadin.sample.perspective.DebugPerspective;
import com.rcpvaadin.sample.perspective.JavaPerspective;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageLayoutTest {

    @Test
    void javaLayoutHasThreePlacements() {
        PageLayout layout = new PageLayout();
        new JavaPerspective().createInitialLayout(layout);
        assertThat(layout.getPlacements()).hasSize(3);
    }

    @Test
    void javaLayoutHasCorrectViews() {
        PageLayout layout = new PageLayout();
        new JavaPerspective().createInitialLayout(layout);
        assertThat(layout.getPlacements())
                .extracting(PageLayout.ViewPlacement::viewId)
                .containsExactlyInAnyOrder("projectExplorer", "outline", "console");
    }

    @Test
    void callStackIsRelativeToVariables() {
        PageLayout layout = new PageLayout();
        new DebugPerspective().createInitialLayout(layout);
        assertThat(layout.getPlacements())
                .filteredOn(p -> "callStack".equals(p.viewId()))
                .singleElement()
                .extracting(PageLayout.ViewPlacement::refPartId)
                .isEqualTo("variables");
    }
}
