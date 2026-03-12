package com.rcpvaadin.ui;

import com.rcpvaadin.workbench.search.SearchCriteria;
import com.rcpvaadin.workbench.search.SearchFieldDescriptor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SearchPanel extends VerticalLayout {

    /** Maps component key → Vaadin component. For date/number fields the key
     *  uses the suffixes expected by {@link SearchCriteria} (_from/_to/_min/_max). */
    private final Map<String, Object> fieldComponents = new HashMap<>();
    private final Consumer<SearchCriteria> onSearch;
    private final Runnable onClear;

    public SearchPanel(List<SearchFieldDescriptor> fields,
                       Consumer<SearchCriteria> onSearch,
                       Runnable onClear) {
        this.onSearch = onSearch;
        this.onClear  = onClear;

        addClassName("search-panel");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        HorizontalLayout criteriaRow = new HorizontalLayout();
        criteriaRow.addClassName("search-criteria-row");
        criteriaRow.setPadding(false);
        criteriaRow.setSpacing(false);
        criteriaRow.setAlignItems(FlexComponent.Alignment.END);
        criteriaRow.setWidthFull();

        for (SearchFieldDescriptor field : fields) {
            buildFieldComponent(field, criteriaRow);
        }

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.addClassName("search-action-row");
        actionRow.setPadding(false);
        actionRow.setSpacing(false);
        actionRow.setWidthFull();

        Button searchBtn = new Button("Search", new Icon(VaadinIcon.SEARCH));
        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        searchBtn.addClickListener(e -> fireSearch());

        Button resetBtn = new Button("Reset", new Icon(VaadinIcon.CLOSE_SMALL));
        resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        resetBtn.addClickListener(e -> fireReset());

        actionRow.add(searchBtn, resetBtn);

        add(criteriaRow, actionRow);
    }

    private void buildFieldComponent(SearchFieldDescriptor field, HorizontalLayout row) {
        switch (field.type()) {
            case TEXT -> {
                TextField tf = new TextField(field.label());
                tf.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                fieldComponents.put(field.fieldId(), tf);
                row.add(tf);
            }
            case SELECT -> {
                ComboBox<String> cb = new ComboBox<>(field.label());
                cb.setItems(field.selectOptions());
                cb.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
                fieldComponents.put(field.fieldId(), cb);
                row.add(cb);
            }
            case DATE_RANGE -> {
                DatePicker from = new DatePicker(field.label() + " From");
                DatePicker to   = new DatePicker(field.label() + " To");
                from.addThemeVariants(DatePickerVariant.LUMO_SMALL);
                to.addThemeVariants(DatePickerVariant.LUMO_SMALL);
                fieldComponents.put(field.fieldId() + "_from", from);
                fieldComponents.put(field.fieldId() + "_to",   to);
                row.add(from, to);
            }
            case NUMBER_RANGE -> {
                NumberField min = new NumberField(field.label() + " Min");
                NumberField max = new NumberField(field.label() + " Max");
                min.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                max.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                fieldComponents.put(field.fieldId() + "_min", min);
                fieldComponents.put(field.fieldId() + "_max", max);
                row.add(min, max);
            }
            case BOOLEAN -> {
                Select<String> sel = new Select<>();
                sel.setLabel(field.label());
                sel.setItems("Any", "Yes", "No");
                sel.setValue("Any");
                fieldComponents.put(field.fieldId(), sel);
                row.add(sel);
            }
        }
    }

    private void fireSearch() {
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> entry : fieldComponents.entrySet()) {
            String key  = entry.getKey();
            Object comp = entry.getValue();
            if (comp instanceof TextField tf) {
                String v = tf.getValue();
                if (!v.isBlank()) values.put(key, v);
            } else if (comp instanceof ComboBox<?> cb) {
                Object v = cb.getValue();
                if (v != null) values.put(key, v.toString());
            } else if (comp instanceof DatePicker dp) {
                LocalDate v = dp.getValue();
                if (v != null) values.put(key, v);
            } else if (comp instanceof NumberField nf) {
                Double v = nf.getValue();
                if (v != null) values.put(key, v);
            } else if (comp instanceof Select<?> sel) {
                Object v = sel.getValue();
                if (v != null && !"Any".equals(v.toString())) {
                    values.put(key, "Yes".equals(v.toString()));
                }
            }
        }
        onSearch.accept(new SearchCriteria(values));
    }

    @SuppressWarnings("unchecked")
    private void fireReset() {
        for (Object comp : fieldComponents.values()) {
            if (comp instanceof TextField tf)        tf.clear();
            else if (comp instanceof ComboBox<?> cb) cb.clear();
            else if (comp instanceof DatePicker dp)  dp.clear();
            else if (comp instanceof NumberField nf) nf.clear();
            else if (comp instanceof Select<?> sel)  ((Select<String>) sel).setValue("Any");
        }
        onClear.run();
    }
}
