package com.rcpvaadin.workbench;

import com.rcpvaadin.sample.editor.TextEditorInput;
import com.rcpvaadin.sample.editor.TextEditorPart;
import com.rcpvaadin.sample.view.ConsoleView;
import com.rcpvaadin.workbench.descriptor.EditorDescriptor;
import com.rcpvaadin.workbench.descriptor.ViewDescriptor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.rcpvaadin.workbench.registry.WorkbenchRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkbenchPageTest {

    private WorkbenchPage                       page;
    private IWorkbench                          workbench;
    private WorkbenchRegistry                   registry;
    private ApplicationContext                  ctx;
    private WorkbenchPage.WorkbenchPageListener listener;

    @BeforeEach
    void setUp() {
        registry  = mock(WorkbenchRegistry.class);
        ctx       = mock(ApplicationContext.class);
        workbench = mock(IWorkbench.class);
        when(workbench.getRegistry()).thenReturn(registry);

        listener = mock(WorkbenchPage.WorkbenchPageListener.class);
        page     = new WorkbenchPage(workbench, ctx);
        page.addListener(listener);
    }

    @Test
    void openEditorIsIdempotent() {
        TextEditorInput input      = new TextEditorInput("/path/Main.java", "Main.java");
        TextEditorPart  editorPart = new TextEditorPart();

        when(registry.findEditor("textEditor"))
                .thenReturn(Optional.of(new EditorDescriptor("textEditor", "Text Editor", VaadinIcon.EDIT, TextEditorPart.class)));
        when(ctx.getBean(TextEditorPart.class)).thenReturn(editorPart);

        IEditorPart first  = page.openEditor(input, "textEditor");
        IEditorPart second = page.openEditor(input, "textEditor");

        assertThat(first).isSameAs(second);
        verify(ctx, times(1)).getBean(TextEditorPart.class);
        verify(listener, times(2)).editorOpened(any(), any());
    }

    @Test
    void closeEditorRemovesIt() {
        TextEditorInput input      = new TextEditorInput("/path/Main.java", "Main.java");
        TextEditorPart  editorPart = new TextEditorPart();

        when(registry.findEditor("textEditor"))
                .thenReturn(Optional.of(new EditorDescriptor("textEditor", "Text Editor", VaadinIcon.EDIT, TextEditorPart.class)));
        when(ctx.getBean(TextEditorPart.class)).thenReturn(editorPart);

        page.openEditor(input, "textEditor");
        page.closeEditor(input);

        assertThat(page.getOpenEditors()).isEmpty();
        verify(listener).editorClosed(input);
    }

    @Test
    void switchPerspectiveClearsViews() {
        ConsoleView consoleView = new ConsoleView();
        when(registry.findView("console"))
                .thenReturn(Optional.of(new ViewDescriptor("console", "Console", VaadinIcon.TERMINAL, ConsoleView.class)));
        when(ctx.getBean(ConsoleView.class)).thenReturn(consoleView);

        page.showView("console");
        assertThat(page.isViewVisible("console")).isTrue();

        page.setPerspective("debugPerspective");
        assertThat(page.isViewVisible("console")).isFalse();

        verify(listener).perspectiveChanged("debugPerspective");
    }

    @Test
    void showViewInitializesIt() {
        ConsoleView consoleView = new ConsoleView();
        when(registry.findView("console"))
                .thenReturn(Optional.of(new ViewDescriptor("console", "Console", VaadinIcon.TERMINAL, ConsoleView.class)));
        when(ctx.getBean(ConsoleView.class)).thenReturn(consoleView);

        page.showView("console");

        assertThat(page.isViewVisible("console")).isTrue();
        verify(listener).viewVisibilityChanged("console", true);
    }
}
