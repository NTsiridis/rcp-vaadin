package com.rcpvaadin.sample.perspective;

import com.rcpvaadin.workbench.annotation.RcpPerspective;
import com.rcpvaadin.workbench.descriptor.PerspectiveNavItem;
import com.rcpvaadin.workbench.perspective.IPageLayout;
import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.rcpvaadin.workbench.perspective.IPerspectiveNavigator;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.stereotype.Component;

import java.util.List;

@RcpPerspective(id = "javaPerspective", name = "Java", icon = VaadinIcon.CODE)
public class JavaPerspective implements IPerspectiveFactory, IPerspectiveNavigator {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        // Fallback layout (used when no nav items are active)
        layout.addView("projectExplorer", IPageLayout.LEFT,   0.25f, IPageLayout.ID_EDITOR_AREA);
        layout.addView("outline",         IPageLayout.RIGHT,  0.25f, IPageLayout.ID_EDITOR_AREA);
        layout.addView("console",         IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA);
    }

    @Override
    public List<PerspectiveNavItem> getNavItems() {
        return List.of(
                // Groups
                new PerspectiveNavItem("dev",          "Development",   VaadinIcon.CODE,         null,                    null,  0, false),
                new PerspectiveNavItem("collab",       "Collaboration", VaadinIcon.GROUP,        null,                    null,  10, false),
                // Development children
                new PerspectiveNavItem("coding",       "Coding",        VaadinIcon.EDIT,         JavaCodingLayout.class,  "dev", 1, true),
                new PerspectiveNavItem("codeReview",   "Code Review",   VaadinIcon.EYE,          JavaReviewLayout.class,  "dev", 2, false),
                // Collaboration children
                new PerspectiveNavItem("team",         "Team",          VaadinIcon.USERS,        JavaTeamLayout.class,    "collab", 1, false)
        );
    }

    // -------------------------------------------------------------------------
    // Companion layout factories (package-private, Spring-managed)
    // -------------------------------------------------------------------------

    @Component
    static class JavaCodingLayout implements IPerspectiveFactory {
        @Override
        public void createInitialLayout(IPageLayout layout) {
            layout.addView("projectExplorer", IPageLayout.LEFT,   0.25f, IPageLayout.ID_EDITOR_AREA);
            layout.addView("outline",         IPageLayout.RIGHT,  0.25f, IPageLayout.ID_EDITOR_AREA);
            layout.addView("console",         IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA);
        }
    }

    @Component
    static class JavaReviewLayout implements IPerspectiveFactory {
        @Override
        public void createInitialLayout(IPageLayout layout) {
            layout.addView("outline",  IPageLayout.LEFT,   0.25f, IPageLayout.ID_EDITOR_AREA);
            layout.addView("console",  IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA);
        }
    }

    @Component
    static class JavaTeamLayout implements IPerspectiveFactory {
        @Override
        public void createInitialLayout(IPageLayout layout) {
            layout.addView("projectExplorer", IPageLayout.LEFT,   0.25f, IPageLayout.ID_EDITOR_AREA);
            layout.addView("console",         IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA);
        }
    }
}
