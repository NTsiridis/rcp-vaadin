package com.rcpvaadin.workbench;

import com.rcpvaadin.workbench.registry.WorkbenchRegistry;

public interface IWorkbench {
    WorkbenchRegistry getRegistry();
    IWorkbenchPage createPage();
}
