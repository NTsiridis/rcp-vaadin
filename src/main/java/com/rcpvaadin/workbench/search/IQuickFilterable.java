package com.rcpvaadin.workbench.search;

public interface IQuickFilterable {
    /** Called with each keystroke; impl filters grid rows in-place. */
    void applyQuickFilter(String text);
}
