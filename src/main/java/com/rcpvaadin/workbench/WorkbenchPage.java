package com.rcpvaadin.workbench;

import com.rcpvaadin.workbench.descriptor.EditorDescriptor;
import com.rcpvaadin.workbench.descriptor.ViewDescriptor;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * Core model state machine. Not a Spring bean — one instance is owned per browser tab
 * (per WorkbenchView). All state transitions fire events to registered listeners so
 * that the UI layer can react without the model knowing about Vaadin components.
 */
public class WorkbenchPage implements IWorkbenchPage {

    public interface WorkbenchPageListener {
        void editorOpened(IEditorPart editor, IEditorInput input);
        void editorClosed(IEditorInput input);
        void perspectiveChanged(String perspectiveId);
        void viewVisibilityChanged(String viewId, boolean visible);
    }

    private final IWorkbench         workbench;
    private final ApplicationContext ctx;

    private final LinkedHashMap<IEditorInput, IEditorPart> openEditors = new LinkedHashMap<>();
    private final Map<String, IViewPart>                   openViews   = new LinkedHashMap<>();

    private String activePerspectiveId;
    private final List<WorkbenchPageListener> listeners = new ArrayList<>();

    public WorkbenchPage(IWorkbench workbench, ApplicationContext ctx) {
        this.workbench = workbench;
        this.ctx       = ctx;
    }

    public void addListener(WorkbenchPageListener listener)    { listeners.add(listener); }
    public void removeListener(WorkbenchPageListener listener) { listeners.remove(listener); }

    // -------------------------------------------------------------------------
    // IWorkbenchPage
    // -------------------------------------------------------------------------

    @Override
    public IEditorPart openEditor(IEditorInput input, String editorId) {
        if (openEditors.containsKey(input)) {
            IEditorPart existing = openEditors.get(input);
            fireEditorOpened(existing, input);   // re-focus existing tab
            return existing;
        }
        EditorDescriptor desc = workbench.getRegistry().findEditor(editorId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown editor id: " + editorId));
        IEditorPart editor = (IEditorPart) ctx.getBean(desc.editorClass());
        editor.init(new PartSite(editorId, editor, this, workbench));
        editor.setInput(input);
        openEditors.put(input, editor);
        fireEditorOpened(editor, input);
        return editor;
    }

    @Override
    public void closeEditor(IEditorInput input) {
        IEditorPart editor = openEditors.remove(input);
        if (editor != null) {
            editor.dispose();
            fireEditorClosed(input);
        }
    }

    @Override
    public void showView(String viewId) {
        if (!openViews.containsKey(viewId)) {
            ViewDescriptor desc = workbench.getRegistry().findView(viewId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown view id: " + viewId));
            IViewPart view = (IViewPart) ctx.getBean(desc.viewClass());
            view.init(new PartSite(viewId, view, this, workbench));
            openViews.put(viewId, view);
        }
        fireViewVisibilityChanged(viewId, true);
    }

    @Override
    public void hideView(String viewId) {
        IViewPart view = openViews.remove(viewId);
        if (view != null) {
            view.dispose();
            fireViewVisibilityChanged(viewId, false);
        }
    }

    @Override
    public boolean isViewVisible(String viewId) {
        return openViews.containsKey(viewId);
    }

    @Override
    public IViewPart getOpenView(String viewId) {
        return openViews.get(viewId);
    }

    @Override
    public void setPerspective(String perspectiveId) {
        this.activePerspectiveId = perspectiveId;
        openViews.values().forEach(IWorkbenchPart::dispose);
        openViews.clear();
        firePerspectiveChanged(perspectiveId);
    }

    @Override
    public String getActivePerspectiveId() {
        return activePerspectiveId;
    }

    @Override
    public List<IEditorPart> getOpenEditors() {
        return List.copyOf(openEditors.values());
    }

    // -------------------------------------------------------------------------
    // Event firing helpers
    // -------------------------------------------------------------------------

    private void fireEditorOpened(IEditorPart editor, IEditorInput input) {
        List.copyOf(listeners).forEach(l -> l.editorOpened(editor, input));
    }

    private void fireEditorClosed(IEditorInput input) {
        List.copyOf(listeners).forEach(l -> l.editorClosed(input));
    }

    private void firePerspectiveChanged(String id) {
        List.copyOf(listeners).forEach(l -> l.perspectiveChanged(id));
    }

    private void fireViewVisibilityChanged(String viewId, boolean visible) {
        List.copyOf(listeners).forEach(l -> l.viewVisibilityChanged(viewId, visible));
    }
}
