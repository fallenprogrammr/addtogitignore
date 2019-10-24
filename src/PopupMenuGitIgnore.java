import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

public class PopupMenuGitIgnore extends AnAction {

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final GitIgnore gitIgnore = new GitIgnore(e.getProject());

        final VirtualFile highlightedItem = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        final String gitIgnoreEntry = getGitIgnoreEntry(highlightedItem);

        if (!gitIgnore.exists()) {
            gitIgnore.create();
        }
        gitIgnore.addEntry(gitIgnoreEntry);
    }

    private String getGitIgnoreEntry(final VirtualFile highlightedItem) {
        final StringBuilder sb = new StringBuilder(highlightedItem.getPresentableName());
        if (highlightedItem.isDirectory()) {
            sb.append(File.separator);
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void update(final AnActionEvent e) {
        final VirtualFile highlightedItem = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (isNotALocalFile(highlightedItem) || highlightedItem.getName().equals(GitIgnore.FILENAME)) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setText("Add " + highlightedItem.getName() + " to gitignore");
        }
    }

    private boolean isNotALocalFile(final VirtualFile highlightedItem) {
        // highlightedItem is null for 'External libraries'
        return highlightedItem == null || !highlightedItem.isInLocalFileSystem();
    }

}
