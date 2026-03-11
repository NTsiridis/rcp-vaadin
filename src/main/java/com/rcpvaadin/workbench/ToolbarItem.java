package com.rcpvaadin.workbench;

import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Declares a single action button in a part's title-bar toolbar.
 *
 * <p>Parts return a list of these from {@link IWorkbenchPart#getToolbarItems()}.
 * The workbench renders each item as a small icon button and wires the click
 * to {@code action}. The default implementation returns an empty list, so
 * parts that need no toolbar require no changes.
 *
 * <p>Example usage in a view:
 * <pre>{@code
 * @Override
 * public List<ToolbarItem> getToolbarItems() {
 *     return List.of(
 *         new ToolbarItem(VaadinIcon.SEARCH, "Search",  () -> site.setStatusMessage("Searching...")),
 *         new ToolbarItem(VaadinIcon.REFRESH,"Refresh", this::reload)
 *     );
 * }
 * }</pre>
 *
 * @param icon    Vaadin icon shown on the button
 * @param tooltip Tooltip text shown on hover
 * @param action  Callback invoked when the button is clicked
 */
public record ToolbarItem(VaadinIcon icon, String tooltip, Runnable action) {}
