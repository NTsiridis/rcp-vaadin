package com.rcpvaadin.sample.system;

import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.annotation.RcpEditor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import org.springframework.context.annotation.Scope;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpEditor(id = "systemPreferences", name = "Preferences", icon = VaadinIcon.COG)
public class SystemPreferencesEditor implements IEditorPart {

    private IEditorInput input;
    private IPartSite    site;

    @Override public String getTitle()        { return "Preferences"; }
    @Override public void   init(IPartSite s) { this.site = s; }
    @Override public void   dispose()         {}
    @Override public IPartSite getSite()      { return site; }

    @Override public void         setInput(IEditorInput input) { this.input = input; }
    @Override public IEditorInput getEditorInput()             { return input; }
    @Override public boolean      isDirty()                    { return false; }

    @Override
    public Component createPartControl() {
        VerticalLayout root = new VerticalLayout();
        root.setSizeFull();
        root.setAlignItems(VerticalLayout.Alignment.START);
        root.getStyle().set("max-width", "600px").set("margin", "0 auto").set("padding", "24px");

        H3 heading = new H3("System Preferences");
        Span subtitle = new Span("Customize your workbench experience");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-bottom", "24px");

        // Appearance section
        H4 appearanceTitle = new H4("Appearance");
        appearanceTitle.getStyle().set("margin-bottom", "8px");

        Select<String> theme = new Select<>();
        theme.setLabel("Theme");
        theme.setItems("System Default", "Light", "Dark");
        theme.setValue("System Default");
        theme.setWidth("240px");

        Select<String> fontSize = new Select<>();
        fontSize.setLabel("Editor Font Size");
        fontSize.setItems("12px", "13px", "14px", "16px", "18px");
        fontSize.setValue("14px");
        fontSize.setWidth("240px");

        // Behaviour section
        H4 behaviourTitle = new H4("Behaviour");
        behaviourTitle.getStyle().set("margin-top", "16px").set("margin-bottom", "8px");

        Checkbox autoSave = new Checkbox("Auto-save editors on focus loss");
        autoSave.setValue(true);
        Checkbox restoreLayout = new Checkbox("Restore perspective layout on startup");
        restoreLayout.setValue(true);
        Checkbox showLineNumbers = new Checkbox("Show line numbers in code editor");
        showLineNumbers.setValue(false);

        // Notifications section
        H4 notificationsTitle = new H4("Notifications");
        notificationsTitle.getStyle().set("margin-top", "16px").set("margin-bottom", "8px");

        Checkbox buildNotifications = new Checkbox("Show build notifications");
        buildNotifications.setValue(true);
        Checkbox errorAlerts = new Checkbox("Show error alerts");
        errorAlerts.setValue(true);

        root.add(heading, subtitle,
                appearanceTitle, theme, fontSize,
                behaviourTitle, autoSave, restoreLayout, showLineNumbers,
                notificationsTitle, buildNotifications, errorAlerts);
        return root;
    }
}
