# agents.md — Fix & Modernize addtogitignore IntelliJ Plugin

## Context

`addtogitignore` is an IntelliJ IDEA plugin that adds a right-click context menu option in the Project Explorer to append a file or directory to `.gitignore`. It was written ~7 years ago against an old IntelliJ Platform SDK (since-build 143.0 / IDEA 15). The current plugin source is a single action class ([src/PopupMenuGitIgnore.java](src/PopupMenuGitIgnore.java)) in a legacy DevKit module format with no Gradle build file.

Newer versions of IntelliJ (2022.3+) have removed or deprecated several APIs this plugin relies on, causing exceptions at startup and at action invocation.

---

## Known Issues to Fix

### 1. `project.getBaseDir()` is removed

`getBaseDir()` on `Project` was deprecated in 2019.3 and **removed** in recent SDK versions. Calling it throws `NoSuchMethodError` at runtime.

**Fix:** Replace with `Path`-based or `LocalFileSystem`-based lookup:
```java
// Old (broken)
project.getBaseDir().createChildData(this, GITIGNORE);

// New
Path basePath = Path.of(project.getBasePath());
VirtualFile baseDir = LocalFileSystem.getInstance().findFileByNioFile(basePath);
baseDir.createChildData(this, GITIGNORE);
```

### 2. Missing `getActionUpdateThread()` causes `AssertionError`

Since IntelliJ 2022.3, every `AnAction` subclass **must** override `getActionUpdateThread()` and return either `ActionUpdateThread.EDT` or `ActionUpdateThread.BGT`. Omitting this override causes an assertion error on every action update tick.

**Fix:** Add to `PopupMenuGitIgnore`:
```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
}
```

### 3. Null safety in `actionPerformed`

`e.getProject()` can return `null`. The current code calls `project.getBasePath()` without a null check, throwing `NullPointerException`.

**Fix:** Guard at the top of `actionPerformed`:
```java
Project project = e.getProject();
if (project == null) return;
```

### 4. VFS not refreshed after writing `.gitignore`

The plugin writes to `.gitignore` via raw `FileWriter` but never notifies the IntelliJ Virtual File System. This means the IDE doesn't see the change until a manual VFS sync, and can throw `InvalidVirtualFileAccessException` on subsequent accesses.

**Fix:** After writing, call:
```java
VirtualFile gitignoreVf = LocalFileSystem.getInstance().findFileByPath(gitIgnorePath);
if (gitignoreVf != null) {
    gitignoreVf.refresh(false, false);
}
```

### 5. No Gradle build — project cannot be compiled or distributed

The plugin uses a legacy IntelliJ DevKit module (`.iml` + manually managed SDK). There is no `build.gradle.kts` or `settings.gradle.kts`. This makes it impossible to build from the command line, run tests, or publish to the JetBrains Marketplace.

**Fix:** Migrate to the [IntelliJ Platform Gradle Plugin v2](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html). Minimum required files:
- `settings.gradle.kts`
- `build.gradle.kts` declaring `intellijPlatform` dependency and `pluginVerification` task
- Move `src/` to `src/main/java/` (standard Gradle source layout)
- Move `resources/` to `src/main/resources/`

### 6. `plugin.xml` targets a stale build range

`since-build="143.0"` (IDEA 15, 2016) is ancient. The Marketplace and recent IDEs expect a realistic `since-build` / `until-build` range.

**Fix:** Update to a modern minimum, e.g.:
```xml
<idea-version since-build="232" until-build="251.*"/>
```
(232 = 2023.2, adjust `until-build` to match the IntelliJ Platform version targeted in `build.gradle.kts`.)

---

## Implementation Plan

### Step 1 — Migrate to Gradle build

1. Create `settings.gradle.kts`:
   ```kotlin
   rootProject.name = "addtogitignore"
   ```
2. Create `build.gradle.kts` using IntelliJ Platform Gradle Plugin v2:
   ```kotlin
   plugins {
       id("java")
       id("org.jetbrains.intellij.platform") version "2.+"
   }

   group = "com.euphoricity"
   version = "0.4.0"

   repositories {
       mavenCentral()
       intellijPlatform { defaultRepositories() }
   }

   dependencies {
       intellijPlatform {
           intellijIdeaCommunity("2024.1")
           pluginVerifier()
           zipSigner()
       }
   }

   intellijPlatform {
       pluginConfiguration {
           ideaVersion {
               sinceBuild = "232"
               untilBuild = "251.*"
           }
       }
   }
   ```
3. Move source files:
   - `src/PopupMenuGitIgnore.java` → `src/main/java/com/euphoricity/addtogitignore/PopupMenuGitIgnore.java`
   - `resources/META-INF/plugin.xml` → `src/main/resources/META-INF/plugin.xml`
4. Add package declaration to `PopupMenuGitIgnore.java`: `package com.euphoricity.addtogitignore;`
5. Update `plugin.xml` `<action class>` to use the fully qualified class name: `com.euphoricity.addtogitignore.PopupMenuGitIgnore`
6. Delete the legacy `.iml` file.

### Step 2 — Fix deprecated/removed APIs in `PopupMenuGitIgnore.java`

Apply all fixes from the "Known Issues" section above:

1. Add `getActionUpdateThread()` returning `ActionUpdateThread.BGT`.
2. Add null check for `project` in `actionPerformed`.
3. Replace `project.getBaseDir()` with `LocalFileSystem.getInstance().findFileByNioFile(Path.of(project.getBasePath()))`.
4. Add VFS refresh after writing `.gitignore`.

### Step 3 — Update `plugin.xml`

1. Update `<idea-version since-build>` to `232`.
2. Bump `<version>` to `0.4`.
3. Add a change note for v0.4 describing the SDK modernization.

### Step 4 — Verify

Run the following Gradle tasks and confirm they succeed before marking this done:
```bash
./gradlew buildPlugin          # compiles and packages the plugin
./gradlew verifyPlugin         # runs Plugin Verifier against the target IDE version
./gradlew runIde               # launches a sandboxed IDE to smoke-test the right-click menu
```

Smoke test checklist:
- [ ] Plugin loads without exceptions in the IDE log (`Help > Show Log`)
- [ ] Right-clicking a file in Project Explorer shows "Add \<filename\> to gitignore"
- [ ] Clicking the action appends the correct entry to `.gitignore`
- [ ] Right-clicking `.gitignore` itself does NOT show the action (disabled)
- [ ] Right-clicking an External Libraries node does NOT show the action (disabled)
- [ ] `.gitignore` is created if it doesn't exist yet

---

## File Map

| Current path | New path (post-migration) |
|---|---|
| `src/PopupMenuGitIgnore.java` | `src/main/java/com/euphoricity/addtogitignore/PopupMenuGitIgnore.java` |
| `resources/META-INF/plugin.xml` | `src/main/resources/META-INF/plugin.xml` |
| `com.euphoricity.addtogitignore.iml` | deleted |
| _(new)_ | `build.gradle.kts` |
| _(new)_ | `settings.gradle.kts` |

---

## References

- [IntelliJ Platform Gradle Plugin v2 docs](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [ActionUpdateThread migration guide](https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html#action-update-thread)
- [VirtualFile API — createChildData](https://plugins.jetbrains.com/docs/intellij/virtual-file.html)
- [IntelliJ build number ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
