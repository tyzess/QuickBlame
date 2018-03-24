package ch.urszysset;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.history.GitHistoryUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QuickBlameAction extends AnAction {

    private final static String CAPTURE_AUTHOR_AND_EMAIL = "Author\\:\\s(.*?)\\s\\<(.*)?\\>";
    private final static String CAPTURE_CODE_ADDITIONS = "\\@\\@.*?\\@\\@(?:(?:.|\n)*?)\\+\\s*(.*)?\n";
    private final static String CAPTURE_CODE_ADDITIONS_EACH = "(?:(?:\\n|^)\\+\\s*(.*)?(?<!\\n)|(?:\\n|^)(?!\\+|\\-)\\s*(.*)?(?<!\\n))";
    private final static String MATCH_NEW_COMMIT_MESSAGE = "\n(?=commit[ ])";

    private final Pattern authorPattern = Pattern.compile(CAPTURE_AUTHOR_AND_EMAIL);
    private final Pattern codeAdditionsPattern = Pattern.compile(CAPTURE_CODE_ADDITIONS_EACH);
    private Caret caret;


    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = getEditor(event);
        VirtualFile virtualFile = getVirtualFile(event);
        caret = getCaret(event);

        int selectionStartLine = getSelectionStartLine();
        int selectionEndLine = getSelectionEndLine();

        String output = runGitLogCommand(project, virtualFile, selectionStartLine, selectionEndLine);
        List<String[]> changes = parseGitLogOutput(output);

        JBPopup jbPopup = buildPopup(project, changes);
        jbPopup.showInBestPositionFor(editor);
    }

    private Editor getEditor(AnActionEvent event) {
        return (Editor) event.getDataContext().getData(DataKeys.EDITOR.getName());
    }

    private VirtualFile getVirtualFile(AnActionEvent event) {
        return (VirtualFile) event.getDataContext().getData(DataKeys.VIRTUAL_FILE.getName());
    }

    private Caret getCaret(AnActionEvent event) {
        return (Caret) event.getDataContext().getData(DataKeys.CARET.getName());
    }

    private int getSelectionStartLine() {
        return caret.getSelectionStartPosition().getLine() + 1;
    }

    private int getSelectionEndLine() {
        return caret.getSelectionEndPosition().getLine() + 1;
    }

    private JBPopup buildPopup(Project project, List<String[]> changes) {
        JComponent panel = new JBPanel<>(new GridLayout(0, 2));

        for (String[] change : changes) {
            panel.add(new JBLabel(change[0]));
            panel.add(buildEditorField(project, change[1]));
        }

        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        return popupFactory.createComponentPopupBuilder(panel, null).createPopup();
    }

    private JComponent buildEditorField(Project project, String text) {
        EditorImpl readOnlyViewer = createViewer(project, text);
        readOnlyViewer.setOneLineMode(isOneLineMode());
        readOnlyViewer.setHighlighter(createLanguageHighlighter(project));
        setLineNumberOffset(readOnlyViewer);
        return readOnlyViewer.getComponent();
    }

    private void setLineNumberOffset(EditorImpl readOnlyViewer) {
        EditorGutterComponentEx gutter = (EditorGutterComponentEx) readOnlyViewer.getGutter();
        gutter.setLineNumberConvertor(i -> i + getSelectionStartLine());
    }

    private boolean isOneLineMode() {
        return getSelectionStartLine() == getSelectionEndLine();
    }

    private EditorImpl createViewer(Project project, String text) {
        Document myDocument = EditorFactory.getInstance().createDocument(text == null ? "" : text);
        return (EditorImpl) EditorFactory.getInstance().createViewer(myDocument, project);//, PREVIEW);
    }

    private EditorHighlighter createLanguageHighlighter(Project project) {
        FileType fileType = JavaLanguage.INSTANCE.getAssociatedFileType();
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType);
    }

    private List<String[]> parseGitLogOutput(String s) {
        String[] commits = s.split(MATCH_NEW_COMMIT_MESSAGE);
        List<String[]> changes = new ArrayList<>();
        for (String commit : commits) {
            String author = parseAuthor(commit);
            String code = parseCodeAdditions(commit);
            changes.add(new String[]{author, code});
        }
        return changes;
    }

    private String parseCodeAdditions(String commit) {
        StringBuilder code = new StringBuilder();
        commit = commit.split("\\@\\@(.*)?\\@\\@\n")[1];
        String[] lines = commit.split("\n");
        for (String line : lines) {
            if (line.startsWith("-")) {
                continue;
            }
            Matcher matcher = codeAdditionsPattern.matcher(line);
            if (matcher.find()) {
                code.append(matcher.group()).append("\n");
            } else {
                code.append("\n");
            }
        }

        return code.toString();
    }

    private String parseAuthor(String commit) {
        String author = "";
        Matcher matcher = authorPattern.matcher(commit);
        if (matcher.find()) {
            author = matcher.group(1);
            if (matcher.groupCount() >= 2) {
                author += " " + matcher.group(2);
            }
        }
        return author;
    }

    private String runGitLogCommand(Project project, VirtualFile virtualFile, int lineStart, int lineEnd) {
        try {
            FilePath currentFilePath = VcsUtil.getFilePath(virtualFile.getPath());
            FilePath repositoryFilePath = GitHistoryUtils.getLastCommitName(project, currentFilePath);
            VirtualFile root = GitUtil.getGitRoot(repositoryFilePath);
            GitSimpleHandler gitLogHandler = new GitSimpleHandler(project, root, GitCommand.LOG);
            gitLogHandler.setStdoutSuppressed(true);
            gitLogHandler.addParameters("-L" + lineStart + "," + lineEnd + ":" + virtualFile.getPath());
            gitLogHandler.endOptions();
            return gitLogHandler.run();
        } catch (VcsException e) {
            e.printStackTrace();
            return "";
        }
    }
}