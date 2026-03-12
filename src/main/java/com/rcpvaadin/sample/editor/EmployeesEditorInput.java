package com.rcpvaadin.sample.editor;

import com.rcpvaadin.workbench.IEditorInput;

public class EmployeesEditorInput implements IEditorInput {

    private static final EmployeesEditorInput INSTANCE = new EmployeesEditorInput();

    public static EmployeesEditorInput get() { return INSTANCE; }

    @Override public String getName()    { return "Employees"; }
    @Override public String getToolTip() { return "Employee directory"; }

    @Override
    public boolean equals(Object obj) { return obj instanceof EmployeesEditorInput; }

    @Override
    public int hashCode() { return EmployeesEditorInput.class.hashCode(); }
}
