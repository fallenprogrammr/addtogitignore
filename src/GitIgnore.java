import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class GitIgnore {

    public static final String FILENAME = ".gitignore";

    private final Project project;

    public GitIgnore(final Project project) {
        this.project = Objects.requireNonNull(project);
    }

    private String getGitIgnorePath() {
        return project.getBasePath() + File.separator + FILENAME;
    }

    boolean exists() {
        return new File(getGitIgnorePath()).exists();
    }

    void create() {
        Application application = ApplicationManager.getApplication();
        application.runWriteAction(() -> {
            try {
                project.getBaseDir().createChildData(this, FILENAME);
            } catch (IOException ioex) {
                Messages.showErrorDialog(ioex.getMessage(), "Could not create .gitignore file");
            }
        });
    }

    void addEntry(final String gitignoreEntry) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(getGitIgnorePath(), true));
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
}
