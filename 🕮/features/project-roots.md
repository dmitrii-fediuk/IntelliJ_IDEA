# `Project Roots` Feature (`df.idea.feature.projectroots`)

Automatically removes redundant nested `ProjectRootEntity` instances whose root URL is strictly nested inside any module `ContentRootEntity` URL of the current project, restoring module ownership and visibility of nested project directories in the `Project Files` view.  
Part of the unified [«Dmitrii Fediuk»](../.md) (`df.idea`) plugin.

## 1. Problem and Root Cause

When a project directory (`$PROJECT_DIR$`, e.g., `<workspace>/.idea/example.com--1`) resides inside an enclosing module's `<content>` root (`<workspace>`) and `ide.create.project.root.entity=true`, `IntelliJ IDEA` registers a `ProjectRootEntity` on `$PROJECT_DIR$`:

1. `ProjectRootEntityWorkspaceFileIndexContributor` registers `entity.root` with `WorkspaceFileKind.CONTENT_NON_INDEXABLE` and `customData = null` (`DummyWorkspaceFileSetData.INSTANCE`, which does not implement `ModuleRelatedRootData`).  
1. `ProjectFileIndexImpl.getModuleForFile(file, false)` queries `WorkspaceFileIndex.findFileSetWithCustomData` with `includeContentNonIndexableSets = true`; `WorkspaceFileIndexDataImpl.getFileInfo` stops at `$PROJECT_DIR$` before reaching the outer `ModuleContentRootData`, so `getModuleForFile` returns `null`.  
1. In the `Project Files` view (`ScopeViewPane`) with `Modules` enabled (`showModules=true`), `ProjectFileNode.findArea` falls back to `project`, so `ProjectFileTreeModel.FileNode.isValidChild` drops `$PROJECT_DIR$` from its parent folder under the outer module (`Module.equals(project)` is `false`).  
1. At the top level of `Project Files`, `BaseProjectDirectoriesImpl` collapses `$PROJECT_DIR$` under the outer content root via `virtualFilesTree.getRoots()`, hiding the current project directory completely.

## 2. Algorithm and Architecture

The feature is isolated in `df.idea.feature.projectroots` with zero hardcoded project names or paths and zero compile-time dependencies on `@ApiStatus.Internal` platform types:

1. **Pure URL containment invariant**: [NestedProjectRootMatcher.java](../../⛯/src/main/java/df/idea/feature/projectroots/NestedProjectRootMatcher.java) normalizes schemes, slashes, and trailing separators (preserving POSIX `/` and Windows drive roots) and checks strict descendant containment on segment boundaries (`/`), rejecting equal roots, prefix traps (`example.com-2`), and mismatched schemes while honoring OS file-system case sensitivity (`SystemInfoRt.isFileSystemCaseSensitive`).  
1. **Reflective internal API boundary**: [ProjectRootEntityAdapter.java](../../⛯/src/main/java/df/idea/feature/projectroots/ProjectRootEntityAdapter.java) loads `com.intellij.workspaceModel.ide.ProjectRootEntity` and resolves `getRoot()` via reflection at runtime; if the `@ApiStatus.Internal` entity is renamed or removed in a future IDE release, the feature degrades silently to a no-op with zero `LinkageError`.  
1. **Storage reconciliation and change filtering**: [NestedProjectRootCleaner.java](../../⛯/src/main/java/df/idea/feature/projectroots/NestedProjectRootCleaner.java) scans `EntityStorage` for any `ProjectRootEntity` strictly covered by any `ContentRootEntity.url`, skips updates when none are found, and removes matching entities atomically inside `WorkspaceModel.update(...)`.  
   Event filtering inspects only `Added` and `Replaced` changes (`getNewEntity() != null`) on `ProjectRootEntity` and `ContentRootEntity`, ignoring pure `Removed` events (preventing self-triggered feedback loops) and unrelated entity updates.  
1. **Project open and reactive observation**: [NestedProjectRootCleanupActivity.java](../../⛯/src/main/java/df/idea/feature/projectroots/NestedProjectRootCleanupActivity.java) implements the public `com.intellij.postStartupActivity` (`ProjectActivity`) extension point registered in [plugin.xml](../../⛯/src/main/resources/META-INF/plugin.xml).  
   It subscribes to `WorkspaceModel.getEventLog()` using `FlowKt.onSubscription` (with a `FlowKt.onStart` fallback for non-`SharedFlow` implementations), reconciling the current snapshot immediately upon subscription and catching any subsequent `ProjectRootEntity` registration (such as `ProjectRootsSynchronizer.doRegister`) or late JPS module loading (`ContentRootEntity` additions).

## 3. Effects and Invariants

1. Removing a redundant nested `ProjectRootEntity` synchronously removes its `CONTENT_NON_INDEXABLE` entry from `WorkspaceFileIndexDataImpl`, restores `getModuleForFile` to the enclosing module, triggers `ModuleRootListener.rootsChanged` through `ModuleRootListenerBridgeImpl` (refreshing `ProjectFileTreeModel`), clears `<component name="ProjectRoots">` via `ProjectRootsSynchronizer`, and persists the clean storage to `project-model-cache` on close.  
1. Moduleless projects (zero `ContentRootEntity`), standard projects where `$PROJECT_DIR$` equals the module content root, and disjoint attached project roots remain completely untouched.  
1. Works identically in `IntelliJ IDEA 2026.2.5`, `IntelliJ IDEA 2026.3`, and future builds regardless of whether the `ide.create.project.root.entity` registry key is present.

## 4. Tests

1. [NestedProjectRootMatcherTest.java](../../⛯/src/test/java/df/idea/feature/projectroots/NestedProjectRootMatcherTest.java) — `37` assertions covering URL normalization, trailing slashes, POSIX and Windows drive roots, prefix traps, scheme isolation, case sensitivity, spaces, and non-BMP Unicode paths.  
1. [NestedProjectRootCleanerTest.java](../../⛯/src/test/java/df/idea/feature/projectroots/NestedProjectRootCleanerTest.java) — `24` assertions covering reflective adapter degradation, disposed/null guards, `Added`/`Removed` event filtering, plain `Flow` fallback, transient error resilience, and coroutine cancellation.  
1. [IdeProjectRootIntegrationTest.java](../../⛯/src/test/java/df/idea/feature/projectroots/IdeProjectRootIntegrationTest.java) — `30` end-to-end integration assertions against actual `MutableEntityStorage`, `VirtualFileUrlManagerImpl`, `ModuleEntityModifications`, `ContentRootEntityModifications`, `ProjectRootEntityModifications`, `ProjectRootEntityWorkspaceFileIndexContributor`, `ProjectRootsSynchronizer`, and `MutableSharedFlow`, plus bytecode verification confirming zero compile-time references to `@ApiStatus.Internal` `ProjectRootEntity` in production classes.
