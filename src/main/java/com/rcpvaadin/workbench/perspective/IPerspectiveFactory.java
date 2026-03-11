package com.rcpvaadin.workbench.perspective;

public interface IPerspectiveFactory {
    void createInitialLayout(IPageLayout layout);

    /** Override to open default editors when this perspective is first activated. */
    default void createInitialEditors(com.rcpvaadin.workbench.IWorkbenchPage page) {}
}
