package com.rcpvaadin.workbench.annotation;

import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface RcpPerspective {
    String id();
    String name();
    VaadinIcon icon() default VaadinIcon.VAADIN_H;
}
