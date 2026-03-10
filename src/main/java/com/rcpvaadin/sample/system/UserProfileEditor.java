package com.rcpvaadin.sample.system;

import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.annotation.RcpEditor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import org.springframework.context.annotation.Scope;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpEditor(id = "userProfile", name = "User Profile", icon = VaadinIcon.USER)
public class UserProfileEditor implements IEditorPart {

    private IEditorInput input;
    private IPartSite    site;

    @Override public String getTitle()        { return "User Profile"; }
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

        Icon avatar = new Icon(VaadinIcon.USER_CARD);
        avatar.setSize("64px");
        avatar.getStyle().set("color", "var(--lumo-primary-color)").set("margin-bottom", "8px");

        H3 heading = new H3("User Profile");

        Span subtitle = new Span("Manage your account information");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-bottom", "24px");

        HorizontalLayout topRow = new HorizontalLayout(avatar);
        topRow.setAlignItems(HorizontalLayout.Alignment.CENTER);

        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        TextField firstName = new TextField("First Name");
        firstName.setValue("Jane");
        TextField lastName = new TextField("Last Name");
        lastName.setValue("Developer");
        EmailField email = new EmailField("Email");
        email.setValue("jane.developer@example.com");
        TextField role = new TextField("Role");
        role.setValue("Software Engineer");
        role.setReadOnly(true);
        TextField department = new TextField("Department");
        department.setValue("Engineering");
        department.setReadOnly(true);

        form.add(firstName, lastName, email, role, department);

        root.add(topRow, heading, subtitle, form);
        return root;
    }
}
