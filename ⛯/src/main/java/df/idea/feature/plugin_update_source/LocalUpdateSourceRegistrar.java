// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.plugin_update_source;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class LocalUpdateSourceRegistrar implements AppLifecycleListener {
	private static final Logger LOG = Logger.getInstance(LocalUpdateSourceRegistrar.class);
	public enum Status {
		REGISTERED,
		UPDATED_STALE,
		ALREADY_CONFIGURED,
		UNSUPPORTED,
		REPOSITORY_MISSING,
		API_UNAVAILABLE,
		FAILED
	}
	/**
	 * - @used-by META-INF/plugin.xml
	 * - @used-by {@link AppLifecycleListener#appFrameCreated(List)}
	 * @see AppLifecycleListener#appFrameCreated(List)
	 */
	@Override
	public void appFrameCreated(@NotNull List<String> commandLineArgs) {
		Application app = ApplicationManager.getApplication();
		if (null != app && !app.isCommandLine() && !app.isHeadlessEnvironment() && !app.isUnitTestMode()) {
			try {
				String repositoryUrl = LocalPluginRepositoryLocator.discoverLocalRepositoryUrl();
				if (null == repositoryUrl) {
					LOG.warn(
						"Sibling " + LocalPluginRepositoryLocator.REPOSITORY_FILE_NAME
						+ " not found next to df-idea.jar; skipping local update source registration."
					);
				}
				else {
					PluginUpdateSourceClient client = PluginUpdateSourceClient.Reflective.tryBind(app);
					Status status = reconcile(client, repositoryUrl);
					if (Status.REGISTERED == status || Status.UPDATED_STALE == status) {
						LOG.info(
							"Configured local plugin update source (" + status + ") for "
							+ LocalPluginRepositoryLocator.PLUGIN_ID + ": " + repositoryUrl
						);
					}
				}
			}
			catch (CancellationException e) {
				throw e;
			}
			catch (LinkageError | RuntimeException e) {
				LOG.warn("Skipped automatic local plugin update source registration due to incompatible IDE API", e);
			}
		}
	}
	/**
	 * - @used-by {@link LocalUpdateSourceRegistrar#reconcile(PluginUpdateSourceClient, String)}
	 */
	static boolean isStaleLocalRepositoryUrl(@Nullable String host) {
		boolean r = false;
		if (null != host && host.startsWith("file:")) {
			try {
				URI uri = new URI(host);
				if ("file".equalsIgnoreCase(uri.getScheme())) {
					Path path = Path.of(uri);
					Path fileName = path.getFileName();
					if (null != fileName && LocalPluginRepositoryLocator.REPOSITORY_FILE_NAME.equals(fileName.toString())) {
						r = !Files.isRegularFile(path);
					}
				}
			}
			catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException ignored) {
			}
		}
		return r;
	}
	/**
	 * - @used-by {@link IdeUpdateSourceIntegrationTest#verifyPlatformUpdateSourceContract(String)}
	 * - @used-by {@link LocalUpdateSourceRegistrar#appFrameCreated(List)}
	 * - @used-by {@link LocalUpdateSourceRegistrarTest#testMultiInstanceIsolation(Path)}
	 * - @used-by {@link LocalUpdateSourceRegistrarTest#testReconcileGuardsAndLifecycle(Path)}
	 * - @used-by {@link LocalUpdateSourceRegistrarTest#testReflectiveBridgeAcross2026_3And2026_2_5(Path)}
	 */
	static @NotNull Status reconcile(
		@Nullable PluginUpdateSourceClient client,
		@Nullable String repositoryUrl
	) {
		Status r;
		if (null == repositoryUrl || repositoryUrl.isBlank()) {
			r = Status.REPOSITORY_MISSING;
		}
		else if (null == client) {
			r = Status.API_UNAVAILABLE;
		}
		else {
			try {
				if (!client.isSupported()) {
					r = Status.UNSUPPORTED;
				}
				else {
					PluginId pluginId = PluginId.getId(LocalPluginRepositoryLocator.PLUGIN_ID);
					String currentHost = client.getCurrentSourceHost(pluginId);
					if (null == currentHost) {
						client.setCustomRepositorySource(pluginId, repositoryUrl);
						r = Status.REGISTERED;
					}
					else if (client.matchesCustomRepository(pluginId, repositoryUrl)) {
						r = Status.ALREADY_CONFIGURED;
					}
					else if (isStaleLocalRepositoryUrl(currentHost)) {
						client.setCustomRepositorySource(pluginId, repositoryUrl);
						r = Status.UPDATED_STALE;
					}
					else {
						r = Status.ALREADY_CONFIGURED;
					}
				}
			}
			catch (CancellationException e) {
				throw e;
			}
			catch (LinkageError | RuntimeException e) {
				LOG.warn("Failed to reconcile local plugin update source for " + LocalPluginRepositoryLocator.PLUGIN_ID, e);
				r = Status.FAILED;
			}
		}
		return r;
	}
}
