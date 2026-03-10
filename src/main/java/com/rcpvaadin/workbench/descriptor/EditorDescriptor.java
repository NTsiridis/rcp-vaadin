package com.rcpvaadin.workbench.descriptor;

import com.rcpvaadin.workbench.IEditorPart;
import com.vaadin.flow.component.icon.VaadinIcon;

public record EditorDescriptor(String id, String name, VaadinIcon icon, Class<? extends IEditorPart> editorClass) {}
