package com.euphoricity.addtogitignore;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.MapDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class PopupMenuGitIgnoreTest extends BasePlatformTestCase {

    private PopupMenuGitIgnore action;
    private Path projectBase;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        action = new PopupMenuGitIgnore();
        projectBase = Path.of(Objects.requireNonNull(getProject().getBasePath()));
    }

    @Override
    protected void tearDown() throws Exception {
        Files.deleteIfExists(projectBase.resolve(".gitignore"));
        super.tearDown();
    }

    // ------------------------------------------------------------------ tests

    public void testActionAppendsFileToGitignore() throws IOException {
        VirtualFile targetFile = localFile("Foo.java", "class Foo {}");
        localFile(".gitignore", "*.class\n");

        action.actionPerformed(event(targetFile));

        assertContains(".gitignore", "Foo.java");
    }

    public void testActionCreatesGitignoreWhenMissing() throws IOException {
        VirtualFile targetFile = localFile("output.jar", "");
        Files.deleteIfExists(projectBase.resolve(".gitignore"));

        action.actionPerformed(event(targetFile));

        assertTrue(".gitignore should be created", Files.exists(projectBase.resolve(".gitignore")));
        assertContains(".gitignore", "output.jar");
    }

    public void testActionAppendsDirectoryWithSeparator() throws IOException {
        Path buildDir = projectBase.resolve("build");
        Files.createDirectories(buildDir);
        VirtualFile dir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(buildDir);
        assertNotNull(dir);
        localFile(".gitignore", "");

        action.actionPerformed(event(dir));

        assertContains(".gitignore", "build" + File.separator);
    }

    public void testUpdateDisablesActionForGitignoreItself() throws IOException {
        VirtualFile gitignore = localFile(".gitignore", "");

        AnActionEvent e = event(gitignore);
        action.update(e);

        assertFalse("Action must be disabled when .gitignore is selected",
                e.getPresentation().isEnabled());
    }

    public void testUpdateSetsTextWithFileName() throws IOException {
        VirtualFile targetFile = localFile("settings.xml", "<root/>");

        AnActionEvent e = event(targetFile);
        action.update(e);

        assertTrue("Presentation text should include the file name",
                e.getPresentation().getText().contains("settings.xml"));
    }

    // ------------------------------------------------------------------ helpers

    /** Creates a file on the real local filesystem so isInLocalFileSystem() == true. */
    private VirtualFile localFile(String relativePath, String content) throws IOException {
        Path path = projectBase.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertNotNull("VirtualFile not found at " + path, vf);
        return vf;
    }

    private void assertContains(String relativePath, String expected) throws IOException {
        String content = Files.readString(projectBase.resolve(relativePath));
        assertTrue("Expected '" + expected + "' in " + relativePath + " but got:\n" + content,
                content.contains(expected));
    }

    private AnActionEvent event(VirtualFile file) {
        MapDataContext ctx = new MapDataContext();
        ctx.put(CommonDataKeys.PROJECT, getProject());
        ctx.put(CommonDataKeys.VIRTUAL_FILE, file);
        return TestActionEvent.createTestEvent(action, ctx);
    }
}
