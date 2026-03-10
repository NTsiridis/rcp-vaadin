package com.rcpvaadin.workbench;

import com.vaadin.flow.component.Component;

public interface IEditorPart extends IWorkbenchPart {
    void setInput(IEditorInput input);
    IEditorInput getEditorInput();
    boolean isDirty();
    Component createPartControl();
}
