package com.rcpvaadin.sample.view;

import com.rcpvaadin.sample.editor.TextEditorInput;
import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.annotation.RcpView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.treegrid.TreeGrid;
import org.springframework.context.annotation.Scope;

import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpView(id = "projectExplorer", name = "Project Explorer", icon = com.vaadin.flow.component.icon.VaadinIcon.FOLDER_OPEN)
public class ProjectExplorerView implements IViewPart {

    public record FileNode(String name, String path, boolean directory) {}

    private static final List<FileNode> ALL_FILES = List.of(
            new FileNode("my-project",  "/my-project",               true),
            new FileNode("src",          "/my-project/src",           true),
            new FileNode("Main.java",    "/my-project/src/Main.java", false),
            new FileNode("Utils.java",   "/my-project/src/Utils.java", false)
    );

    private IPartSite site;

    @Override public String getTitle()   { return "Project Explorer"; }
    @Override public IPartSite getSite() { return site; }
    @Override public void   dispose()    {}

    @Override
    public void init(IPartSite s) {
        this.site = s;
        s.setSystemInfo(ALL_FILES.size() + " items");
    }

    @Override
    public Component createPartControl() {
        TreeGrid<FileNode> grid = new TreeGrid<>();
        grid.addHierarchyColumn(FileNode::name).setHeader("Name");
        grid.setSizeFull();

        FileNode root = ALL_FILES.get(0);
        FileNode src  = ALL_FILES.get(1);
        FileNode main = ALL_FILES.get(2);
        FileNode util = ALL_FILES.get(3);

        grid.setItems(List.of(root), node -> {
            if (node == root) return List.of(src);
            if (node == src)  return List.of(main, util);
            return List.of();
        });

        grid.addItemClickListener(e -> {
            FileNode node = e.getItem();
            if (site != null) site.setStatusMessage(node.path());
        });

        grid.addItemDoubleClickListener(e -> {
            FileNode node = e.getItem();
            if (!node.directory() && site != null) {
                site.getPage().openEditor(new TextEditorInput(node.path(), node.name()), "textEditor");
            }
        });

        return grid;
    }
}
