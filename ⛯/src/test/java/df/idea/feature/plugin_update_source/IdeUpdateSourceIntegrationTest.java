// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.plugin_update_source;
import com.intellij.configurationStore.XmlSerializer;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.ide.plugins.newui.PluginNodeModelBuilderFactory;
import com.intellij.ide.plugins.newui.PluginUiModel;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import df.idea.support.Assertions;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jdom.Element;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class IdeUpdateSourceIntegrationTest {
	private static final Assertions A = new Assertions();
	/**
	 * - @used-by build.ps1
	 */
	public static void main(String[] args) throws Exception {
		if (1 > args.length) {
			throw new IllegalArgumentException("Usage: IdeUpdateSourceIntegrationTest <path-to-staged-df-idea.jar>");
		}
		Path stagedJar = Path.of(args[0]).toAbsolutePath().normalize();
		Path stagedXml = LocalPluginRepositoryLocator.resolveRepositoryFile(stagedJar);
		A.assertTrue("staged updatePlugins.xml exists next to staged df-idea.jar", null != stagedXml && Files.isRegularFile(stagedXml));
		// 2026-11-22
		// "Standardize df.idea feature package names with snake_case word separators":
		// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
		Path tempRoot = Files.createTempDirectory("df-plugin-update-source-ide-");
		try {
			Path nonBmpOut = tempRoot
				.resolve("IntelliJ IDEA")
				.resolve("\u2C0F")
				.resolve("\u227B")
				.resolve("\u2655")
				.resolve("\u26EF")
				.resolve("\uD835\uDC0F")
				.resolve("\u227B");
			Files.createDirectories(nonBmpOut);
			Path nonBmpJar = nonBmpOut.resolve("df-idea.jar");
			Path nonBmpXml = nonBmpOut.resolve("updatePlugins.xml");
			Files.copy(stagedJar, nonBmpJar);
			Files.copy(stagedXml, nonBmpXml);
			Path resolved = LocalPluginRepositoryLocator.resolveRepositoryFile(nonBmpJar);
			A.assertTrue("resolved non-BMP updatePlugins.xml", null != resolved && Files.isSameFile(nonBmpXml, resolved));
			String repoUrl = LocalPluginRepositoryLocator.toRepositoryUrl(resolved);
			Url ijUrl = Urls.newFromEncoded(repoUrl);
			A.assertEquals("Urls.newFromEncoded scheme is file", "file", ijUrl.getScheme());
			A.assertTrue("Url.toExternalForm is ASCII file:/// URL", ijUrl.toExternalForm().startsWith("file:///"));
			URI resolvedJarUri = new URI(repoUrl).resolve(new URI("df-idea.jar"));
			A.assertTrue("PluginDownloader relative URL resolves to sibling df-idea.jar", Files.isSameFile(nonBmpJar, Path.of(resolvedJarUri)));
			for (String buildStr : new String[]{"262.12573.28", "263.7307.200", "271.1.0"}) {
				List<PluginUiModel> models = RepositoryHelper.loadPluginModels(
					repoUrl,
					BuildNumber.fromString(buildStr),
					null,
					PluginNodeModelBuilderFactory.INSTANCE
				);
				A.assertEquals("RepositoryHelper loaded 1 plugin for build " + buildStr, 1, models.size());
				PluginUiModel model = models.get(0);
				A.assertEquals("pluginId for " + buildStr, "df.idea", model.getPluginId().getIdString());
				A.assertEquals("name for " + buildStr, "Dmitrii Fediuk", model.getName());
				A.assertEquals("version for " + buildStr, "1.0.0", model.getVersion());
				A.assertEquals("downloadUrl for " + buildStr, "df-idea.jar", model.getDownloadUrl());
				A.assertEquals("repositoryName for " + buildStr, repoUrl, model.getRepositoryName());
				A.assertEquals("sinceBuild for " + buildStr, "262", model.getDescriptor().getSinceBuild());
				A.assertEquals("untilBuild open-ended for " + buildStr, null, model.getDescriptor().getUntilBuild());
			}
			verifyPlatformUpdateSourceContract(repoUrl);
		}
		finally {
			deleteRecursively(tempRoot);
		}
		System.out.println("IDE_UPDATE_SOURCE_INTEGRATION_PASSED (" + A.passedCount() + " assertions)");
	}
	/**
	 * - @used-by {@link IdeUpdateSourceIntegrationTest#main(String[])}
	 */
	private static void deleteRecursively(Path root) throws IOException {
		if (Files.exists(root)) {
			try (Stream<Path> walk = Files.walk(root)) {
				for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(path);
				}
			}
		}
	}
	/**
	 * - @used-by {@link IdeUpdateSourceIntegrationTest#main(String[])}
	 */
	private static void verifyPlatformUpdateSourceContract(String repoUrl) throws Exception {
		ClassLoader cl = IdeUpdateSourceIntegrationTest.class.getClassLoader();
		Class<?> serviceClass = Class.forName(PluginUpdateSourceClient.Reflective.SERVICE_CLASS_NAME, false, cl);
		Method isSupportedMethod = serviceClass.getMethod("isFunctionalitySupported");
		A.assertEquals("PluginUpdateSourceService.isFunctionalitySupported returns boolean", boolean.class, isSupportedMethod.getReturnType());
		Class<?> implClass = Class.forName("com.intellij.openapi.updateSettings.impl.PluginUpdateSourceServiceImpl", false, cl);
		State stateAnn = implClass.getAnnotation(State.class);
		A.assertTrue("PluginUpdateSourceServiceImpl has @State annotation", null != stateAnn);
		A.assertEquals("@State name is PluginUpdateSources", "PluginUpdateSources", stateAnn.name());
		Storage[] storages = stateAnn.storages();
		A.assertEquals("@State declares 1 storage", 1, storages.length);
		A.assertEquals("@Storage value is relative pluginUpdateSources.xml (bound to active instance options dir)", "pluginUpdateSources.xml", storages[0].value());
		A.assertEquals("@Storage roamingType is DISABLED", RoamingType.DISABLED, storages[0].roamingType());
		boolean has2026_3Api = false;
		try {
			serviceClass.getMethod("createCustomRepositoryPluginUpdateSourceId", String.class);
			has2026_3Api = true;
		}
		catch (NoSuchMethodException ignored) {
		}
		if (!has2026_3Api) {
			System.out.println("DETECTED_IDE_UPDATE_SOURCE_API=2026.2.5");
			Class<?> legacySourceType = Class.forName("com.intellij.openapi.updateSettings.impl.PluginUpdateSourceId", false, cl);
			Method legacyGet = serviceClass.getMethod("getPluginUpdateSourceId", PluginId.class);
			A.assertEquals("2026.2.5 getPluginUpdateSourceId returns PluginUpdateSourceId", legacySourceType, legacyGet.getReturnType());
			PluginUpdateSourceClient bound = PluginUpdateSourceClient.Reflective.tryBind(
				cl,
				PluginUpdateSourceClient.Reflective.SERVICE_CLASS_NAME,
				cls -> null
			);
			LocalUpdateSourceRegistrar.Status status = LocalUpdateSourceRegistrar.reconcile(bound, repoUrl);
			A.assertTrue(
				"2026.2.5 reconcile gracefully degrades to UNSUPPORTED or API_UNAVAILABLE (got " + status + ")",
				LocalUpdateSourceRegistrar.Status.UNSUPPORTED == status
				|| LocalUpdateSourceRegistrar.Status.API_UNAVAILABLE == status
			);
		}
		else {
			System.out.println("DETECTED_IDE_UPDATE_SOURCE_API=2026.3+");
			Method getMethod = serviceClass.getMethod("getPluginUpdateSourceId", PluginId.class);
			Method createCustom = serviceClass.getMethod("createCustomRepositoryPluginUpdateSourceId", String.class);
			Class<?> sourceType = createCustom.getReturnType();
			Method setMethod = serviceClass.getMethod("setPluginUpdateSourceId", PluginId.class, sourceType);
			Method presentable = sourceType.getMethod("getPresentableName");
			Method canInstall = sourceType.getMethod("canInstallUpdatesFrom", sourceType);
			A.assertEquals("getPluginUpdateSourceId returns PluginUpdateSource", sourceType, getMethod.getReturnType());
			A.assertEquals("setPluginUpdateSourceId returns void", void.class, setMethod.getReturnType());
			A.assertEquals("getPresentableName returns String", String.class, presentable.getReturnType());
			A.assertEquals("canInstallUpdatesFrom returns boolean", boolean.class, canInstall.getReturnType());
			Class<?> xmlRepoClass = Class.forName("com.intellij.openapi.updateSettings.impl.XmlSerializableRepository", false, cl);
			Constructor<?> xmlRepoCtor = xmlRepoClass.getConstructor(String.class, boolean.class, boolean.class);
			Object localXmlRepo = xmlRepoCtor.newInstance(repoUrl, false, false);
			Object mpXmlRepo = xmlRepoCtor.newInstance("https://plugins.jetbrains.com/pluginManager", true, false);
			Class<?> stateClass = Class.forName("com.intellij.openapi.updateSettings.impl.PluginUpdateSourceServiceImpl$State", false, cl);
			Constructor<?> stateCtor = stateClass.getConstructor(Map.class);
			Object stateObj = stateCtor.newInstance(Map.of("df.idea", localXmlRepo));
			Element serialized = XmlSerializer.serialize(stateObj);
			String xmlText = JDOMUtil.writeElement(serialized);
			A.assertTrue("serialized XML contains df.idea", xmlText.contains("df.idea"));
			A.assertTrue("serialized XML contains readable non-BMP 𝐏", xmlText.contains("\uD835\uDC0F"));
			A.assertTrue("serialized XML sets isMarketplace=\"false\"", xmlText.contains("isMarketplace=\"false\""));
			Object deserialized = XmlSerializer.deserialize(JDOMUtil.load(xmlText), stateClass);
			Field sourcesField = stateClass.getField("sources");
			@SuppressWarnings("unchecked")
			Map<String, Object> loadedMap = (Map<String, Object>) sourcesField.get(deserialized);
			Object loadedRepo = loadedMap.get("df.idea");
			A.assertTrue("deserialized state contains df.idea", null != loadedRepo);
			A.assertEquals("deserialized host round-trips identically", repoUrl, xmlRepoClass.getField("hostToSerialize").get(loadedRepo));
			A.assertEquals("deserialized isMarketplace is false", false, xmlRepoClass.getField("isMarketplaceToSerialize").get(loadedRepo));
			Method toSource = xmlRepoClass.getMethod("toPluginSourceId");
			Object localSource = toSource.invoke(loadedRepo);
			Object mpSource = toSource.invoke(mpXmlRepo);
			A.assertEquals("local source allows updates from itself", true, canInstall.invoke(localSource, localSource));
			A.assertEquals("local source blocks updates from JetBrains Marketplace (IJPL-171317)", false, canInstall.invoke(localSource, mpSource));
			A.assertEquals("presentableName equals local repository URL", repoUrl, presentable.invoke(localSource));
		}
	}
}
