// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.plugin_update_source;
import df.idea.support.Assertions;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class LocalPluginRepositoryLocatorTest {
	private static final Assertions A = new Assertions();
	/**
	 * - @used-by build.ps1
	 */
	public static void main(String[] args) throws Exception {
		// 2026-11-22
		// "Standardize df.idea feature package names with snake_case word separators":
		// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
		Path tempRoot = Files.createTempDirectory("df-plugin-update-source-locator-");
		try {
			testSiblingFileInNonBmpPath(tempRoot);
			testMissingUpdateXmlReturnsNull(tempRoot);
			testDirectoryLayout(tempRoot);
			testLocatePluginArtifactFallback(tempRoot);
		}
		finally {
			deleteRecursively(tempRoot);
		}
		System.out.println("LOCATOR_UPDATE_SOURCE_TESTS_PASSED (" + A.passedCount() + " assertions)");
	}
	/**
	 * - @used-by {@link LocalPluginRepositoryLocatorTest#main(String[])}
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
	 * - @used-by {@link LocalPluginRepositoryLocatorTest#main(String[])}
	 */
	private static void testDirectoryLayout(Path tempRoot) throws Exception {
		Path dirInside = tempRoot.resolve("dir-inside");
		Files.createDirectories(dirInside);
		Path insideXml = dirInside.resolve("updatePlugins.xml");
		Files.writeString(insideXml, "<plugins/>", StandardCharsets.UTF_8);
		Path resolvedInside = LocalPluginRepositoryLocator.resolveRepositoryFile(dirInside);
		A.assertTrue("resolves updatePlugins.xml inside plugin directory", null != resolvedInside && Files.isSameFile(insideXml, resolvedInside));
		Path parentDir = tempRoot.resolve("dir-parent");
		Path childDir = parentDir.resolve("extracted");
		Files.createDirectories(childDir);
		Path parentXml = parentDir.resolve("updatePlugins.xml");
		Files.writeString(parentXml, "<plugins/>", StandardCharsets.UTF_8);
		Path resolvedParent = LocalPluginRepositoryLocator.resolveRepositoryFile(childDir);
		A.assertTrue("falls back to parent directory updatePlugins.xml", null != resolvedParent && Files.isSameFile(parentXml, resolvedParent));
	}
	/**
	 * - @used-by {@link LocalPluginRepositoryLocatorTest#main(String[])}
	 */
	private static void testLocatePluginArtifactFallback(Path tempRoot) throws Exception {
		Path preferred = tempRoot.resolve("preferred.jar");
		Path chosen = LocalPluginRepositoryLocator.locatePluginArtifact(() -> preferred, LocalPluginRepositoryLocator.class);
		A.assertEquals("prefers descriptor supplier over CodeSource", preferred.toAbsolutePath().normalize(), chosen);
		Path fallback = LocalPluginRepositoryLocator.locatePluginArtifact(() -> null, LocalPluginRepositoryLocator.class);
		A.assertTrue("falls back to CodeSource when descriptor supplier returns null", null != fallback && Files.exists(fallback));
		Path afterException = LocalPluginRepositoryLocator.locatePluginArtifact(() -> {
			throw new IllegalStateException("registry unavailable");
		}, LocalPluginRepositoryLocator.class);
		A.assertTrue("falls back to CodeSource when descriptor supplier throws", null != afterException && Files.exists(afterException));
	}
	/**
	 * - @used-by {@link LocalPluginRepositoryLocatorTest#main(String[])}
	 */
	private static void testMissingUpdateXmlReturnsNull(Path tempRoot) throws Exception {
		Path dir = tempRoot.resolve("missing-xml");
		Files.createDirectories(dir);
		Path jar = dir.resolve("df-idea.jar");
		Files.writeString(jar, "jar-only", StandardCharsets.UTF_8);
		A.assertEquals("missing sibling updatePlugins.xml returns null", null, LocalPluginRepositoryLocator.resolveRepositoryFile(jar));
		A.assertEquals("null plugin location returns null", null, LocalPluginRepositoryLocator.resolveRepositoryFile(null));
		A.assertEquals("non-existent path returns null", null, LocalPluginRepositoryLocator.resolveRepositoryFile(dir.resolve("absent.jar")));
	}
	/**
	 * - @used-by {@link LocalPluginRepositoryLocatorTest#main(String[])}
	 */
	private static void testSiblingFileInNonBmpPath(Path tempRoot) throws Exception {
		Path outDir = tempRoot
			.resolve("IntelliJ IDEA")
			.resolve("\u2C0F")
			.resolve("\u227B")
			.resolve("\u2655")
			.resolve("\u26EF")
			.resolve("\uD835\uDC0F")
			.resolve("\u227B");
		Files.createDirectories(outDir);
		Path jar = outDir.resolve("df-idea.jar");
		Path xml = outDir.resolve("updatePlugins.xml");
		Files.writeString(jar, "jar-placeholder", StandardCharsets.UTF_8);
		Files.writeString(xml, "<plugins/>", StandardCharsets.UTF_8);
		Path resolved = LocalPluginRepositoryLocator.resolveRepositoryFile(jar);
		A.assertTrue("resolves sibling updatePlugins.xml next to JAR in non-BMP path", null != resolved);
		A.assertTrue("resolved path matches exact updatePlugins.xml", Files.isSameFile(xml, resolved));
		String url = LocalPluginRepositoryLocator.toRepositoryUrl(resolved);
		A.assertTrue("URL uses file:/// scheme", url.startsWith("file:///"));
		A.assertTrue("URL percent-encodes spaces", url.contains("IntelliJ%20IDEA"));
		A.assertTrue("URL preserves readable non-BMP 𝐏", url.contains("\uD835\uDC0F"));
		A.assertTrue("URL ends with /updatePlugins.xml", url.endsWith("/updatePlugins.xml"));
		URI parsed = new URI(url);
		URI resolvedJarUri = parsed.resolve(new URI("df-idea.jar"));
		A.assertTrue("relative df-idea.jar resolves back to exact JAR", Files.isSameFile(jar, Path.of(resolvedJarUri)));
	}
}
