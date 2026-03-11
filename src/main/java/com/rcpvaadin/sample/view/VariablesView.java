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
@RcpView(id = "variables", name = "Variables", icon = com.vaadin.flow.component.icon.VaadinIcon.INFO_CIRCLE)
public class VariablesView implements IViewPart {

    public record DebugVar(String name, String type, String value) {}

    private IPartSite site;

    @Override public String getTitle()        { return "Variables"; }
    @Override public void   init(IPartSite s) { this.site = s; }
    @Override public void   dispose()         {}
    @Override public IPartSite getSite()      { return site; }

    @Override
    public List<ToolbarItem> getToolbarItems() {
        return List.of(
            new ToolbarItem(VaadinIcon.REFRESH, "Refresh variables",
                    () -> { if (site != null) site.setStatusMessage("Refreshed"); }),
            new ToolbarItem(VaadinIcon.SEARCH,  "Search variables",
                    () -> { if (site != null) site.setStatusMessage("Search..."); })
        );
    }

    @Override
    public Component createPartControl() {
        Grid<DebugVar> grid = new Grid<>(DebugVar.class, false);
        grid.addColumn(DebugVar::name).setHeader("Name");
        grid.addColumn(DebugVar::type).setHeader("Type");
        grid.addColumn(DebugVar::value).setHeader("Value");
        grid.setSizeFull();
        grid.setItems(List.of(
                new DebugVar("args",    "String[]", "[]"),
                new DebugVar("count",   "int",      "42"),
                new DebugVar("message", "String",   "\"Hello World\"")
        ));
        return grid;
    }
}
