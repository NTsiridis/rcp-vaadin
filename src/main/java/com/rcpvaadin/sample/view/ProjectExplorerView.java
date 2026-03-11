package com.rcpvaadin.sample.view;

import com.rcpvaadin.sample.editor.TextEditorInput;
import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.annotation.RcpView;
import com.rcpvaadin.workbench.search.IQuickFilterable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.TreeGrid;
import org.springframework.context.annotation.Scope;

import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpView(id = "projectExplorer", name = "Project Explorer", icon = com.vaadin.flow.component.icon.VaadinIcon.FOLDER_OPEN)
public class ProjectExplorerView implements IViewPart, IQuickFilterable {

    public record FileNode(String name, String path, boolean directory) {}

    private static final List<FileNode> ALL_FILES = List.of(
            new FileNode("my-project",  "/my-project",               true),
            new FileNode("src",          "/my-project/src",           true),
            new FileNode("Main.java",    "/my-project/src/Main.java", false),
            new FileNode("Utils.java",   "/my-project/src/Utils.java", false)
    );

    private static final FileNode ROOT = ALL_FILES.get(0);
    private static final FileNode SRC  = ALL_FILES.get(1);
    private static final FileNode MAIN = ALL_FILES.get(2);
    private static final FileNode UTIL = ALL_FILES.get(3);

    private IPartSite site;
    private TreeGrid<FileNode> grid;

    @Override public String getTitle()   { return "Project Explorer"; }
    @Override public IPartSite getSite() { return site; }
    @Override public void   dispose()    {}

    @Override
    public List<ToolbarItem> getToolbarItems() {
        return List.of(
            new ToolbarItem(VaadinIcon.SEARCH, "Search files",
                    () -> { if (site != null) site.setStatusMessage("Search..."); }),
            new ToolbarItem(VaadinIcon.MINUS,  "Collapse All",
                    () -> { if (site != null) site.setStatusMessage("Collapsed"); })
        );
    }

    @Override
    public void init(IPartSite s) {
        this.site = s;
        s.setSystemInfo(ALL_FILES.size() + " items");
    }

    @Override
    public Component createPartControl() {
        grid = new TreeGrid<>();
        grid.addHierarchyColumn(FileNode::name).setHeader("Name");
        grid.setSizeFull();

        loadFullTree();

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

    @Override
    public void applyQuickFilter(String text) {
        if (grid == null) return;
        if (text.isBlank()) {
            loadFullTree();
        } else {
            // Show only matching nodes as a flat list
            List<FileNode> matches = ALL_FILES.stream()
                    .filter(n -> n.name().toLowerCase().contains(text.toLowerCase()))
                    .toList();
            grid.setItems(matches, node -> List.of());
            grid.expandRecursively(matches, 0);
        }
    }

    private void loadFullTree() {
        grid.setItems(List.of(ROOT), node -> {
            if (node == ROOT) return List.of(SRC);
            if (node == SRC)  return List.of(MAIN, UTIL);
            return List.of();
        });
        grid.expandRecursively(List.of(ROOT), 2);
    }
}
