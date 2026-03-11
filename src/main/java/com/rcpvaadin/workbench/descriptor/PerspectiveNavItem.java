package com.rcpvaadin.workbench.descriptor;

import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.vaadin.flow.component.icon.VaadinIcon;

public record PerspectiveNavItem(
        String id,
        String label,
        VaadinIcon icon,
        Class<? extends IPerspectiveFactory> layoutFactory,  // null = group label
        String parentId,   // null = top-level
        int order,
        boolean defaultItem
) {
    public boolean isLeaf()  { return layoutFactory != null; }
    public boolean isGroup() { return layoutFactory == null; }
}
