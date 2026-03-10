package com.rcpvaadin.workbench;

public interface IWorkbenchPart {
    String getTitle();
    void init(IPartSite site);
    void dispose();
    IPartSite getSite();
}
