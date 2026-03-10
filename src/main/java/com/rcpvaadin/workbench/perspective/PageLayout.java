package com.rcpvaadin.workbench.perspective;

import java.util.ArrayList;
import java.util.List;

public class PageLayout implements IPageLayout {

    public record ViewPlacement(String viewId, int relationship, float ratio, String refPartId) {}

    private final List<ViewPlacement> placements = new ArrayList<>();

    @Override
    public void addView(String viewId, int relationship, float ratio, String refPartId) {
        placements.add(new ViewPlacement(viewId, relationship, ratio, refPartId));
    }

    public List<ViewPlacement> getPlacements() {
        return List.copyOf(placements);
    }
}
