package com.rcpvaadin.workbench;

/**
 * Represents the input to an editor. Implementations must provide correct
 * equals/hashCode based on identity (e.g. file path) so that the same resource
 * is never opened in two separate tabs.
 */
public interface IEditorInput {
    String getName();
    String getToolTip();
}
