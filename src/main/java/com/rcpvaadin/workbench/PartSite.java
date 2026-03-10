package com.rcpvaadin.workbench;

import java.util.function.Consumer;

public class PartSite implements IPartSite {

    private final String id;
    private final IWorkbenchPart part;
    private final IWorkbenchPage page;
    private final IWorkbench workbench;

    private Consumer<String> messageConsumer = msg  -> {};
    private Consumer<String> systemConsumer  = info -> {};

    // Last-set values — replayed when a new status bar is bound
    private String lastMessage    = null;
    private String lastSystemInfo = null;

    public PartSite(String id, IWorkbenchPart part, IWorkbenchPage page, IWorkbench workbench) {
        this.id = id;
        this.part = part;
        this.page = page;
        this.workbench = workbench;
    }

    @Override public String getId()             { return id; }
    @Override public IWorkbenchPage getPage()   { return page; }
    @Override public IWorkbench getWorkbench()  { return workbench; }
    @Override public IWorkbenchPart getPart()   { return part; }

    public void bindStatusBar(Consumer<String> msgConsumer, Consumer<String> sysConsumer) {
        this.messageConsumer = msgConsumer != null ? msgConsumer : msg  -> {};
        this.systemConsumer  = sysConsumer  != null ? sysConsumer  : info -> {};
        // Replay last-set values so parts that called setStatusMessage in init() are shown
        if (lastMessage    != null) this.messageConsumer.accept(lastMessage);
        if (lastSystemInfo != null) this.systemConsumer.accept(lastSystemInfo);
    }

    @Override
    public void setStatusMessage(String m) {
        lastMessage = m;
        messageConsumer.accept(m != null ? m : "");
    }

    @Override
    public void setSystemInfo(String i) {
        lastSystemInfo = i;
        systemConsumer.accept(i != null ? i : "");
    }
}
