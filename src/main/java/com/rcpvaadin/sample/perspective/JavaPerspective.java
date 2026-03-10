package com.rcpvaadin.sample.perspective;

import com.rcpvaadin.workbench.annotation.RcpPerspective;
import com.rcpvaadin.workbench.perspective.IPageLayout;
import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.vaadin.flow.component.icon.VaadinIcon;

@RcpPerspective(id = "javaPerspective", name = "Java", icon = VaadinIcon.CODE)
public class JavaPerspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        layout.addView("projectExplorer", IPageLayout.LEFT,   0.25f, IPageLayout.ID_EDITOR_AREA);
        layout.addView("outline",         IPageLayout.RIGHT,  0.25f, IPageLayout.ID_EDITOR_AREA);
        layout.addView("console",         IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA);
    }
}
