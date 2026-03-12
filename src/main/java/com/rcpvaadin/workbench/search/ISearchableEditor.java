package com.rcpvaadin.workbench.search;

import java.util.List;

public interface ISearchableEditor {
    List<SearchFieldDescriptor> getSearchFields();
    void executeSearch(SearchCriteria criteria);
    void clearSearch();
}
