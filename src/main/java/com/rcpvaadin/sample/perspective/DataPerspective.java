package com.rcpvaadin.sample.perspective;

import com.rcpvaadin.sample.editor.EmployeesEditorInput;
import com.rcpvaadin.workbench.IWorkbenchPage;
import com.rcpvaadin.workbench.annotation.RcpPerspective;
import com.rcpvaadin.workbench.perspective.IPageLayout;
import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.stereotype.Component;

@RcpPerspective(id = "dataPerspective", name = "Data", icon = VaadinIcon.DATABASE)
@Component
public class DataPerspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        // Editor area only — the SearchPanel on EmployeesEditor is the filter UI
    }

    @Override
    public void createInitialEditors(IWorkbenchPage page) {
        page.openEditor(EmployeesEditorInput.get(), "employeesEditor");
    }
}
