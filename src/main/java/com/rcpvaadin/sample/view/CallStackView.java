package com.rcpvaadin.sample.view;

import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.annotation.RcpView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.context.annotation.Scope;

import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpView(id = "callStack", name = "Call Stack", icon = com.vaadin.flow.component.icon.VaadinIcon.LEVEL_DOWN)
public class CallStackView implements IViewPart {

    public record StackFrame(String method, String location) {}

    private IPartSite site;

    @Override public String getTitle()        { return "Call Stack"; }
    @Override public void   init(IPartSite s) { this.site = s; }
    @Override public void   dispose()         {}
    @Override public IPartSite getSite()      { return site; }

    @Override
    public List<ToolbarItem> getToolbarItems() {
        return List.of(
            new ToolbarItem(VaadinIcon.REFRESH, "Refresh stack",
                    () -> { if (site != null) site.setStatusMessage("Refreshed"); })
        );
    }

    @Override
    public Component createPartControl() {
        Grid<StackFrame> grid = new Grid<>(StackFrame.class, false);
        grid.addColumn(StackFrame::method).setHeader("Method");
        grid.addColumn(StackFrame::location).setHeader("Location");
        grid.setSizeFull();
        grid.setItems(List.of(
                new StackFrame("main(String[])", "Main.java:10"),
                new StackFrame("run()",          "Main.java:25"),
                new StackFrame("process(List)",  "Utils.java:42")
        ));
        return grid;
    }
}
