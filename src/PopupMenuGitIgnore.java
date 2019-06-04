import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PopupMenuGitIgnore extends AnAction {

    private static final String GITIGNORE = ".gitignore";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String gitIgnorePath = project.getBasePath() + File.separator + GITIGNORE;
        final VirtualFile highlightedItem = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (!new File(gitIgnorePath).exists()) {
            createGitIgnoreFile(project);
        }
        writeHighlightedItemToGitIgnore(gitIgnorePath, highlightedItem);
    }

    private void createGitIgnoreFile(Project project) {
        Application application = ApplicationManager.getApplication();
        application.runWriteAction(() -> {
            try {
                project.getBaseDir().createChildData(this, GITIGNORE);
            } catch (IOException ioex) {
                Messages.showErrorDialog(ioex.getMessage(), "Could not create .gitignore file");
            }
        });
    }

    private void writeHighlightedItemToGitIgnore(String gitIgnorePath, VirtualFile highlightedItem) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(gitIgnorePath, true));
            String gitignoreEntry = getGitignoreEntry(highlightedItem);
            writer.write(gitignoreEntry);
        } catch (IOException e1) {
            Messages.showErrorDialog(e1.getMessage(), "Could not write to gitignore");
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private String getGitignoreEntry(VirtualFile highlightedItem) {
        return ((highlightedItem.isDirectory()) ? highlightedItem.getPresentableName() + File.separator : highlightedItem.getPresentableName()) + System.lineSeparator();
    }

    @Override
    public void update(AnActionEvent e) {
        final VirtualFile highlightedItem = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (isNotALocalFile(highlightedItem) || highlightedItem.getName().equals(GITIGNORE)) {
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
