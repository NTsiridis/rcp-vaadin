package com.rcpvaadin.workbench;

public interface IPartSite {
    String getId();
    IWorkbenchPage getPage();
    IWorkbench getWorkbench();
    IWorkbenchPart getPart();
    void setStatusMessage(String message);
    void setSystemInfo(String info);
}
