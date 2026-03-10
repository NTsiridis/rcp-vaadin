package com.rcpvaadin.sample.editor;

import com.rcpvaadin.workbench.IEditorInput;

import java.util.Objects;

/**
 * Editor input keyed on filePath — same path always maps to the same tab.
 */
public class TextEditorInput implements IEditorInput {

    private final String filePath;
    private final String fileName;

    public TextEditorInput(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    public String getFilePath() { return filePath; }

    @Override public String getName()    { return fileName; }
    @Override public String getToolTip() { return filePath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextEditorInput other)) return false;
        return Objects.equals(filePath, other.filePath);
    }

    @Override
    public int hashCode() { return Objects.hashCode(filePath); }
}
