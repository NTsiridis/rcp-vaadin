package com.rcpvaadin.sample.editor;

import com.rcpvaadin.workbench.IEditorInput;
import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IPartSite;
import com.rcpvaadin.workbench.ToolbarItem;
import com.rcpvaadin.workbench.annotation.RcpEditor;
import com.rcpvaadin.workbench.search.ISearchableEditor;
import com.rcpvaadin.workbench.search.SearchCriteria;
import com.rcpvaadin.workbench.search.SearchFieldDescriptor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.ListDataProvider;
import org.springframework.context.annotation.Scope;

import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
@RcpEditor(id = "employeesEditor", name = "Employees", icon = VaadinIcon.USERS)
public class EmployeesEditor implements IEditorPart, ISearchableEditor {

    public record Employee(String name, String department, LocalDate hireDate, boolean active) {}

    private static final List<Employee> ALL_EMPLOYEES = List.of(
            new Employee("Alice Johnson",  "Engineering", LocalDate.of(2019, 3, 15), true),
            new Employee("Bob Smith",      "HR",          LocalDate.of(2021, 7,  1), true),
            new Employee("Carol White",    "Finance",     LocalDate.of(2018, 11, 20), false),
            new Employee("David Brown",    "Engineering", LocalDate.of(2022, 1,  10), true),
            new Employee("Eva Martinez",   "Finance",     LocalDate.of(2020, 5,   5), true),
            new Employee("Frank Lee",      "HR",          LocalDate.of(2017, 9,  30), false),
            new Employee("Grace Kim",      "Engineering", LocalDate.of(2023, 2,  28), true)
    );

    private IEditorInput input;
    private IPartSite    site;
    private ListDataProvider<Employee> dataProvider;

    @Override public String getTitle()        { return "Employees"; }
    @Override public void   init(IPartSite s) { this.site = s; }
    @Override public void   dispose()         {}
    @Override public IPartSite getSite()      { return site; }

    @Override public void         setInput(IEditorInput input) { this.input = input; }
    @Override public IEditorInput getEditorInput()             { return input; }
    @Override public boolean      isDirty()                    { return false; }

    @Override
    public List<ToolbarItem> getToolbarItems() {
        return List.of(
            new ToolbarItem(VaadinIcon.REFRESH, "Refresh",
                    () -> { if (site != null) site.setStatusMessage("Refreshed"); })
        );
    }

    @Override
    public List<SearchFieldDescriptor> getSearchFields() {
        return List.of(
                SearchFieldDescriptor.text("name",       "Name"),
                SearchFieldDescriptor.select("dept",     "Department",
                        List.of("Engineering", "HR", "Finance")),
                SearchFieldDescriptor.dateRange("hireDate", "Hire Date"),
                SearchFieldDescriptor.bool("active",     "Active")
        );
    }

    @Override
    public void executeSearch(SearchCriteria c) {
        if (dataProvider == null) return;
        dataProvider.clearFilters();
        dataProvider.addFilter(emp -> {
            String name = c.text("name");
            if (!name.isBlank() && !emp.name().toLowerCase().contains(name.toLowerCase())) return false;

            String dept = c.selected("dept");
            if (!dept.isBlank() && !emp.department().equals(dept)) return false;

            LocalDate from = c.dateFrom("hireDate");
            if (from != null && emp.hireDate().isBefore(from)) return false;

            LocalDate to = c.dateTo("hireDate");
            if (to != null && emp.hireDate().isAfter(to)) return false;

            // active filter only applied when a value was explicitly provided
            if (c.values().containsKey("active")) {
                boolean activeFilter = c.bool("active");
                if (emp.active() != activeFilter) return false;
            }

            return true;
        });

        if (site != null) site.setStatusMessage("Search applied");
    }

    @Override
    public void clearSearch() {
        if (dataProvider != null) dataProvider.clearFilters();
        if (site != null) site.setStatusMessage("Filter cleared");
    }

    @Override
    public Component createPartControl() {
        Grid<Employee> grid = new Grid<>(Employee.class, false);
        grid.addColumn(Employee::name).setHeader("Name").setSortable(true);
        grid.addColumn(Employee::department).setHeader("Department").setSortable(true);
        grid.addColumn(Employee::hireDate).setHeader("Hire Date").setSortable(true);
        grid.addColumn(e -> e.active() ? "Yes" : "No").setHeader("Active").setSortable(true);
        grid.setSizeFull();

        dataProvider = new ListDataProvider<>(ALL_EMPLOYEES);
        grid.setDataProvider(dataProvider);

        if (site != null) site.setSystemInfo(ALL_EMPLOYEES.size() + " employees");
        return grid;
    }
}
