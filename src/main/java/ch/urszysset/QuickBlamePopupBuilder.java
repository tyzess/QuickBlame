package ch.urszysset;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class QuickBlamePopupBuilder {

    public static JBPopup buildPopup(Project project, List<CommitInfo> commitInfos) {
        JComponent panel = new JBPanel<>(new GridBagLayout());

        int y = 0;
        for (CommitInfo commitInfo : commitInfos) {
            panel.add(new JBLabel(commitInfo.getAuthor()), buildGridBagConstraints(0, y));
            panel.add(new JBLabel(commitInfo.getEmail()), buildGridBagConstraints(0, y + 1));
            panel.add(new JBLabel(commitInfo.getDate()), buildGridBagConstraints(0, y + 2));
            panel.add(buildEditorField(project, commitInfo), buildGridBagConstraints(1, y, 3));
            y += 3;
        }

        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        return popupFactory.createComponentPopupBuilder(panel, null).createPopup();
    }

    private static GridBagConstraints buildGridBagConstraints(int x, int y) {
        return buildGridBagConstraints(x, y, 1);
    }

    private static GridBagConstraints buildGridBagConstraints(int x, int y, int weightY) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = x;
        c.gridy = y;
        c.gridheight = weightY;
        return c;
    }

    private static JComponent buildEditorField(Project project, CommitInfo commitInfo) {
        EditorImpl readOnlyViewer = createViewer(project, commitInfo.getCode());
        readOnlyViewer.setOneLineMode(commitInfo.isOneLineCode());
        readOnlyViewer.setHighlighter(createLanguageHighlighter(project));
        setLineNumberOffset(readOnlyViewer, commitInfo.getStartLineNumber());

        readOnlyViewer.setVerticalScrollbarVisible(false);
        readOnlyViewer.getInsets().set(0, 0, 0, 0); //TODO is this necessary?
        readOnlyViewer.getSettings().setCaretRowShown(false);
        readOnlyViewer.getSettings().setAdditionalLinesCount(0);

        return readOnlyViewer.getComponent();
    }

    private static void setLineNumberOffset(EditorImpl readOnlyViewer, int startLineNumber) {
        EditorGutterComponentEx gutter = (EditorGutterComponentEx) readOnlyViewer.getGutter();
        gutter.setLineNumberConvertor(i -> i + startLineNumber);
    }

    private static EditorImpl createViewer(Project project, String code) {
        Document myDocument = EditorFactory.getInstance().createDocument(code);
        return (EditorImpl) EditorFactory.getInstance().createViewer(myDocument, project, EditorKind.DIFF); //TODO What's the purpose of EditorKind??
    }

    private static EditorHighlighter createLanguageHighlighter(Project project) {
        FileType fileType = JavaLanguage.INSTANCE.getAssociatedFileType();
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType);
    }
}
