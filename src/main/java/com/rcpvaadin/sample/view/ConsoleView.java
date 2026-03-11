package com.rcpvaadin.sample.view;

import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.annotation.RcpView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import org.springframework.context.annotation.Scope;

import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpView(id = "console", name = "Console", icon = com.vaadin.flow.component.icon.VaadinIcon.TERMINAL)
public class ConsoleView implements IViewPart {

    private static final String BUILD_OUTPUT =
            "[INFO] Scanning for projects...\n" +
            "[INFO] Building rcp-vaadin 1.0-SNAPSHOT\n" +
            "[INFO] Compiling 42 source files\n" +
            "[INFO] BUILD SUCCESS\n" +
            "[INFO] Total time: 3.142 s\n";

    private IPartSite site;

    @Override public String getTitle()   { return "Console"; }
    @Override public IPartSite getSite() { return site; }
    @Override public void   dispose()    {}

    @Override
    public List<ToolbarItem> getToolbarItems() {
        return List.of(
            new ToolbarItem(VaadinIcon.SEARCH, "Search output",
                    () -> { if (site != null) site.setStatusMessage("Search..."); }),
            new ToolbarItem(VaadinIcon.CLOSE,  "Clear output",
                    () -> { if (site != null) site.setStatusMessage("Cleared"); })
        );
    }

    @Override
    public void init(IPartSite s) {
        this.site = s;
        int lines = BUILD_OUTPUT.split("\n", -1).length;
        s.setStatusMessage("Build output");
        s.setSystemInfo(lines + " lines");
    }

    @Override
    public Component createPartControl() {
        TextArea console = new TextArea();
        console.addClassName("console-output");
        console.setValue(BUILD_OUTPUT);
        console.setReadOnly(true);
        console.setSizeFull();
        return console;
    }
}
