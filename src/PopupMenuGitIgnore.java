import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;

public class PopupMenuGitIgnore extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String gitIgnorePath = project.getBasePath() + File.separator + ".gitignore";
        if(new File(gitIgnorePath).exists()){
            //Messages.showMessageDialog(project, "Hello", gitIgnorePath + " exists!", Messages.getInformationIcon());
        }
        else{
            try {
                project.getBaseDir().createChildData(this,".gitignore");
            } catch (IOException ioex) {
                Messages.showErrorDialog(ioex.getMessage(),"Could not create .gitignore file");
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final VirtualFile highlightedItem = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (highlightedItem.isDirectory()) {
            e.getPresentation().setText("Add " + highlightedItem.getName() +" to gitignore");
        } else {
            if(highlightedItem.getName().equals(".gitignore")){
                e.getPresentation().setEnabled(false);
            }else {
                e.getPresentation().setText("Add" + highlightedItem.getName() + " to gitignore");
            }
        }
    }

}
