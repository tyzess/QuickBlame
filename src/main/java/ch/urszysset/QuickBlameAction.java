package ch.urszysset;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QuickBlameAction extends AnAction {

    private final static String MATCH_NEW_COMMIT_MESSAGE = "\n(?=commit[ ])";
    private final static String MATCH_CODE_SECTION_START = "@@(.*)?@@\n";

    private final static String CAPTURE_DATE = "Date:\\s(.*?)\n";
    private final static String CAPTURE_AUTHOR = "Author:\\s(.*?)\\s<";
    private final static String CAPTURE_EMAIL = "Author:\\s(?:.*?)\\s<(.*?)>";

    private final Pattern datePattern = Pattern.compile(CAPTURE_DATE);
    private final Pattern emailPattern = Pattern.compile(CAPTURE_EMAIL);
    private final Pattern authorPattern = Pattern.compile(CAPTURE_AUTHOR);

    private final QuickBlameSettings quickBlameSettings = QuickBlameSettings.getInstance();

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = getEditor(event);
        VirtualFile virtualFile = getVirtualFile(event);
        Caret caret = getCaret(event);

        int selectionStartLine = getSelectionStartLine(caret);
        int selectionEndLine = getSelectionEndLine(caret);

        String output = GitUtil.runGitLogCommand(project, virtualFile, selectionStartLine, selectionEndLine);
        List<CommitInfo> commitInfos = parseGitLogOutput(output, selectionStartLine);

        JBPopup popup = QuickBlamePopupBuilder.buildPopup(project, commitInfos);
        popup.showInBestPositionFor(editor);
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

    private int getSelectionStartLine(Caret caret) {
        return caret.getSelectionStartPosition().getLine() + 1;
    }

    private int getSelectionEndLine(Caret caret) {
        return caret.getSelectionEndPosition().getLine() + 1;
    }

    private List<CommitInfo> parseGitLogOutput(String gitLogMessage, int selectionStartLine) {
        List<CommitInfo> commitInfos = new ArrayList<>();
        for (String commitMessage : splitByCommit(gitLogMessage)) {
            CommitInfo commitInfo = new CommitInfo(selectionStartLine);
            commitInfo.setAuthor(parseAuthor(commitMessage));
            commitInfo.setEmail(parseEmail(commitMessage));
            commitInfo.setDate(parseDate(commitMessage));
            commitInfo.setCode(parseCode(commitMessage));
            commitInfos.add(commitInfo);
        }
        return commitInfos;
    }

    @NotNull
    private String[] splitByCommit(String gitLogMessage) {
        return gitLogMessage.split(MATCH_NEW_COMMIT_MESSAGE);
    }

    private String parseCode(String commit) {
        return splitOffIntoCodeSection(commit);
    }

    private String splitOffIntoCodeSection(String commit) {
        return commit.split(MATCH_CODE_SECTION_START)[1];
    }

    private String parseAuthor(String commit) {
        String author = parsePattern(commit, authorPattern);
        if (quickBlameSettings.containsMappingForAuthor(author)) {
            return quickBlameSettings.getMappedAuthorName(author);
        }
        return author;
    }

    private String parseEmail(String commit) {
        return parsePattern(commit, emailPattern);
    }

    private String parseDate(String commit) {
        return parsePattern(commit, datePattern);
    }

    private String parsePattern(String commit, Pattern pattern) {
        Matcher matcher = pattern.matcher(commit);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}