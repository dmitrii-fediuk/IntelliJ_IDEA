// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.plugin_update_source;
import com.intellij.openapi.extensions.PluginId;
import df.idea.support.Assertions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public final class LocalUpdateSourceRegistrarTest {
	private static final Assertions A = new Assertions();
	public static void main(String[] args) throws Exception {
		Path tempRoot = Files.createTempDirectory("df-plugin-update-source-registrar-");
		try {
			testReconcileGuardsAndLifecycle(tempRoot);
			testReflectiveBridgeAcross2026_3And2026_2_5(tempRoot);
			testMultiInstanceIsolation(tempRoot);
		}
		finally {
			deleteRecursively(tempRoot);
		}
		System.out.println("REGISTRAR_UPDATE_SOURCE_TESTS_PASSED (" + A.passedCount() + " assertions)");
	}
	private static void deleteRecursively(Path root) throws IOException {
		if (Files.exists(root)) {
			try (Stream<Path> walk = Files.walk(root)) {
				for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(path);
				}
			}
		}
	}
	private static void testMultiInstanceIsolation(Path tempRoot) throws Exception {
		Path xml = tempRoot.resolve("multi").resolve("updatePlugins.xml");
		Files.createDirectories(xml.getParent());
		Files.writeString(xml, "<plugins/>", StandardCharsets.UTF_8);
		String url = LocalPluginRepositoryLocator.toRepositoryUrl(xml);
		Simulated2026_3Service.supported = true;
		Simulated2026_3ServiceImpl activeInstanceService = new Simulated2026_3ServiceImpl();
		Simulated2026_3ServiceImpl siblingInstanceService = new Simulated2026_3ServiceImpl();
		siblingInstanceService.persisted.put("com.jetbrains.smarty", new Simulated2026_3SourceImpl("https://plugins.jetbrains.com/pluginManager"));
		PluginUpdateSourceClient activeClient = PluginUpdateSourceClient.Reflective.tryBind(
			LocalUpdateSourceRegistrarTest.class.getClassLoader(),
			Simulated2026_3Service.class.getName(),
			cls -> activeInstanceService
		);
		A.assertEquals("active instance updates its own service", LocalUpdateSourceRegistrar.Status.REGISTERED, LocalUpdateSourceRegistrar.reconcile(activeClient, url));
		A.assertEquals("active instance now contains df.idea", url, activeInstanceService.persisted.get("df.idea").getPresentableName());
		A.assertEquals("sibling instance does NOT receive df.idea", null, siblingInstanceService.persisted.get("df.idea"));
		A.assertEquals("sibling instance write count remained 0", 0, siblingInstanceService.writeCount);
		A.assertEquals("sibling instance existing entries untouched", 1, siblingInstanceService.persisted.size());
	}
	private static void testReconcileGuardsAndLifecycle(Path tempRoot) throws Exception {
		Path existingXml = tempRoot.resolve("current").resolve("updatePlugins.xml");
		Files.createDirectories(existingXml.getParent());
		Files.writeString(existingXml, "<plugins/>", StandardCharsets.UTF_8);
		String currentUrl = LocalPluginRepositoryLocator.toRepositoryUrl(existingXml);
		FakeClient client = new FakeClient(true);
		A.assertEquals("null repository URL returns REPOSITORY_MISSING", LocalUpdateSourceRegistrar.Status.REPOSITORY_MISSING, LocalUpdateSourceRegistrar.reconcile(client, null));
		A.assertEquals("blank repository URL returns REPOSITORY_MISSING", LocalUpdateSourceRegistrar.Status.REPOSITORY_MISSING, LocalUpdateSourceRegistrar.reconcile(client, "  "));
		A.assertEquals("null client returns API_UNAVAILABLE", LocalUpdateSourceRegistrar.Status.API_UNAVAILABLE, LocalUpdateSourceRegistrar.reconcile(null, currentUrl));
		FakeClient unsupported = new FakeClient(false);
		A.assertEquals("unsupported IDE returns UNSUPPORTED", LocalUpdateSourceRegistrar.Status.UNSUPPORTED, LocalUpdateSourceRegistrar.reconcile(unsupported, currentUrl));
		A.assertEquals("unsupported IDE performs zero writes", 0, unsupported.writeCount);
		A.assertEquals("missing source registers local repository", LocalUpdateSourceRegistrar.Status.REGISTERED, LocalUpdateSourceRegistrar.reconcile(client, currentUrl));
		A.assertEquals("stored host equals local repository URL", currentUrl, client.sources.get("df.idea"));
		A.assertEquals("exactly one write performed", 1, client.writeCount);
		A.assertEquals("already matching source returns ALREADY_CONFIGURED", LocalUpdateSourceRegistrar.Status.ALREADY_CONFIGURED, LocalUpdateSourceRegistrar.reconcile(client, currentUrl));
		A.assertEquals("idempotent check performs no additional write", 1, client.writeCount);
		client.sources.put("df.idea", "JetBrains Marketplace");
		A.assertEquals("explicit Marketplace choice is preserved", LocalUpdateSourceRegistrar.Status.ALREADY_CONFIGURED, LocalUpdateSourceRegistrar.reconcile(client, currentUrl));
		A.assertEquals("Marketplace host not overwritten", "JetBrains Marketplace", client.sources.get("df.idea"));
		A.assertEquals("no write when explicit Marketplace choice exists", 1, client.writeCount);
		client.sources.put("df.idea", "https://plugins.example.com/updatePlugins.xml");
		A.assertEquals("explicit HTTPS custom repo is preserved", LocalUpdateSourceRegistrar.Status.ALREADY_CONFIGURED, LocalUpdateSourceRegistrar.reconcile(client, currentUrl));
		A.assertEquals("HTTPS custom repo not overwritten", "https://plugins.example.com/updatePlugins.xml", client.sources.get("df.idea"));
		Path otherExistingXml = tempRoot.resolve("other").resolve("updatePlugins.xml");
		Files.createDirectories(otherExistingXml.getParent());
		Files.writeString(otherExistingXml, "<plugins/>", StandardCharsets.UTF_8);
		String otherExistingUrl = LocalPluginRepositoryLocator.toRepositoryUrl(otherExistingXml);
		client.sources.put("df.idea", otherExistingUrl);
		A.assertEquals("existing alternate local repository is preserved", LocalUpdateSourceRegistrar.Status.ALREADY_CONFIGURED, LocalUpdateSourceRegistrar.reconcile(client, currentUrl));
		A.assertEquals("alternate existing local host unchanged", otherExistingUrl, client.sources.get("df.idea"));
		Path deletedOldXml = tempRoot.resolve("moved-away").resolve("updatePlugins.xml");
		String staleUrl = LocalPluginRepositoryLocator.toRepositoryUrl(deletedOldXml);
		client.sources.put("df.idea", staleUrl);
		A.assertEquals("stale deleted local updatePlugins.xml self-heals", LocalUpdateSourceRegistrar.Status.UPDATED_STALE, LocalUpdateSourceRegistrar.reconcile(client, currentUrl));
		A.assertEquals("stale host replaced with current URL", currentUrl, client.sources.get("df.idea"));
		A.assertEquals("self-heal incremented write count", 2, client.writeCount);
		PluginUpdateSourceClient brokenClient = new PluginUpdateSourceClient() {
			@Override
			public @Nullable String getCurrentSourceHost(@NotNull PluginId pluginId) {
				return null;
			}
			@Override
			public boolean isSupported() {
				throw new NoSuchMethodError("simulated internal API break");
			}
			@Override
			public boolean matchesCustomRepository(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {
				return false;
			}
			@Override
			public void setCustomRepositorySource(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {}
		};
		A.assertEquals("LinkageError returns FAILED without throwing", LocalUpdateSourceRegistrar.Status.FAILED, LocalUpdateSourceRegistrar.reconcile(brokenClient, currentUrl));
	}
	private static void testReflectiveBridgeAcross2026_3And2026_2_5(Path tempRoot) throws Exception {
		Path xml = tempRoot.resolve("reflective").resolve("updatePlugins.xml");
		Files.createDirectories(xml.getParent());
		Files.writeString(xml, "<plugins/>", StandardCharsets.UTF_8);
		String url = LocalPluginRepositoryLocator.toRepositoryUrl(xml);
		Simulated2026_2_5Service.supported = false;
		PluginUpdateSourceClient client262Disabled = PluginUpdateSourceClient.Reflective.tryBind(
			LocalUpdateSourceRegistrarTest.class.getClassLoader(),
			Simulated2026_2_5Service.class.getName(),
			cls -> new Simulated2026_2_5Service()
		);
		A.assertTrue("2026.2.5 disabled binds to non-null UNSUPPORTED client", null != client262Disabled && !client262Disabled.isSupported());
		A.assertEquals("2026.2.5 disabled reconciles as UNSUPPORTED", LocalUpdateSourceRegistrar.Status.UNSUPPORTED, LocalUpdateSourceRegistrar.reconcile(client262Disabled, url));
		Simulated2026_2_5Service.supported = true;
		PluginUpdateSourceClient client262Enabled = PluginUpdateSourceClient.Reflective.tryBind(
			LocalUpdateSourceRegistrarTest.class.getClassLoader(),
			Simulated2026_2_5Service.class.getName(),
			cls -> new Simulated2026_2_5Service()
		);
		A.assertEquals("2026.2.5 missing 2026.3 method binds to null (API_UNAVAILABLE)", null, client262Enabled);
		A.assertEquals("reconcile returns API_UNAVAILABLE without throwing", LocalUpdateSourceRegistrar.Status.API_UNAVAILABLE, LocalUpdateSourceRegistrar.reconcile(client262Enabled, url));
		Simulated2026_3Service.supported = true;
		Simulated2026_3ServiceImpl impl = new Simulated2026_3ServiceImpl();
		PluginUpdateSourceClient client263 = PluginUpdateSourceClient.Reflective.tryBind(
			LocalUpdateSourceRegistrarTest.class.getClassLoader(),
			Simulated2026_3Service.class.getName(),
			cls -> impl
		);
		A.assertTrue("2026.3 reflective client binds cleanly", null != client263 && client263.isSupported());
		A.assertEquals("2026.3 reflective reconcile registers source", LocalUpdateSourceRegistrar.Status.REGISTERED, LocalUpdateSourceRegistrar.reconcile(client263, url));
		A.assertEquals("2026.3 stored host matches URL", url, impl.persisted.get("df.idea").getPresentableName());
		A.assertEquals("2026.3 second reflective reconcile is idempotent", LocalUpdateSourceRegistrar.Status.ALREADY_CONFIGURED, LocalUpdateSourceRegistrar.reconcile(client263, url));
		A.assertEquals("2026.3 write count remained 1", 1, impl.writeCount);
	}
	public interface Simulated2026_3Source {
		boolean canInstallUpdatesFrom(@NotNull Simulated2026_3Source other);
		@NotNull String getPresentableName();
	}
	public static final class Simulated2026_2_5Service {
		static boolean supported = false;
		public @Nullable Object getPluginUpdateSourceId(@NotNull PluginId pluginId) {
			return null;
		}
		public static boolean isFunctionalitySupported() {
			return supported;
		}
	}
	public static abstract class Simulated2026_3Service {
		static boolean supported = true;
		public abstract @NotNull Simulated2026_3Source createCustomRepositoryPluginUpdateSourceId(@NotNull String host);
		public abstract @Nullable Simulated2026_3Source getPluginUpdateSourceId(@NotNull PluginId pluginId);
		public static boolean isFunctionalitySupported() {
			return supported;
		}
		public abstract void setPluginUpdateSourceId(@NotNull PluginId pluginId, @NotNull Simulated2026_3Source source);
	}
	private static final class FakeClient implements PluginUpdateSourceClient {
		final boolean supported;
		final Map<String, String> sources = new HashMap<>();
		int writeCount = 0;
		FakeClient(boolean supported) {
			this.supported = supported;
		}
		@Override
		public @Nullable String getCurrentSourceHost(@NotNull PluginId pluginId) {
			return sources.get(pluginId.getIdString());
		}
		@Override
		public boolean isSupported() {
			return supported;
		}
		@Override
		public boolean matchesCustomRepository(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {
			return repositoryUrl.equals(sources.get(pluginId.getIdString()));
		}
		@Override
		public void setCustomRepositorySource(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {
			sources.put(pluginId.getIdString(), repositoryUrl);
			writeCount++;
		}
	}
	private static final class Simulated2026_3ServiceImpl extends Simulated2026_3Service {
		final Map<String, Simulated2026_3Source> persisted = new HashMap<>();
		int writeCount = 0;
		@Override
		public @NotNull Simulated2026_3Source createCustomRepositoryPluginUpdateSourceId(@NotNull String host) {
			return new Simulated2026_3SourceImpl(host);
		}
		@Override
		public @Nullable Simulated2026_3Source getPluginUpdateSourceId(@NotNull PluginId pluginId) {
			return persisted.get(pluginId.getIdString());
		}
		@Override
		public void setPluginUpdateSourceId(@NotNull PluginId pluginId, @NotNull Simulated2026_3Source source) {
			persisted.put(pluginId.getIdString(), source);
			writeCount++;
		}
	}
	private static final class Simulated2026_3SourceImpl implements Simulated2026_3Source {
		private final String host;
		Simulated2026_3SourceImpl(String host) {
			this.host = host;
		}
		@Override
		public boolean canInstallUpdatesFrom(@NotNull Simulated2026_3Source other) {
			return host.equals(other.getPresentableName());
		}
		@Override
		public @NotNull String getPresentableName() {
			return host;
		}
	}
}
