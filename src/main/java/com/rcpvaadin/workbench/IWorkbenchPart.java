package com.rcpvaadin.workbench;

import java.util.List;

public interface IWorkbenchPart {
    String getTitle();
    void init(IPartSite site);
    void dispose();
    IPartSite getSite();

    /**
     * Returns the toolbar items to display in this part's title bar.
     * Override to contribute action buttons; the default is an empty toolbar.
     */
    default List<ToolbarItem> getToolbarItems() { return List.of(); }
}
