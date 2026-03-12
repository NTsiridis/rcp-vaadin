# rcp-vaadin

A Vaadin Flow + Spring Boot workbench shell that mirrors the Eclipse RCP programming model. Build IDE-style web applications with perspectives, views, editors, drag-and-drop layout, search, and session-persistent state — all without writing any layout plumbing by hand.

---

## Features

- **Perspectives** — named workbench configurations, each with its own panel layout and optional nested navigation items
- **Views** — read-only panels (trees, grids, consoles) docked into the layout; support quick-filter search
- **Editors** — document/resource panels opened in a central tab sheet; support multi-criteria search; deduplicated by input identity
- **Split layouts** — arbitrary nested horizontal/vertical splits built from a declarative layout descriptor
- **Drag-and-drop view stacking** — drag any view onto another to create a tabbed stack; drag it out to unstack
- **Maximize / minimize / collapse** — every panel supports all three; minimized panels queue in a side bar and restore on click
- **Session state persistence** — splitter positions and minimized panels are remembered per perspective per browser session
- **Annotation-driven registry** — `@RcpView`, `@RcpEditor`, `@RcpPerspective` auto-register parts; no manual wiring needed
- **Two search patterns** — quick-filter bar for views (keystroke-driven), multi-criteria search panel for editors (form-driven)

---

## Technology Stack

| Layer | Technology |
|---|---|
| Server | Spring Boot 3, Java 24 |
| UI | Vaadin Flow 24 |
| Build | Maven |
| Styling | Vaadin Lumo theme + custom CSS |

---

## Project Structure

```
src/main/java/com/rcpvaadin/
├── Application.java                  # Spring Boot entry point
├── AppShell.java                     # Vaadin AppShell (applies rcp-theme)
├── workbench/                        # Core API (interfaces, annotations, search)
│   ├── IWorkbenchPart.java
│   ├── IViewPart.java
│   ├── IEditorPart.java
│   ├── IEditorInput.java
│   ├── IPartSite.java
│   ├── IWorkbench.java
│   ├── IWorkbenchPage.java
│   ├── ToolbarItem.java
│   ├── annotation/                   # @RcpView, @RcpEditor, @RcpPerspective
│   ├── descriptor/                   # ViewDescriptor, EditorDescriptor, PerspectiveDescriptor, PerspectiveNavItem
│   ├── perspective/                  # IPerspectiveFactory, IPageLayout, IPerspectiveNavigator, PageLayout
│   ├── registry/                     # WorkbenchRegistry (annotation scanner)
│   └── search/                       # IQuickFilterable, ISearchableEditor, SearchFieldDescriptor, SearchCriteria
├── ui/                               # Vaadin UI components
│   ├── WorkbenchView.java            # Top-level @Route("") shell
│   ├── PerspectiveBar.java           # Left icon strip
│   ├── PerspectiveLayout.java        # Split tree builder + DnD
│   ├── EditorArea.java               # Central TabSheet for editors
│   ├── EditorContainer.java          # Single editor wrapper
│   ├── ViewContainer.java            # Single view wrapper
│   ├── StackedViewContainer.java     # Tabbed stack of views (from DnD)
│   ├── SearchPanel.java              # Multi-criteria search form
│   ├── QuickFilterBar.java           # Inline keystroke filter bar
│   ├── MinimizedBar.java             # Right sidebar for minimized panels
│   ├── StatusBar.java                # Global status bar
│   └── PartStatusBar.java            # Per-part status bar
└── sample/                           # Reference implementations
    ├── view/                         # VariablesView, ProjectExplorerView, OutlineView, ConsoleView, CallStackView
    ├── editor/                       # EmployeesEditor, TextEditorPart (+ inputs)
    ├── system/                       # SystemPreferencesEditor, UserProfileEditor
    └── perspective/                  # JavaPerspective, DataPerspective, DebugPerspective
```

---

## Core API

### Interfaces

| Interface | Purpose |
|---|---|
| `IWorkbenchPart` | Base for all parts: `getTitle()`, `init(IPartSite)`, `dispose()`, `getToolbarItems()` |
| `IViewPart` | Extends `IWorkbenchPart`; add `createPartControl(HasComponents)` to build the panel UI |
| `IEditorPart` | Extends `IWorkbenchPart`; add `setInput(IEditorInput)`, `isDirty()`, `createPartControl()` |
| `IEditorInput` | Identifies the resource backing an editor; **must implement `equals`/`hashCode`** for tab deduplication |
| `IPartSite` | Runtime context injected into each part: access to `getPage()`, `setStatusMessage()`, `setSystemInfo()` |
| `IWorkbenchPage` | Page model: `openEditor()`, `closeEditor()`, `showView()`, `hideView()`, `setPerspective()` |
| `IPerspectiveFactory` | Defines layout via `createInitialLayout(IPageLayout)` and optional `createInitialEditors(IWorkbenchPage)` |

### Annotations

| Annotation | Fields | Usage |
|---|---|---|
| `@RcpView` | `id`, `name`, `icon` | Mark a Spring `@Component` that implements `IViewPart` |
| `@RcpEditor` | `id`, `name`, `icon` | Mark a Spring `@Component` that implements `IEditorPart` |
| `@RcpPerspective` | `id`, `name`, `icon` | Mark a Spring `@Component` that implements `IPerspectiveFactory` |

All annotated classes are discovered by `WorkbenchRegistry` at startup — no registration code required.

### ToolbarItem

```java
// Declare in your part:
@Override
public List<ToolbarItem> getToolbarItems() {
    return List.of(
        new ToolbarItem(VaadinIcon.REFRESH, "Reload", this::reload)
    );
}
```

---

## Creating a View

```java
@RcpView(id = "myView", name = "My View", icon = VaadinIcon.LIST)
@Component
@Scope("prototype")
public class MyView implements IViewPart {

    private IPartSite site;

    @Override
    public void init(IPartSite site) {
        this.site = site;
    }

    @Override
    public void createPartControl(HasComponents parent) {
        Grid<MyItem> grid = new Grid<>(MyItem.class);
        grid.setItems(loadItems());
        parent.add(grid);
        site.setStatusMessage(grid.getGenericDataView().getItemCount() + " items");
    }

    @Override public String getTitle() { return "My View"; }
    @Override public void dispose() {}
    @Override public List<ToolbarItem> getToolbarItems() { return List.of(); }
}
```

### Adding Quick Filter to a View

Implement `IQuickFilterable` and the framework wires the filter bar to the SEARCH toolbar button automatically:

```java
@RcpView(id = "myView", name = "My View", icon = VaadinIcon.LIST)
@Component @Scope("prototype")
public class MyView implements IViewPart, IQuickFilterable {

    private Grid<MyItem> grid;
    private List<MyItem> allItems;

    @Override
    public void createPartControl(HasComponents parent) {
        grid = new Grid<>(MyItem.class);
        allItems = loadItems();
        grid.setItems(allItems);
        parent.add(grid);
    }

    @Override
    public void applyQuickFilter(String text) {
        String q = text.toLowerCase();
        grid.setItems(allItems.stream()
            .filter(i -> i.getName().toLowerCase().contains(q))
            .toList());
    }

    // ...
}
```

---

## Creating an Editor

```java
@RcpEditor(id = "myEditor", name = "My Editor", icon = VaadinIcon.EDIT)
@Component
@Scope("prototype")
public class MyEditor implements IEditorPart {

    private IPartSite site;
    private MyEditorInput input;

    @Override public void init(IPartSite site) { this.site = site; }

    @Override
    public void setInput(IEditorInput input) {
        this.input = (MyEditorInput) input;
    }

    @Override
    public void createPartControl(HasComponents parent) {
        TextField field = new TextField("Name", input.getName(), "");
        parent.add(field);
    }

    @Override public String getTitle() { return input != null ? input.getName() : "My Editor"; }
    @Override public boolean isDirty() { return false; }
    @Override public void dispose() {}
    @Override public List<ToolbarItem> getToolbarItems() { return List.of(); }
}
```

### Editor Input

```java
public record MyEditorInput(String id, String name) implements IEditorInput {
    @Override public String getName() { return name; }
    @Override public String getToolTip() { return "Editing: " + name; }
    // equals/hashCode derived from record — ensures one tab per unique id
}
```

Open an editor programmatically:

```java
page.openEditor(new MyEditorInput("42", "Item 42"), "myEditor");
```

### Adding Multi-Criteria Search to an Editor

Implement `ISearchableEditor`:

```java
@RcpEditor(id = "myEditor", ...)
@Component @Scope("prototype")
public class MyEditor implements IEditorPart, ISearchableEditor {

    @Override
    public List<SearchFieldDescriptor> getSearchFields() {
        return List.of(
            SearchFieldDescriptor.text("name",   "Name"),
            SearchFieldDescriptor.select("dept",  "Department", List.of("Engineering", "Sales")),
            SearchFieldDescriptor.bool("active",  "Active")
        );
    }

    @Override
    public void executeSearch(SearchCriteria c) {
        String name   = c.text("name");
        String dept   = c.selected("dept");
        Boolean active = c.bool("active");
        grid.setItems(allItems.stream()
            .filter(i -> name   == null || i.getName().toLowerCase().contains(name.toLowerCase()))
            .filter(i -> dept   == null || i.getDepartment().equals(dept))
            .filter(i -> active == null || i.isActive() == active)
            .toList());
    }

    @Override
    public void clearSearch() { grid.setItems(allItems); }
}
```

---

## Creating a Perspective

```java
@RcpPerspective(id = "myPerspective", name = "My Perspective", icon = VaadinIcon.VIEWPORT)
@Component
@Scope("prototype")
public class MyPerspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        // Add myView to the left of the editor area, taking 25% of width
        layout.addView("myView", IPageLayout.LEFT, 0.25f, IPageLayout.ID_EDITOR_AREA);
        // Add consoleView below the editor area, taking 30% of height
        layout.addView("console", IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA);
    }

    // Optional: open editors automatically when perspective is activated
    @Override
    public void createInitialEditors(IWorkbenchPage page) {
        page.openEditor(MyEditorInput.get(), "myEditor");
    }
}
```

### Nested Navigation within a Perspective

Implement `IPerspectiveNavigator` to provide sub-items in the sidebar nav panel:

```java
@RcpPerspective(id = "myPerspective", ...)
@Component @Scope("prototype")
public class MyPerspective implements IPerspectiveFactory, IPerspectiveNavigator {

    @Override
    public List<PerspectiveNavItem> getNavItems() {
        return List.of(
            PerspectiveNavItem.group("tools", "Tools", VaadinIcon.TOOLS, null, 0),
            PerspectiveNavItem.leaf("editor", "Code Editor", VaadinIcon.CODE,
                CodeEditorLayout::new, "tools", 0, true),
            PerspectiveNavItem.leaf("review", "Code Review", VaadinIcon.EYE,
                ReviewLayout::new, "tools", 1, false)
        );
    }
}
```

---

## Layout Relationships

`IPageLayout` constants for positioning views:

| Constant | Meaning |
|---|---|
| `IPageLayout.LEFT` | Left of the reference part |
| `IPageLayout.RIGHT` | Right of the reference part |
| `IPageLayout.TOP` | Above the reference part |
| `IPageLayout.BOTTOM` | Below the reference part |
| `IPageLayout.ID_EDITOR_AREA` | The central editor tab sheet |

The `ratio` parameter is the fraction of the **combined** space taken by the new view (e.g. `0.25f` = new view gets 25%, reference keeps 75%).

---

## Drag-and-Drop View Stacking

Views and stacked containers are DnD sources and targets out of the box. Dragging view A onto view B creates a `StackedViewContainer` that holds both in a tab strip. Dragging a view out of a stack restores it as a standalone panel. The layout tree is updated automatically; no extra code is needed in individual views.

---

## Session State

`PerspectiveState` (Spring `@SessionScope`) stores per-perspective splitter ratios and minimized panel IDs. When the user switches away from a perspective and returns, the layout is rebuilt and state is restored. Each nested navigation item within a perspective gets its own state slot.

---

## Sample Implementations

| Class | Type | Demonstrates |
|---|---|---|
| `VariablesView` | View | `IQuickFilterable`, Grid with quick filter |
| `ProjectExplorerView` | View | `IQuickFilterable`, TreeGrid, open editor on double-click |
| `OutlineView` | View | TreeGrid, static hierarchy |
| `ConsoleView` | View | Read-only TextArea, status bar line count |
| `CallStackView` | View | Grid, mock debugger data |
| `EmployeesEditor` | Editor | `ISearchableEditor`, all five search field types |
| `TextEditorPart` | Editor | Multiple tabs via `TextEditorInput` keyed on file path |
| `SystemPreferencesEditor` | Editor | System editor opened from PerspectiveBar button |
| `UserProfileEditor` | Editor | System editor opened from PerspectiveBar button |
| `JavaPerspective` | Perspective | `IPerspectiveNavigator`, nested nav groups and leaves |
| `DataPerspective` | Perspective | `createInitialEditors`, auto-opens EmployeesEditor |
| `DebugPerspective` | Perspective | Multi-view layout (variables, call stack, console) |

---

## Running the Application

```bash
mvn spring-boot:run
```

Then open `http://localhost:8080` in a browser.

```bash
mvn test   # runs 36 unit tests
```
