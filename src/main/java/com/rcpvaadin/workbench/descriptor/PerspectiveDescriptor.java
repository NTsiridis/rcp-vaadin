package com.rcpvaadin.workbench.descriptor;

import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import com.vaadin.flow.component.icon.VaadinIcon;

public record PerspectiveDescriptor(String id, String name, VaadinIcon icon, String color,
                                    Class<? extends IPerspectiveFactory> factoryClass) {}
