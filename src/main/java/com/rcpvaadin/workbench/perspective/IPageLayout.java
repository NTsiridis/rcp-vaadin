package com.rcpvaadin.workbench.perspective;

public interface IPageLayout {
    int LEFT   = 1;
    int RIGHT  = 2;
    int TOP    = 3;
    int BOTTOM = 4;

    String ID_EDITOR_AREA = "editorArea";

    void addView(String viewId, int relationship, float ratio, String refPartId);
}
