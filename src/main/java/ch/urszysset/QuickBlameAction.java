package ch.urszysset;

import com.intellij.codeInsight.hint.TooltipController;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.annotate.GitAnnotationProvider;
import git4idea.annotate.GitFileAnnotation;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QuickBlameAction extends AnAction {

    private final static String AUTHOR_REGEX = "Author\\:(.*)";

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        VirtualFile virtualFile = (VirtualFile) event.getDataContext().getData(DataKeys.VIRTUAL_FILE.getName());
        Editor editor = (Editor) event.getDataContext().getData(DataKeys.EDITOR.getName());
        Caret caret = (Caret) event.getDataContext().getData(DataKeys.CARET.getName());

        if (project == null || virtualFile == null || editor == null || caret == null) {
            return; // TODO improve null cases
        }

        GitFileAnnotation annotation = getGitFileAnnotation(project, virtualFile);

        if (annotation == null) {
            return; // TODO improve null cases
        }

        int lineNumber = getLineNumber(caret);
        String toolTip = annotation.getToolTip(lineNumber);

        if (toolTip != null) {
            Pattern pattern = Pattern.compile(AUTHOR_REGEX);
            Matcher matcher = pattern.matcher(toolTip);
            if (matcher.find()) {
                String author = matcher.group(1);
                Point point = editor.logicalPositionToXY(editor.getCaretModel().getLogicalPosition());
                SwingUtilities.convertPointToScreen(point, editor.getContentComponent());
                TooltipGroup tooltipGroup = new TooltipGroup("WhatShouldBeHere", 100);
                TooltipController.getInstance().showTooltip(editor, point, author, true, tooltipGroup);
            }
        }
    }

    @Nullable
    private GitFileAnnotation getGitFileAnnotation(Project project, VirtualFile virtualFile) {
        GitFileAnnotation annotate = null;
        try {
            annotate = (GitFileAnnotation) new GitAnnotationProvider(project).annotate(virtualFile);
        } catch (VcsException e) {
            e.printStackTrace();
        }
        return annotate;
    }

    private int getLineNumber(Caret caret) {
        LogicalPosition logicalPosition = caret.getLogicalPosition();
        return logicalPosition.line;
    }

    @Override
    public void update(AnActionEvent e) {
        // TODO implement
    }
}