package com.rcpvaadin.workbench.registry;

import com.rcpvaadin.workbench.IEditorPart;
import com.rcpvaadin.workbench.IViewPart;
import com.rcpvaadin.workbench.annotation.RcpEditor;
import com.rcpvaadin.workbench.annotation.RcpPerspective;
import com.rcpvaadin.workbench.annotation.RcpView;
import com.rcpvaadin.workbench.descriptor.EditorDescriptor;
import com.rcpvaadin.workbench.descriptor.PerspectiveDescriptor;
import com.rcpvaadin.workbench.descriptor.ViewDescriptor;
import com.rcpvaadin.workbench.perspective.IPerspectiveFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkbenchRegistry {

    @Autowired
    private ApplicationContext ctx;

    private final Map<String, ViewDescriptor>        views        = new LinkedHashMap<>();
    private final Map<String, EditorDescriptor>      editors      = new LinkedHashMap<>();
    private final Map<String, PerspectiveDescriptor> perspectives = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        ctx.getBeansWithAnnotation(RcpView.class).values().forEach(bean -> {
            Class<?> cls = AopUtils.getTargetClass(bean);
            RcpView ann  = cls.getAnnotation(RcpView.class);
            if (ann != null && IViewPart.class.isAssignableFrom(cls)) {
                @SuppressWarnings("unchecked")
                Class<? extends IViewPart> viewClass = (Class<? extends IViewPart>) cls;
                views.put(ann.id(), new ViewDescriptor(ann.id(), ann.name(), ann.icon(), viewClass));
            }
        });

        ctx.getBeansWithAnnotation(RcpEditor.class).values().forEach(bean -> {
            Class<?> cls  = AopUtils.getTargetClass(bean);
            RcpEditor ann = cls.getAnnotation(RcpEditor.class);
            if (ann != null && IEditorPart.class.isAssignableFrom(cls)) {
                @SuppressWarnings("unchecked")
                Class<? extends IEditorPart> editorClass = (Class<? extends IEditorPart>) cls;
                editors.put(ann.id(), new EditorDescriptor(ann.id(), ann.name(), ann.icon(), editorClass));
            }
        });

        ctx.getBeansWithAnnotation(RcpPerspective.class).values().forEach(bean -> {
            Class<?> cls       = AopUtils.getTargetClass(bean);
            RcpPerspective ann = cls.getAnnotation(RcpPerspective.class);
            if (ann != null && IPerspectiveFactory.class.isAssignableFrom(cls)) {
                @SuppressWarnings("unchecked")
                Class<? extends IPerspectiveFactory> factoryClass = (Class<? extends IPerspectiveFactory>) cls;
                perspectives.put(ann.id(), new PerspectiveDescriptor(ann.id(), ann.name(), ann.icon(), factoryClass));
            }
        });
    }

    public Optional<ViewDescriptor>        findView(String id)        { return Optional.ofNullable(views.get(id)); }
    public Optional<EditorDescriptor>      findEditor(String id)      { return Optional.ofNullable(editors.get(id)); }
    public Optional<PerspectiveDescriptor> findPerspective(String id) { return Optional.ofNullable(perspectives.get(id)); }

    public Collection<ViewDescriptor>        getAllViews()        { return Collections.unmodifiableCollection(views.values()); }
    public Collection<EditorDescriptor>      getAllEditors()      { return Collections.unmodifiableCollection(editors.values()); }
    public Collection<PerspectiveDescriptor> getAllPerspectives() { return Collections.unmodifiableCollection(perspectives.values()); }
}
