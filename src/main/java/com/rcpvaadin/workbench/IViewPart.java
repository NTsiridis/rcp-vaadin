package com.rcpvaadin.workbench;

import com.vaadin.flow.component.Component;

public interface IViewPart extends IWorkbenchPart {
    Component createPartControl();
}
