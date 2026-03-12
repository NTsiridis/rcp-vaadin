package com.rcpvaadin.workbench.search;

import java.time.LocalDate;
import java.util.Map;

public record SearchCriteria(Map<String, Object> values) {

    public String text(String fieldId) {
        Object v = values.get(fieldId);
        return v instanceof String s ? s : "";
    }

    public String selected(String fieldId) {
        Object v = values.get(fieldId);
        return v instanceof String s ? s : "";
    }

    public LocalDate dateFrom(String fieldId) {
        Object v = values.get(fieldId + "_from");
        return v instanceof LocalDate d ? d : null;
    }

    public LocalDate dateTo(String fieldId) {
        Object v = values.get(fieldId + "_to");
        return v instanceof LocalDate d ? d : null;
    }

    public Double numberFrom(String fieldId) {
        Object v = values.get(fieldId + "_min");
        return v instanceof Double d ? d : null;
    }

    public Double numberTo(String fieldId) {
        Object v = values.get(fieldId + "_max");
        return v instanceof Double d ? d : null;
    }

    public boolean bool(String fieldId) {
        Object v = values.get(fieldId);
        return Boolean.TRUE.equals(v);
    }
}
