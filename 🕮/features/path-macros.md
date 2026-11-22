# `Project Path Macros` Feature (`df.idea.feature.pathmacros`)

Provides project-scoped path variables for `.iml` and project XML files (`php.xml`, `jsonSchemas.xml`, `modules.xml`, `workspace.xml`) from UTF-8 `.properties` files stored inside `.idea` directories, avoiding global entries in `path.macros.xml`.  
Part of the unified [«Dmitrii Fediuk»](../.md) (`df.idea`) plugin.

## 1. Discovering `.properties` Files

[MacroPropertiesLocator.java](../../⛯/src/main/java/df/idea/feature/pathmacros/MacroPropertiesLocator.java) receives `projectFilePath` from `IntelliJ IDEA` (e.g., `.../.idea/example.com--1/.idea/misc.xml` or `.../.idea/misc.xml`) and collects candidate `.properties` files across all ancestor directories named `.idea`, ordered **from outermost to innermost**:

1. Standard directory project (`<project>/.idea/misc.xml`) — reads `<project>/.idea/.properties`.  
1. Multi-project container (`<workspace>/.idea/example.com--1/.idea/misc.xml`) — reads the shared `<workspace>/.idea/.properties` first, then (if present) the local `<workspace>/.idea/example.com--1/.idea/.properties`.  
1. Multi-level container (`<workspace>/.idea/1/sub/.idea/misc.xml`) — reads the shared `<workspace>/.idea/.properties` first, then the nested `<workspace>/.idea/1/sub/.idea/.properties`.  
1. File-based `*.ipr` project — reads a sibling `.properties` in the `.ipr` directory along with any ancestor `.idea/.properties`.

A closer `.properties` file inherits variables from outer files, can override them, or can unset an inherited variable with an empty value (`name=`).  
Relative paths are always resolved relative to the directory of the `.properties` file where the variable is declared.

## 2. File Format and Custom Variables

[MacroPropertiesParser.java](../../⛯/src/main/java/df/idea/feature/pathmacros/MacroPropertiesParser.java) and [MacroPathResolver.java](../../⛯/src/main/java/df/idea/feature/pathmacros/MacroPathResolver.java) support an arbitrary number of custom path variables:

```properties
# Comments start with # or !
df.w=../Ⰿ/≻/♕/⛯/𝐖
df.m=${df.w}/example.com/2026-10-26/⛯
df.v=$df.m$/vendor
df.ext=C:\work\shared\lib
```

1. Encoding is UTF-8 (with automatic BOM removal when present), including supplementary Unicode characters outside the BMP (`𝐖`, `𝐏`).  
1. Key and value are split on the first `=`; surrounding whitespace is trimmed while internal spaces and `#` characters inside paths are preserved.  
1. Backslashes `\` are treated literally (unlike `java.util.Properties.load`), so Windows paths such as `C:\work\...` and `..\dir` are never corrupted by escape sequences and are normalized to `/`.  
1. Valid variable names match `[A-Za-z0-9_.-]+`, disallow `..`, and disallow reserved IDE system macros (`PROJECT_DIR`, `PROJECT_NAME`, `MODULE_DIR`, `MODULE_WORKING_DIR`, `USER_HOME`, `APPLICATION_HOME_DIR`, `APPLICATION_CONFIG_DIR`, `APPLICATION_PLUGINS_DIR`); this matches `PathMacrosCollector.MACRO_PATTERN` (`\$([\w\-.]+?)\$`).  
1. Cross-variable references are supported in both `${name}` and `$name$` syntax, regardless of declaration order and across outer/inner `.idea/.properties` layers, with cycle detection and undefined-reference guards (invalid entries log a warning and are skipped without breaking valid variables).

## 3. Architecture and Upgrade Resilience

1. [ProjectPropertiesPathMacroContributor.java](../../⛯/src/main/java/df/idea/feature/pathmacros/ProjectPropertiesPathMacroContributor.java) implements only the single abstract method `ProjectWidePathMacroContributor.getProjectPathMacros(String)` and avoids calling internal platform static helpers (such as `ProjectWidePathMacroContributorKt.getAllMacros`).  
1. [MacroPathResolver.java](../../⛯/src/main/java/df/idea/feature/pathmacros/MacroPathResolver.java) caches resolved maps by `projectFilePath` and invalidates entries based on existence, `lastModifiedTime`, and byte size of every candidate `.properties` file.  
1. [ProjectPathMacroCacheInvalidator.java](../../⛯/src/main/java/df/idea/feature/pathmacros/ProjectPathMacroCacheInvalidator.java) subscribes to the public `ProjectCloseListener` (`<applicationListeners>`) and evicts cached entries when a project closes.

## 4. Known `IntelliJ IDEA` Platform Constraints

1. **Reopening a project after editing `.properties`**: `PathMacroManager.getReplacePathMap()` caches the reverse macro replacement map for XML saves against the global `PathMacros.getModificationCount()` counter, so reopen the project after adding or changing a variable to ensure UI saves collapse paths using the updated variable.  
1. **Collapse priority in `ReplacePathToMacroMap`**: custom and plugin macros receive index `2` (`weight = 2 * 512 + len(path)`), relative `..` paths (`$MODULE_DIR$/../..`, `$PROJECT_DIR$/../..`) receive index `1`, and direct `$MODULE_DIR$`/`$PROJECT_DIR$` paths without `..` receive index `3`.  
   As a result, `$df.m$` reliably beats `$MODULE_DIR$/../..` and `$PROJECT_DIR$/../..` (and among nested custom variables, the longest matching path wins on save), whereas a path strictly inside `$PROJECT_DIR$` or `$MODULE_DIR$` without `..` still collapses to `$PROJECT_DIR$` or `$MODULE_DIR$`.  
1. **External JPS build process**: the external JPS build runner does not query the `projectPathMacroContributor` extension point.  
1. **IDE instances without the plugin**: add `.idea/externalDependencies.xml` with `<plugin id="df.idea" />` to warn when opening the project in an IDE installation that lacks the plugin.

## 5. Configuration Example

`<workspace>/.idea/.properties`:

```properties
df.m=../Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯
```

Usage in `Magento.iml`:

```xml
	<content url="file://$df.m$">
		<excludeFolder url="file://$df.m$/pub/media" />
		<excludeFolder url="file://$df.m$/pub/opt" />
		<excludeFolder url="file://$df.m$/pub/static" />
		<excludeFolder url="file://$df.m$/var" />
		<excludeFolder url="file://$df.m$/vendor/magento/magento2-base" />
		<sourceFolder url="file://$df.m$/var" isTestSource="false" />
	</content>
```

Usage in `.idea/php.xml` and `.idea/jsonSchemas.xml`: `$df.m$/vendor/bin/phpmd`, `file://$df.m$/composer.json`.

## 6. Tests

1. [MacroPropertiesLocatorTest.java](../../⛯/src/test/java/df/idea/feature/pathmacros/MacroPropertiesLocatorTest.java) — `7` assertions covering ancestor `.idea/.properties` discovery.  
1. [MacroPropertiesParserTest.java](../../⛯/src/test/java/df/idea/feature/pathmacros/MacroPropertiesParserTest.java) — `13` assertions covering UTF-8, BOM, backslashes, and name validation.  
1. [MacroPathResolverTest.java](../../⛯/src/test/java/df/idea/feature/pathmacros/MacroPathResolverTest.java) — `23` assertions covering path resolution, interpolation, layering, cycles, and cache invalidation.  
1. [IdePathMacroIntegrationTest.java](../../⛯/src/test/java/df/idea/feature/pathmacros/IdePathMacroIntegrationTest.java) — `20` integration assertions against actual `ExpandMacroToPathMap`, `ReplacePathToMacroMap`, `PathMacroManager`, `PathMacrosCollector`, and `JDOMUtil` classes.
