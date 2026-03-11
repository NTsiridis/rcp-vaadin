package com.rcpvaadin.sample.editor;

import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.annotation.RcpEditor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import org.springframework.context.annotation.Scope;

import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpEditor(id = "textEditor", name = "Text Editor", icon = com.vaadin.flow.component.icon.VaadinIcon.EDIT)
public class TextEditorPart implements IEditorPart {

    private IEditorInput input;
    private IPartSite    site;

    @Override public String getTitle()        { return input != null ? input.getName() : "Text Editor"; }
    @Override public void   init(IPartSite s) { this.site = s; }
    @Override public void   dispose()         {}
    @Override public IPartSite getSite()      { return site; }

    @Override public void         setInput(IEditorInput input) { this.input = input; }
    @Override public IEditorInput getEditorInput()             { return input; }
    @Override public boolean      isDirty()                    { return false; }

    @Override
    public List<ToolbarItem> getToolbarItems() {
        return List.of(
            new ToolbarItem(VaadinIcon.SEARCH,   "Search",
                    () -> { if (site != null) site.setStatusMessage("Search..."); }),
            new ToolbarItem(VaadinIcon.DOWNLOAD, "Save",
                    () -> { if (site != null) site.setStatusMessage("Saved"); }),
            new ToolbarItem(VaadinIcon.PENCIL,   "Edit",
                    () -> { if (site != null) site.setStatusMessage("Edit mode"); }),
            new ToolbarItem(VaadinIcon.TRASH,    "Delete",
                    () -> { if (site != null) site.setStatusMessage("Delete?"); })
        );
    }

    @Override
    public com.vaadin.flow.component.Component createPartControl() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);

        if (input != null) {
            Span pathLabel = new Span(input.getToolTip());
            pathLabel.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "4px 8px");

            String initialContent =
                    "// " + input.getName() + "\n" +
                    "public class Main {\n\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Hello World\");\n" +
                    "    }\n" +
                    "}\n";

            TextArea textArea = new TextArea();
            textArea.addClassName("code-editor");
            textArea.setValue(initialContent);
            textArea.setSizeFull();

            textArea.addValueChangeListener(e -> {
                if (site != null) {
                    String text = e.getValue();
                    int lines = text.isEmpty() ? 0 : text.split("\n", -1).length;
                    site.setStatusMessage("Modified");
                    site.setSystemInfo("Lines: " + lines);
                }
            });

            layout.add(pathLabel, textArea);
            layout.setFlexGrow(1, textArea);
        }

        return layout;
    }
}
