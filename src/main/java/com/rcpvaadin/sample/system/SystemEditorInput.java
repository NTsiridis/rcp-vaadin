package com.rcpvaadin.sample.system;

import com.rcpvaadin.workbench.IEditorInput;

import java.util.Objects;

/**
 * Singleton-style editor input for system editors (user profile, preferences).
 * Keyed on the editor id so only one tab per system editor can be open at a time.
 */
public class SystemEditorInput implements IEditorInput {

    private final String editorId;
    private final String name;

    public SystemEditorInput(String editorId, String name) {
        this.editorId = editorId;
        this.name     = name;
    }

    @Override public String getName()    { return name; }
    @Override public String getToolTip() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemEditorInput other)) return false;
        return Objects.equals(editorId, other.editorId);
    }

    @Override public int hashCode() { return Objects.hashCode(editorId); }
}
