package com.rcpvaadin.workbench;

import java.util.List;

public interface IWorkbenchPage {
    IEditorPart openEditor(IEditorInput input, String editorId);
    void closeEditor(IEditorInput input);

    void showView(String viewId);
    void hideView(String viewId);
    boolean isViewVisible(String viewId);
    IViewPart getOpenView(String viewId);

    void setPerspective(String perspectiveId);
    String getActivePerspectiveId();
    List<IEditorPart> getOpenEditors();
}
