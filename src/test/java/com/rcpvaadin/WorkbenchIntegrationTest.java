package com.rcpvaadin;

import com.rcpvaadin.sample.perspective.JavaPerspective;
import com.rcpvaadin.workbench.IWorkbench;
import com.rcpvaadin.workbench.IWorkbenchPage;
import com.rcpvaadin.workbench.perspective.PageLayout;
import com.rcpvaadin.workbench.registry.WorkbenchRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class WorkbenchIntegrationTest {

    @Autowired IWorkbench        workbench;
    @Autowired WorkbenchRegistry registry;
    @Autowired ApplicationContext ctx;

    @Test
    void workbenchCanCreatePage() {
        IWorkbenchPage page = workbench.createPage();
        assertThat(page).isNotNull();
    }

    @Test
    void javaPageLayoutPopulatesCorrectly() {
        PageLayout layout = new PageLayout();
        new JavaPerspective().createInitialLayout(layout);
        assertThat(layout.getPlacements()).isNotEmpty();
    }

    @Test
    void allPerspectiveFactoriesAreBeans() {
        registry.getAllPerspectives().forEach(pd ->
                assertThatCode(() -> ctx.getBean(pd.factoryClass()))
                        .doesNotThrowAnyException());
    }
}
