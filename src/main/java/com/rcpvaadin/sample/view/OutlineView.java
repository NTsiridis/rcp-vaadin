package com.rcpvaadin.sample.view;

import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.annotation.RcpView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.TreeGrid;
import org.springframework.context.annotation.Scope;

import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpView(id = "outline", name = "Outline", icon = com.vaadin.flow.component.icon.VaadinIcon.LIST_OL)
public class OutlineView implements IViewPart {

    public record OutlineNode(String name, String type) {}

    private IPartSite site;

    @Override public String getTitle()        { return "Outline"; }
    @Override public void   init(IPartSite s) { this.site = s; }
    @Override public void   dispose()         {}
    @Override public IPartSite getSite()      { return site; }

    @Override
    public List<ToolbarItem> getToolbarItems() {
        return List.of(
            new ToolbarItem(VaadinIcon.SEARCH, "Search elements",
                    () -> { if (site != null) site.setStatusMessage("Search..."); }),
            new ToolbarItem(VaadinIcon.EXPAND, "Expand All",
                    () -> { if (site != null) site.setStatusMessage("Expanded"); })
        );
    }

    @Override
    public Component createPartControl() {
        TreeGrid<OutlineNode> grid = new TreeGrid<>();
        grid.addHierarchyColumn(OutlineNode::name).setHeader("Element");
        grid.addColumn(OutlineNode::type).setHeader("Type");
        grid.setSizeFull();

        OutlineNode cls     = new OutlineNode("Main",           "class");
        OutlineNode field   = new OutlineNode("LOG",            "field");
        OutlineNode method1 = new OutlineNode("main(String[])", "method");
        OutlineNode method2 = new OutlineNode("run()",          "method");

        grid.setItems(List.of(cls), node -> {
            if (node == cls) return List.of(field, method1, method2);
            return List.of();
        });

        return grid;
    }
}
