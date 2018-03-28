package ch.urszysset;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.history.GitHistoryUtils;

public class GitUtil {

    public static String runGitLogCommand(Project project, VirtualFile virtualFile, int lineStart, int lineEnd) {
        try {
            FilePath currentFilePath = VcsUtil.getFilePath(virtualFile.getPath());
            FilePath repositoryFilePath = GitHistoryUtils.getLastCommitName(project, currentFilePath);
            VirtualFile root = git4idea.GitUtil.getGitRoot(repositoryFilePath);
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
