package com.rcpvaadin.workbench.registry;

import com.rcpvaadin.workbench.descriptor.EditorDescriptor;
import com.rcpvaadin.workbench.descriptor.PerspectiveDescriptor;
import com.rcpvaadin.workbench.descriptor.ViewDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class WorkbenchRegistryTest {

    @Autowired
    WorkbenchRegistry registry;

    @Test
    void allViewsAreRegistered() {
        assertThat(registry.getAllViews())
                .extracting(ViewDescriptor::id)
                .containsExactlyInAnyOrder("projectExplorer", "outline", "console", "variables", "callStack");
    }

    @Test
    void allEditorsAreRegistered() {
        assertThat(registry.getAllEditors())
                .extracting(EditorDescriptor::id)
                .containsExactlyInAnyOrder("textEditor", "userProfile", "systemPreferences", "employeesEditor");
    }

    @Test
    void allPerspectivesAreRegistered() {
        assertThat(registry.getAllPerspectives())
                .extracting(PerspectiveDescriptor::id)
                .containsExactlyInAnyOrder("javaPerspective", "debugPerspective", "dataPerspective");
    }
}
