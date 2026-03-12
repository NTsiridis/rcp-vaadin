package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.PartSite;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.search.ISearchableEditor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

public class EditorContainer extends VerticalLayout {

    private boolean maximized = false;
    private final Button maximizeBtn;

    public EditorContainer(IEditorPart editor, VaadinIcon icon,
                           Runnable onCollapse, Runnable onMaximize, Runnable onUnmaximize) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // ── Title bar ──
        Icon collapseIcon = new Icon(VaadinIcon.ANGLE_DOWN);
        collapseIcon.setSize("18px");
        Button collapseBtn = new Button(collapseIcon);
        collapseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        collapseBtn.getStyle().set("min-width", "26px").set("width", "26px").set("height", "26px");
        collapseBtn.setTooltipText("Minimize editors");

        Icon maxIcon = new Icon(VaadinIcon.EXPAND_SQUARE);
        maxIcon.setSize("18px");
        maximizeBtn = new Button(maxIcon);
        maximizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        maximizeBtn.getStyle().set("min-width", "26px").set("width", "26px").set("height", "26px");
        maximizeBtn.setTooltipText("Maximize editors");

        Icon iconComp = new Icon(icon);
        iconComp.getStyle().set("width", "14px").set("height", "14px");

        Span titleSpan = new Span(editor.getTitle());
        titleSpan.addClassName("view-title");

        HorizontalLayout titleBar = new HorizontalLayout(iconComp, titleSpan, maximizeBtn, collapseBtn);
        titleBar.addClassName("view-title-bar");
        titleBar.setWidthFull();
        titleBar.setPadding(false);
        titleBar.setAlignItems(FlexComponent.Alignment.CENTER);
        titleBar.setFlexGrow(1, titleSpan);

        collapseBtn.addClickListener(e -> onCollapse.run());
        maximizeBtn.addClickListener(e -> {
            if (!maximized) onMaximize.run();
            else            onUnmaximize.run();
        });

        add(titleBar);

        // ── Tools Toolbar ──
        List<ToolbarItem> toolbarItems = editor.getToolbarItems();
        if (!toolbarItems.isEmpty()) {
            add(ViewContainer.buildToolbarBar(toolbarItems));
        }

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

    public void setMaximizedState(boolean m) {
        this.maximized = m;
        Icon icon = new Icon(m ? VaadinIcon.COMPRESS : VaadinIcon.EXPAND_SQUARE);
        icon.setSize("18px");
        maximizeBtn.setIcon(icon);
        maximizeBtn.getStyle().set("color", m ? "var(--lumo-primary-color)" : "");
        maximizeBtn.setTooltipText(m ? "Restore editors" : "Maximize editors");
    }
}
