# `Local Update Source` Feature (`df.idea.feature.updatesource`)

Eliminates the `Missing Plugin Update Source` («Cannot update plugin») warning for the local `df.idea` plugin in `IntelliJ IDEA 2026.3+` without manually editing IDE files in `%APPDATA%` and while keeping the entire plugin self-contained inside one root directory.  
Part of the unified [«Dmitrii Fediuk»](../.md) (`df.idea`) plugin.

## 1. Problem and Design

Starting with `IntelliJ IDEA 2026.3` (`IJPL-171317`), update-source verification for non-bundled plugins (`PluginUpdateSourceService`) is enabled by default.  
Because the local `df.idea` plugin is loaded through `plugin.path` and is absent from the JetBrains Marketplace allowlist, the IDE displays the «Cannot update plugin» notification on project open and a warning in `Settings | Plugins`.

The solution keeps all repository metadata inside `≻/` and consists of three parts:

1. **Local repository generation at build time**: [build.ps1](../../⛯/build.ps1) generates a deterministic [updatePlugins.xml](../../≻/updatePlugins.xml) next to [df-idea.jar](../../≻/df-idea.jar), reading `id`, `name`, `version`, `idea-version`, `vendor`, `depends`, and `description` directly from [plugin.xml](../../⛯/src/main/resources/META-INF/plugin.xml).  
   The `url="df-idea.jar"` attribute is relative, avoiding hardcoded absolute paths in `updatePlugins.xml` while keeping its version synchronized with the built JAR.  
1. **Advertising the local repository via a public extension point**: [LocalPluginRepositoryLocator.java](../../⛯/src/main/java/df/idea/feature/updatesource/LocalPluginRepositoryLocator.java) locates the sibling `updatePlugins.xml` relative to the loaded `df-idea.jar` (`PluginManagerCore.getPlugin` with a `CodeSource` fallback) and returns its `file:///.../updatePlugins.xml` URL only when the file exists on disk.  
   [LocalPluginUpdateSettingsProvider.java](../../⛯/src/main/java/df/idea/feature/updatesource/LocalPluginUpdateSettingsProvider.java) implements the public `com.intellij.updateSettingsProvider` extension point (`UpdateSettingsProvider.getPluginRepositories()`), making the local repository visible to `RepositoryHelper`, `PluginUpdateSourceInitializer`, `UpdateChecker`, and the source picker in `Settings | Plugins`.  
1. **Self-healing registration on startup in the active IDE instance**: [LocalUpdateSourceRegistrar.java](../../⛯/src/main/java/df/idea/feature/updatesource/LocalUpdateSourceRegistrar.java) subscribes to the public `com.intellij.ide.AppLifecycleListener` (`appFrameCreated`), which runs before projects open and before `PluginUpdateSourceInitializationActivity` checks for missing update sources.  
   Through the reflective adapter [PluginUpdateSourceClient.java](../../⛯/src/main/java/df/idea/feature/updatesource/PluginUpdateSourceClient.java), it queries `PluginUpdateSourceService` of the active `Application` process:  
   - if `isFunctionalitySupported()` returns `false` (e.g., in `IntelliJ IDEA 2026.2.5`), it exits without making changes;  
   - if `df.idea` has no configured update source, it assigns `file:///.../updatePlugins.xml` via `createCustomRepositoryPluginUpdateSourceId` and `setPluginUpdateSourceId`;  
   - if the configured source already matches the current `updatePlugins.xml` or the user explicitly selected another existing source (e.g., `JetBrains Marketplace`), it leaves the setting untouched;  
   - if the saved source points to a deleted or relocated local `file:///.../updatePlugins.xml`, it automatically updates the source to the current path.

## 2. Isolation Across Side-by-Side IDE Installations

The plugin never reads or writes files under `%APPDATA%\JetBrains\...` directly and never enumerates installed `IntelliJ IDEA` versions.  
All state updates pass exclusively through `PluginUpdateSourceService` obtained from `ApplicationManager.getApplication().getService(...)` inside the running JVM:

1. `PluginUpdateSourceServiceImpl` is annotated with `@State(name = "PluginUpdateSources", storages = {@Storage(value = "pluginUpdateSources.xml", roamingType = RoamingType.DISABLED)})`, so the IDE's own `ComponentStore` writes strictly to `PathManager.getOptionsDir()` of the active instance (`-Didea.paths.selector`).  
1. Running `IntelliJ IDEA 2026.3` updates only `%APPDATA%\JetBrains\IntelliJIdea2026.3\options\pluginUpdateSources.xml`; `IntelliJIdea2026.2` remains untouched.  
1. Running `IntelliJ IDEA 2026.2.5` receives `false` from `PluginUpdateSourceService.isFunctionalitySupported()` and exits cleanly with zero writes.  
1. Reflective binding in [PluginUpdateSourceClient.java](../../⛯/src/main/java/df/idea/feature/updatesource/PluginUpdateSourceClient.java) eliminates compile-time coupling to `@ApiStatus.Internal` interfaces that differ between `2026.2.5` (`PluginUpdateSourceId`) and `2026.3` (`PluginUpdateSource`): if the internal API changes in a future IDE release, registration degrades silently to `API_UNAVAILABLE` without throwing `LinkageError`.

## 3. Security (`IJPL-171317`) and Limitations

1. The registered update source sets `isMarketplace="false"`, so `Repository.canInstallUpdatesFrom` rejects updates from JetBrains Marketplace and preserves `IJPL-171317` protection against plugin ID spoofing for `df.idea`.  
1. The local `updatePlugins.xml` always declares the same `<version>` as `df-idea.jar`; when bumping `<version>` in [plugin.xml](../../⛯/src/main/resources/META-INF/plugin.xml), rebuild the plugin while the IDE is closed so the IDE does not offer to copy an "update" JAR into `%APPDATA%\...\plugins`.  
1. Because `PluginUpdateSourceService` is an internal API, a signature change in a future IDE release would disable automatic startup registration, while `LocalPluginUpdateSettingsProvider` continues to advertise the local repository through the public `com.intellij.updateSettingsProvider` extension point.

## 4. Tests

1. [LocalPluginRepositoryLocatorTest.java](../../⛯/src/test/java/df/idea/feature/updatesource/LocalPluginRepositoryLocatorTest.java) — `15` assertions covering sibling `updatePlugins.xml` lookup in paths with spaces and non-BMP characters (`𝐏`), `file:///` encoding, relative `df-idea.jar` resolution, and missing files.  
1. [LocalUpdateSourceRegistrarTest.java](../../⛯/src/test/java/df/idea/feature/updatesource/LocalUpdateSourceRegistrarTest.java) — `35` assertions covering every `reconcile` branch, reflective binding for `2026.3` and `2026.2.5`, `LinkageError` handling, and multi-instance isolation.  
1. [IdeUpdateSourceIntegrationTest.java](../../⛯/src/test/java/df/idea/feature/updatesource/IdeUpdateSourceIntegrationTest.java) — end-to-end verification against actual `RepositoryHelper.loadPluginModels`, `Urls.newFromEncoded`, `XmlSerializer`, and `@State` annotations of `PluginUpdateSourceServiceImpl` on both `IntelliJ IDEA 2026.3` (`48` assertions) and `IntelliJ IDEA 2026.2.5` (`37` assertions).
