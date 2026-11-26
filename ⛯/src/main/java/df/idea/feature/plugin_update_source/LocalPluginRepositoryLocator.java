// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.plugin_update_source;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class LocalPluginRepositoryLocator {
	public static final String PLUGIN_ID = "df.idea";
	public static final String REPOSITORY_FILE_NAME = "updatePlugins.xml";
	private LocalPluginRepositoryLocator() {}
	public static @Nullable String discoverLocalRepositoryUrl() {
		Path artifact = locatePluginArtifact(
			LocalPluginRepositoryLocator::findLoadedPluginPath,
			LocalPluginRepositoryLocator.class
		);
		Path repositoryFile = resolveRepositoryFile(artifact);
		return null != repositoryFile ? toRepositoryUrl(repositoryFile) : null;
	}
	public static @Nullable Path locatePluginArtifact(
		@Nullable Supplier<Path> descriptorPathSupplier,
		@Nullable Class<?> anchorClass
	) {
		Path r = null;
		if (null != descriptorPathSupplier) {
			try {
				Path fromDescriptor = descriptorPathSupplier.get();
				if (null != fromDescriptor) {
					r = fromDescriptor.toAbsolutePath().normalize();
				}
			}
			catch (RuntimeException | LinkageError ignored) {
			}
		}
		if (null == r && null != anchorClass) {
			try {
				ProtectionDomain domain = anchorClass.getProtectionDomain();
				CodeSource codeSource = null != domain ? domain.getCodeSource() : null;
				URL location = null != codeSource ? codeSource.getLocation() : null;
				if (null != location) {
					URI uri = location.toURI();
					if ("file".equalsIgnoreCase(uri.getScheme())) {
						r = Path.of(uri).toAbsolutePath().normalize();
					}
				}
			}
			catch (URISyntaxException | IllegalArgumentException | SecurityException | FileSystemNotFoundException ignored) {
			}
		}
		return r;
	}
	public static @Nullable Path resolveRepositoryFile(@Nullable Path pluginLocation) {
		Path r = null;
		if (null != pluginLocation) {
			Path normalized = pluginLocation.toAbsolutePath().normalize();
			if (Files.isRegularFile(normalized)) {
				Path parent = normalized.getParent();
				if (null != parent) {
					Path candidate = parent.resolve(REPOSITORY_FILE_NAME);
					if (Files.isRegularFile(candidate)) {
						r = candidate;
					}
				}
			}
			else if (Files.isDirectory(normalized)) {
				Path inside = normalized.resolve(REPOSITORY_FILE_NAME);
				if (Files.isRegularFile(inside)) {
					r = inside;
				}
				else {
					Path parent = normalized.getParent();
					if (null != parent) {
						Path sibling = parent.resolve(REPOSITORY_FILE_NAME);
						if (Files.isRegularFile(sibling)) {
							r = sibling;
						}
					}
				}
			}
		}
		return r;
	}
	public static @NotNull String toRepositoryUrl(@NotNull Path repositoryFile) {
		return repositoryFile.toAbsolutePath().normalize().toUri().toString();
	}
	private static @Nullable Path findLoadedPluginPath() {
		IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
		return null != descriptor ? descriptor.getPluginPath() : null;
	}
}
