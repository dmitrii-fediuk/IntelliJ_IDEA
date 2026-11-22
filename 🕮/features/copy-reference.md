# `Copy Reference` Feature (`df.idea.feature.copyreference`)

Fixes path resolution for `CopyReference` (`Ctrl+Q`) inside nested module content roots when an outer wrapper module declares `<excludeFolder>` over the nested directory.  
Part of the unified [«Dmitrii Fediuk»](../.md) (`df.idea`) plugin.

## 1. Problem and Algorithm

When no `sourceRoot` contains the file, the stock `JavaVirtualFileQualifiedNameProvider` walks upward through parent directories using `ProjectFileIndex.getContentRootForFile(each, false)` (`honorExclusion = false`).  
For a nested module (e.g., a Magento codebase under `Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯`) whose ancestor directory is excluded by an outer wrapper module (`<excludeFolder url="file://$MODULE_DIR$/Ⰿ/≻/♕/⛯/𝐖" />`), that stock loop ignores the exclusion and prepends the outer wrapper prefix to `CopyReference` output.

[ExclusionAwareVirtualFileQualifiedNameProvider.java](../../⛯/src/main/java/df/idea/feature/copyreference/ExclusionAwareVirtualFileQualifiedNameProvider.java) registers on the `com.intellij.virtualFileQualifiedNameProvider` extension point in [plugin.xml](../../⛯/src/main/resources/META-INF/plugin.xml) with `order="first"`:

1. Unwraps injected fragments (`VirtualFileWindow`) via `getDelegate()`.  
1. Checks module ownership via `index.getModuleForFile(targetFile, false)`; for external files, libraries, and scratch files (`module == null`), returns `null` so the standard `FqnUtil.getVirtualFileFqn` chain takes over.  
1. If the file resides inside a `sourceRoot`, returns the path relative to that `sourceRoot`.  
1. Finds the file's nearest content root with `honorExclusion = false`, so files inside excluded subfolders of the module itself (e.g., `var/`, `pub/static/`, `vendor/magento/magento2-base/`) still resolve relative to the module root.  
1. Walks upward with `honorExclusion = true` (`index.getContentRootForFile(parent, true)`) and checks `ModuleRootManager.getInstance(parentModule).getExcludeRoots()`, stopping at any `<excludeFolder>` boundary while preserving standard upward traversal for ordinary unexcluded nested modules (e.g., Maven or Gradle subprojects).  
1. Returns the relative path via `VfsUtilCore.getRelativePath(..., '/')`, or `targetFile.getPath()` when the target is the content root directory itself.

## 2. Platform Compatibility

1. Uses strictly public, non-deprecated, non-`@ApiStatus.Internal` platform APIs (`VirtualFileQualifiedNameProvider`, `ProjectFileIndex`, `ModuleRootManager`, `VirtualFile`, `VfsUtilCore`, `VirtualFileWindow`).  
1. Works across all projects and module types without per-project configuration.  
1. Delegates through the standard `CopyReference` action, preserving FQN resolution for classes and methods, `file:line` references, directories, and multi-selection copies.

## 3. Tests

Unit tests (`15` assertions) reside in [ExclusionAwareVirtualFileQualifiedNameProviderTest.java](../../⛯/src/test/java/df/idea/feature/copyreference/ExclusionAwareVirtualFileQualifiedNameProviderTest.java).
