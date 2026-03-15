package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.PartSite;
import com.rcpvaadin.workbench.search.ISearchableEditor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class EditorContainer extends VerticalLayout {

    public EditorContainer(IEditorPart editor, VaadinIcon icon) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // ── Toolbar bar (editor-specific items only) ──
        HorizontalLayout toolbarBar = ViewContainer.buildToolbarBar(editor.getToolbarItems());
        add(toolbarBar);

        // ── Search Panel ──
        if (editor instanceof ISearchableEditor se) {
            add(new SearchPanel(se.getSearchFields(), se::executeSearch, se::clearSearch));
        }

        // ── Content ──
        com.vaadin.flow.component.Component content = editor.createPartControl();
        add(content);
        setFlexGrow(1, content);

        // ── Status Bar ──
        PartStatusBar statusBar = new PartStatusBar();
        add(statusBar);
        if (editor.getSite() instanceof PartSite ps) {
            ps.bindStatusBar(statusBar::setMessage, statusBar::setSystemInfo);
        }
    }
}
