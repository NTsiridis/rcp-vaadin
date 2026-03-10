package com.rcpvaadin.workbench.descriptor;

import com.rcpvaadin.workbench.IViewPart;
import com.vaadin.flow.component.icon.VaadinIcon;

public record ViewDescriptor(String id, String name, VaadinIcon icon, Class<? extends IViewPart> viewClass) {}
