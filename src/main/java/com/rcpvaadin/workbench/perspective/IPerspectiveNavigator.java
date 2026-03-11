package com.rcpvaadin.workbench.perspective;

import com.rcpvaadin.workbench.descriptor.PerspectiveNavItem;

import java.util.List;

public interface IPerspectiveNavigator {
    List<PerspectiveNavItem> getNavItems();
}
