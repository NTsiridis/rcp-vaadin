package com.rcpvaadin.sample.perspective;

import com.rcpvaadin.workbench.annotation.RcpPerspective;
import com.rcpvaadin.workbench.perspective.IPageLayout;
import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.vaadin.flow.component.icon.VaadinIcon;

@RcpPerspective(id = "debugPerspective", name = "Debug", icon = VaadinIcon.BUG)
public class DebugPerspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        layout.addView("variables", IPageLayout.LEFT,   0.25f, IPageLayout.ID_EDITOR_AREA);
        layout.addView("callStack", IPageLayout.BOTTOM, 0.40f, "variables");
        layout.addView("console",   IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA);
    }
}
