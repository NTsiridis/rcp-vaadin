package com.rcpvaadin.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight snapshot of one perspective's layout state — splitter positions,
 * minimized panel IDs, saved root node tree, and stack selected indices.
 * Lives in WorkbenchView for the browser-tab session lifetime.
 */
public class PerspectiveState {

    private final Map<String, Double> splitterPositions = new HashMap<>();
    private final Set<String>         minimizedIds      = new HashSet<>();

    private PerspectiveLayout.LayoutNode savedRootNode        = null;
    private final Map<String, Integer>   stackSelectedIndices = new HashMap<>();

    public void setSplitterPosition(String key, double pos) {
        splitterPositions.put(key, pos);
    }

    public double getSplitterPosition(String key, double factoryDefault) {
        return splitterPositions.getOrDefault(key, factoryDefault);
    }

    public void setMinimizedIds(Set<String> ids) {
        minimizedIds.clear();
        minimizedIds.addAll(ids);
    }

    public Set<String> getMinimizedIds() {
        return Set.copyOf(minimizedIds);
    }

    public void setSavedRootNode(PerspectiveLayout.LayoutNode node) { this.savedRootNode = node; }
    public PerspectiveLayout.LayoutNode getSavedRootNode()          { return savedRootNode; }

    public void setStackSelectedIndex(String stackId, int index)    { stackSelectedIndices.put(stackId, index); }
    public int  getStackSelectedIndex(String stackId, int fallback) { return stackSelectedIndices.getOrDefault(stackId, fallback); }
}
