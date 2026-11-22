// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
// "Suppress Markdown code fence background renderer in IntelliJ IDEA 2026.3+":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/3
package df.idea.plugin;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.actions.VirtualFileQualifiedNameProvider;
import com.intellij.ide.plugins.DiscoveredPluginsList;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginCompatibilityUtils;
import com.intellij.ide.plugins.PluginDependency;
import com.intellij.ide.plugins.PluginDescriptorLoader;
import com.intellij.ide.plugins.PluginDescriptorLoadingContext;
import com.intellij.ide.plugins.PluginMainDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.PluginsSourceContext;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.ide.plugins.newui.PluginNodeModelBuilderFactory;
import com.intellij.ide.plugins.newui.PluginUiModel;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.impl.ProjectWidePathMacroContributor;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.extensions.ExtensionDescriptor;
import com.intellij.openapi.extensions.LoadingOrder;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.updateSettings.impl.UpdateSettingsProvider;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.util.lang.ZipEntryResolverPool;
import com.intellij.util.messages.ListenerDescriptor;
import df.idea.feature.updatesource.LocalPluginRepositoryLocator;
import df.idea.support.Assertions;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Deferred;
public final class PluginDescriptorVerificationTest {
	private static final String PLUGIN_ID = "df.idea";
	private static final String PLUGIN_NAME = "Dmitrii Fediuk";
	private static final String VENDOR_URL = "https://github.com/dmitrii-fediuk";
	private static final String COPY_REF_IMPL = "df.idea.feature.copyreference.ExclusionAwareVirtualFileQualifiedNameProvider";
	private static final String MARKDOWN_CODE_FENCE_IMPL = "df.idea.feature.markdowncodefence.MarkdownCodeFenceBackgroundEditorListener";
	private static final String PATH_MACROS_IMPL = "df.idea.feature.pathmacros.ProjectPropertiesPathMacroContributor";
	private static final String CACHE_INVALIDATOR_IMPL = "df.idea.feature.pathmacros.ProjectPathMacroCacheInvalidator";
	private static final String PROJECT_ROOTS_IMPL = "df.idea.feature.projectroots.NestedProjectRootCleanupActivity";
	private static final String UPDATE_SETTINGS_PROVIDER_IMPL = "df.idea.feature.updatesource.LocalPluginUpdateSettingsProvider";
	private static final String UPDATE_SOURCE_REGISTRAR_IMPL = "df.idea.feature.updatesource.LocalUpdateSourceRegistrar";
	private static final Assertions A = new Assertions();
	public static void main(String[] args) throws Exception {
		if (1 > args.length) {
			throw new IllegalArgumentException(
				"Usage: PluginDescriptorVerificationTest (<path-to-plugin.jar> | --from-idea-properties)"
			);
		}
		if ("--from-idea-properties".equals(args[0])) {
			verifyViaIdeaProperties();
		}
		else {
			Path jarPath = Path.of(args[0]).toAbsolutePath();
			PluginMainDescriptor basic = PluginDescriptorLoader.readBasicDescriptorDataFromArtifact(jarPath);
			A.assertTrue("basic descriptor loaded", null != basic);
			A.assertEquals("basic pluginId", PLUGIN_ID, basic.getPluginId().getIdString());
			A.assertEquals("basic plugin name", PLUGIN_NAME, basic.getName());
			PluginMainDescriptor descriptor = PluginDescriptorLoader.loadDescriptorFromArtifact(jarPath, null);
			A.assertTrue("full descriptor loaded", null != descriptor);
			verifyDescriptor(descriptor, jarPath);
			System.out.println("DESCRIPTOR_VERIFICATION_PASSED=" + jarPath + " (" + A.passedCount() + " assertions)");
		}
	}
	private static void verifyDescriptor(PluginMainDescriptor descriptor, Path jarPath) throws Exception {
		A.assertEquals("full pluginId", PLUGIN_ID, descriptor.getPluginId().getIdString());
		A.assertEquals("full plugin name", PLUGIN_NAME, descriptor.getName());
		A.assertEquals("plugin vendor URL", VENDOR_URL, descriptor.getVendorUrl());
		A.assertEquals("plugin version", "1.0.0", descriptor.getVersion());
		A.assertEquals("sinceBuild", "262", descriptor.getSinceBuild());
		A.assertEquals("untilBuild is open-ended (null)", null, descriptor.getUntilBuild());
		boolean hasLangDep = false;
		for (PluginDependency dep : descriptor.getDependencies()) {
			if ("com.intellij.modules.lang".equals(dep.getPluginId().getIdString()) && !dep.isOptional()) {
				hasLangDep = true;
			}
		}
		A.assertTrue("declares required dependency com.intellij.modules.lang", hasLangDep);
		boolean legacyWithoutAlias = PluginCompatibilityUtils.INSTANCE
			.isLegacyPluginWithoutPlatformAliasDependencies((IdeaPluginDescriptorImpl) descriptor);
		A.assertTrue("not classified as legacy Java-only plugin", !legacyWithoutAlias);
		String[] compatibleBuilds = {"262.10968.63", "262.12573.28", "262.99999.99", "263.1.0", "263.7307.200", "271.5000.12", "300.0.0"};
		for (String buildStr : compatibleBuilds) {
			BuildNumber bn = BuildNumber.fromString(buildStr);
			A.assertTrue("parsed build " + buildStr, null != bn);
			boolean incompatible = PluginManagerCore.isIncompatible(descriptor, bn);
			A.assertTrue("compatible with build " + buildStr, !incompatible);
		}
		BuildNumber tooOld = BuildNumber.fromString("261.99999.99");
		A.assertTrue("parsed old build 261.99999.99", null != tooOld);
		boolean oldIncompatible = PluginManagerCore.isIncompatible(descriptor, tooOld);
		A.assertTrue("rejects pre-262 build 261.99999.99", oldIncompatible);
		Map<String, List<ExtensionDescriptor>> extensions = descriptor.getExtensions();
		List<ExtensionDescriptor> vfqnp = extensions.get("com.intellij.virtualFileQualifiedNameProvider");
		A.assertTrue("registers com.intellij.virtualFileQualifiedNameProvider", null != vfqnp && 1 == vfqnp.size());
		ExtensionDescriptor copyRefExt = vfqnp.get(0);
		A.assertEquals("copyreference implementation class", COPY_REF_IMPL, copyRefExt.implementation);
		A.assertEquals("copyreference extension order is FIRST", LoadingOrder.FIRST.toString(), copyRefExt.order.toString());
		List<ExtensionDescriptor> ppmc = extensions.get("com.intellij.projectPathMacroContributor");
		A.assertTrue("registers com.intellij.projectPathMacroContributor", null != ppmc && 1 == ppmc.size());
		ExtensionDescriptor macroExt = ppmc.get(0);
		A.assertEquals("pathmacros implementation class", PATH_MACROS_IMPL, macroExt.implementation);
		List<ExtensionDescriptor> efl = extensions.get("com.intellij.editorFactoryListener");
		A.assertTrue("registers com.intellij.editorFactoryListener", null != efl && 1 == efl.size());
		ExtensionDescriptor mdFenceExt = efl.get(0);
		A.assertEquals("markdowncodefence implementation class", MARKDOWN_CODE_FENCE_IMPL, mdFenceExt.implementation);
		List<ExtensionDescriptor> psa = extensions.get("com.intellij.postStartupActivity");
		A.assertTrue("registers com.intellij.postStartupActivity", null != psa && 1 == psa.size());
		ExtensionDescriptor projectRootsExt = psa.get(0);
		A.assertEquals("projectroots implementation class", PROJECT_ROOTS_IMPL, projectRootsExt.implementation);
		List<ExtensionDescriptor> usp = extensions.get("com.intellij.updateSettingsProvider");
		A.assertTrue("registers com.intellij.updateSettingsProvider", null != usp && 1 == usp.size());
		ExtensionDescriptor updateProviderExt = usp.get(0);
		A.assertEquals("updatesource provider implementation class", UPDATE_SETTINGS_PROVIDER_IMPL, updateProviderExt.implementation);
		List<ListenerDescriptor> appListeners = descriptor.getAppContainerDescriptor().listeners;
		boolean foundInvalidator = false;
		boolean foundUpdateRegistrar = false;
		for (ListenerDescriptor ld : appListeners) {
			if (CACHE_INVALIDATOR_IMPL.equals(ld.listenerClassName)
				&& ProjectCloseListener.class.getName().equals(ld.topicClassName)) {
				foundInvalidator = true;
			}
			if (UPDATE_SOURCE_REGISTRAR_IMPL.equals(ld.listenerClassName)
				&& AppLifecycleListener.class.getName().equals(ld.topicClassName)) {
				foundUpdateRegistrar = true;
			}
		}
		A.assertTrue("registers ProjectPathMacroCacheInvalidator on ProjectCloseListener", foundInvalidator);
		A.assertTrue("registers LocalUpdateSourceRegistrar on AppLifecycleListener", foundUpdateRegistrar);
		try (URLClassLoader loader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, PluginDescriptorVerificationTest.class.getClassLoader())) {
			Object copyRefInstance = Class.forName(copyRefExt.implementation, true, loader).getDeclaredConstructor().newInstance();
			A.assertTrue("loaded class implements VirtualFileQualifiedNameProvider", copyRefInstance instanceof VirtualFileQualifiedNameProvider);
			Object mdFenceInstance = Class.forName(mdFenceExt.implementation, true, loader).getDeclaredConstructor().newInstance();
			A.assertTrue("loaded class implements EditorFactoryListener", mdFenceInstance instanceof EditorFactoryListener);
			Object macroInstance = Class.forName(macroExt.implementation, true, loader).getDeclaredConstructor().newInstance();
			A.assertTrue("loaded class implements ProjectWidePathMacroContributor", macroInstance instanceof ProjectWidePathMacroContributor);
			Object projectRootsInstance = Class.forName(projectRootsExt.implementation, true, loader).getDeclaredConstructor().newInstance();
			A.assertTrue("loaded class implements ProjectActivity", projectRootsInstance instanceof ProjectActivity);
			Object updateProviderInstance = Class.forName(updateProviderExt.implementation, true, loader).getDeclaredConstructor().newInstance();
			A.assertTrue("loaded class implements UpdateSettingsProvider", updateProviderInstance instanceof UpdateSettingsProvider);
			Object listenerInstance = Class.forName(CACHE_INVALIDATOR_IMPL, true, loader).getDeclaredConstructor().newInstance();
			A.assertTrue("loaded class implements ProjectCloseListener", listenerInstance instanceof ProjectCloseListener);
			Object registrarInstance = Class.forName(UPDATE_SOURCE_REGISTRAR_IMPL, true, loader).getDeclaredConstructor().newInstance();
			A.assertTrue("loaded class implements AppLifecycleListener", registrarInstance instanceof AppLifecycleListener);
		}
	}
	private static void verifyViaIdeaProperties() throws Exception {
		PathManager.loadProperties();
		String pluginPath = System.getProperty("plugin.path");
		A.assertTrue("plugin.path loaded from idea.properties", null != pluginPath && !pluginPath.isBlank());
		System.out.println("LOADED_PLUGIN_PATH=" + pluginPath);
		Path legacyCopyRefInPlugins = PathManager.getPluginsDir().resolve("df-copy-reference.jar");
		Path duplicateInPlugins = PathManager.getPluginsDir().resolve("df-idea.jar");
		A.assertTrue("no legacy df-copy-reference.jar in IDEA plugins dir", !Files.exists(legacyCopyRefInPlugins));
		A.assertTrue("no duplicate df-idea.jar in IDEA plugins dir", !Files.exists(duplicateInPlugins));
		PluginDescriptorLoadingContext loadingContext = new PluginDescriptorLoadingContext();
		ZipEntryResolverPool pool = (ZipEntryResolverPool) Class
			.forName("com.intellij.ide.plugins.NonShareableJavaZipFilePool")
			.getDeclaredConstructor()
			.newInstance();
		Method loadFromProperty = PluginDescriptorLoader.class.getMethod(
			"access$loadDescriptorsFromProperty",
			CoroutineScope.class,
			PluginDescriptorLoadingContext.class,
			ZipEntryResolverPool.class
		);
		try {
			DiscoveredPluginsList discovered = BuildersKt.runBlocking(
				EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> {
					try {
						@SuppressWarnings("unchecked")
						Deferred<DiscoveredPluginsList> deferred = (Deferred<DiscoveredPluginsList>) loadFromProperty
							.invoke(null, scope, loadingContext, pool);
						return deferred.await(continuation);
					}
					catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}
			);
			A.assertTrue("loadDescriptorsFromProperty returned non-null list", null != discovered);
			A.assertTrue(
				"source context is SystemPropertyProvided",
				discovered.getSource() instanceof PluginsSourceContext.SystemPropertyProvided
			);
			PluginMainDescriptor matched = null;
			int matchedCount = 0;
			boolean foundLegacyStandalone = false;
			for (PluginMainDescriptor p : discovered.getPlugins()) {
				String id = p.getPluginId().getIdString();
				if (id.endsWith(".copyreference")) {
					foundLegacyStandalone = true;
				}
				else if (PLUGIN_ID.equals(id)) {
					matched = p;
					matchedCount++;
				}
			}
			A.assertTrue("no legacy standalone copyreference plugin loaded alongside unified plugin", !foundLegacyStandalone);
			A.assertEquals("exactly one df.idea plugin discovered", 1, matchedCount);
			A.assertTrue("discovered df.idea via plugin.path (with non-BMP 𝐏)", null != matched);
			Path resolvedJar = matched.getPluginPath();
			A.assertTrue("resolved pluginPath exists on disk", null != resolvedJar && Files.isRegularFile(resolvedJar));
			verifyDescriptor(matched, resolvedJar);
			Path siblingUpdateXml = LocalPluginRepositoryLocator.resolveRepositoryFile(resolvedJar);
			A.assertTrue("sibling updatePlugins.xml exists next to resolved df-idea.jar", null != siblingUpdateXml && Files.isRegularFile(siblingUpdateXml));
			String repoUrl = LocalPluginRepositoryLocator.toRepositoryUrl(siblingUpdateXml);
			URI resolvedFromXml = new URI(repoUrl).resolve(new URI("df-idea.jar"));
			A.assertTrue("updatePlugins.xml relative jar URL resolves to exact df-idea.jar", Files.isSameFile(resolvedJar, Path.of(resolvedFromXml)));
			for (String buildStr : new String[]{"262.12573.28", "263.7307.200"}) {
				List<PluginUiModel> models = RepositoryHelper.loadPluginModels(
					repoUrl,
					BuildNumber.fromString(buildStr),
					null,
					PluginNodeModelBuilderFactory.INSTANCE
				);
				A.assertEquals("real sibling updatePlugins.xml loads 1 plugin for " + buildStr, 1, models.size());
				A.assertEquals("real updatePlugins.xml pluginId matches", PLUGIN_ID, models.get(0).getPluginId().getIdString());
				A.assertEquals("real updatePlugins.xml version matches loaded JAR", matched.getVersion(), models.get(0).getVersion());
			}
			System.out.println("IDEA_PROPERTIES_VERIFICATION_PASSED=" + resolvedJar + " (" + A.passedCount() + " assertions)");
		}
		finally {
			loadingContext.close();
			pool.close();
		}
	}
}
