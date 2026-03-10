package com.rcpvaadin.workbench;

import com.rcpvaadin.workbench.registry.WorkbenchRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class Workbench implements IWorkbench {

    @Autowired private WorkbenchRegistry registry;
    @Autowired private ApplicationContext ctx;

    @Override
    public WorkbenchRegistry getRegistry() {
        return registry;
    }

    @Override
    public IWorkbenchPage createPage() {
        return new WorkbenchPage(this, ctx);
    }
}
