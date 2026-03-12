package com.rcpvaadin.workbench.search;

import java.util.List;

public record SearchFieldDescriptor(
        String fieldId,
        String label,
        SearchFieldType type,
        List<String> selectOptions) {

    public static SearchFieldDescriptor text(String id, String label) {
        return new SearchFieldDescriptor(id, label, SearchFieldType.TEXT, List.of());
    }

    public static SearchFieldDescriptor select(String id, String label, List<String> opts) {
        return new SearchFieldDescriptor(id, label, SearchFieldType.SELECT, List.copyOf(opts));
    }

    public static SearchFieldDescriptor dateRange(String id, String label) {
        return new SearchFieldDescriptor(id, label, SearchFieldType.DATE_RANGE, List.of());
    }

    public static SearchFieldDescriptor numberRange(String id, String label) {
        return new SearchFieldDescriptor(id, label, SearchFieldType.NUMBER_RANGE, List.of());
    }

    public static SearchFieldDescriptor bool(String id, String label) {
        return new SearchFieldDescriptor(id, label, SearchFieldType.BOOLEAN, List.of());
    }
}
