package com.rcpvaadin.ui;

/**
 * Implemented by UI components that can be collapsed into a minimal strip or
 * maximized to fill the workbench area.
 * PerspectiveLayout wires the callbacks after building the SplitLayout tree.
 */
public interface Collapsible {
    void setCollapseSupport(Runnable onCollapse, Runnable onExpand);
    void setMaximizeSupport(Runnable onMaximize, Runnable onUnmaximize);
    void setMaximizedState(boolean maximized);
}
