package com.rcpvaadin.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

import java.util.function.Consumer;

public class QuickFilterBar extends HorizontalLayout {

    private final TextField filterField = new TextField();

    public QuickFilterBar(Consumer<String> onFilter, Runnable onClose) {
        addClassName("quick-filter-bar");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);

        Icon searchIcon = new Icon(VaadinIcon.SEARCH);
        searchIcon.setSize("16px");
        searchIcon.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("flex-shrink", "0")
                .set("margin", "0 4px");

        filterField.setPlaceholder("Filter...");
        filterField.setClearButtonVisible(true);
        filterField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        filterField.getStyle().set("flex", "1");
        filterField.addValueChangeListener(e -> onFilter.accept(e.getValue()));
        filterField.addKeyDownListener(Key.ESCAPE, e -> {
            filterField.clear();
            onClose.run();
        });

        Button closeBtn = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        closeBtn.setTooltipText("Close filter");
        closeBtn.addClickListener(e -> {
            filterField.clear();
            onClose.run();
        });

        add(searchIcon, filterField, closeBtn);
    }

    public void focus() {
        filterField.focus();
    }

    public void reset() {
        filterField.clear();
    }
}
