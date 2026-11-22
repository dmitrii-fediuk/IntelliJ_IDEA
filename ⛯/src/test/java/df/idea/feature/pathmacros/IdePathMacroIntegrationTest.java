// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
package df.idea.feature.pathmacros;
import com.intellij.application.options.PathMacrosCollector;
import com.intellij.application.options.ReplacePathToMacroMap;
import com.intellij.openapi.components.ExpandMacroToPathMap;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.util.JDOMUtil;
import df.idea.support.Assertions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import org.jdom.Element;
public final class IdePathMacroIntegrationTest {
	private static final Assertions A = new Assertions();
	public static void main(String[] args) throws Exception {
		testDotMacroMatchesCollectorPattern();
		testNestedImlAndProjectXmlRoundTrip();
		testMultipleNestedVariablesChooseLongestMatchOnSave();
		System.out.println("IDE_PATH_MACRO_INTEGRATION_TESTS_PASSED=" + A.passedCount());
	}
	private static void deleteTree(Path root) throws IOException {
		if (Files.exists(root)) {
			try (var stream = Files.walk(root)) {
				List<Path> paths = stream.sorted((a, b) -> b.compareTo(a)).toList();
				for (Path p : paths) {
					Files.deleteIfExists(p);
				}
			}
		}
	}
	private static void testDotMacroMatchesCollectorPattern() {
		Matcher m = PathMacrosCollector.MACRO_PATTERN.matcher("file://$df.m$/pub/media");
		A.assertTrue("PathMacrosCollector.MACRO_PATTERN matches $df.m$", m.find());
		A.assertEquals("extracted macro name is df.m", "df.m", m.group(1));
	}
	private static void testMultipleNestedVariablesChooseLongestMatchOnSave() throws IOException {
		Path tmp = Files.createTempDirectory("df-idea-multi-");
		try {
			Path clientRoot = Files.createDirectories(tmp.resolve("client"));
			Path outerIdea = Files.createDirectories(clientRoot.resolve(".idea"));
			Path projectDir = Files.createDirectories(outerIdea.resolve("proj-1"));
			Path innerIdea = Files.createDirectories(projectDir.resolve(".idea"));
			Path miscXml = innerIdea.resolve("misc.xml");
			Files.writeString(
				outerIdea.resolve(".properties"),
				"df.w=../Ⰿ/≻/♕/⛯/𝐖\n"
				+ "df.m=${df.w}/site/2026-10-26/⛯\n"
				+ "df.v=${df.m}/vendor\n",
				StandardCharsets.UTF_8
			);
			MacroPathResolver resolver = new MacroPathResolver(w -> {});
			Map<String, String> macros = resolver.resolveForProject(miscXml.toString());
			String projectUnix = projectDir.toAbsolutePath().normalize().toString().replace('\\', '/');
			ReplacePathToMacroMap replaceMap = new ReplacePathToMacroMap();
			PathMacroManager.addFileHierarchyReplacements(replaceMap, "MODULE_DIR", projectUnix, null);
			macros.forEach((k, v) -> replaceMap.addMacroReplacement(v, k));
			String clientUnix = clientRoot.toAbsolutePath().normalize().toString().replace('\\', '/');
			A.assertEquals(
				"vendor path picks deepest variable $df.v$",
				"file://$df.v$/magento/magento2-base",
				replaceMap.substitute("file://" + clientUnix + "/Ⰿ/≻/♕/⛯/𝐖/site/2026-10-26/⛯/vendor/magento/magento2-base", false)
			);
			A.assertEquals(
				"magento pub path picks $df.m$",
				"file://$df.m$/pub/media",
				replaceMap.substitute("file://" + clientUnix + "/Ⰿ/≻/♕/⛯/𝐖/site/2026-10-26/⛯/pub/media", false)
			);
			A.assertEquals(
				"sibling site path picks $df.w$",
				"file://$df.w$/other/index.php",
				replaceMap.substitute("file://" + clientUnix + "/Ⰿ/≻/♕/⛯/𝐖/other/index.php", false)
			);
		}
		finally {
			deleteTree(tmp);
		}
	}
	private static void testNestedImlAndProjectXmlRoundTrip() throws Exception {
		Path tmp = Files.createTempDirectory("df-idea-int-");
		try {
			Path clientRoot = Files.createDirectories(tmp.resolve("example.com"));
			Path outerIdea = Files.createDirectories(clientRoot.resolve(".idea"));
			Path projectDir = Files.createDirectories(outerIdea.resolve("example.com--1"));
			Path innerIdea = Files.createDirectories(projectDir.resolve(".idea"));
			Path miscXml = innerIdea.resolve("misc.xml");
			Files.writeString(miscXml, "<project version=\"4\"/>", StandardCharsets.UTF_8);
			Files.writeString(
				outerIdea.resolve(".properties"),
				"df.m=../Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯\n",
				StandardCharsets.UTF_8
			);
			ProjectPropertiesPathMacroContributor contributor = new ProjectPropertiesPathMacroContributor();
			Map<String, String> macros = contributor.getProjectPathMacros(miscXml.toString().replace('\\', '/'));
			String clientUnix = clientRoot.toAbsolutePath().normalize().toString().replace('\\', '/');
			String projectUnix = projectDir.toAbsolutePath().normalize().toString().replace('\\', '/');
			String expectedMagento = clientUnix + "/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯";
			A.assertEquals("contributor returns df.m", expectedMagento, macros.get("df.m"));
			ExpandMacroToPathMap modExpand = new ExpandMacroToPathMap();
			PathMacroManager.addFileHierarchyReplacements(modExpand, "MODULE_DIR", projectUnix);
			macros.forEach(modExpand::addMacroExpand);
			ReplacePathToMacroMap modReplace = new ReplacePathToMacroMap();
			PathMacroManager.addFileHierarchyReplacements(modReplace, "MODULE_DIR", projectUnix, null);
			macros.forEach((k, v) -> modReplace.addMacroReplacement(v, k));
			String imlWithMacro = """
				<?xml version="1.0" encoding="UTF-8"?>
				<module type="JAVA_MODULE" version="4">
					<component name="NewModuleRootManager" inherit-compiler-output="true">
						<exclude-output />
						<content url="file://$df.m$">
							<excludeFolder url="file://$df.m$/pub/media" />
							<excludeFolder url="file://$df.m$/pub/opt" />
							<excludeFolder url="file://$df.m$/pub/static" />
							<excludeFolder url="file://$df.m$/var" />
							<excludeFolder url="file://$df.m$/vendor/magento/magento2-base" />
							<sourceFolder url="file://$df.m$/var" isTestSource="false" />
						</content>
					</component>
				</module>
				""";
			Element imlEl = JDOMUtil.load(imlWithMacro);
			modExpand.substitute(imlEl, false);
			Element content = imlEl.getChild("component").getChild("content");
			A.assertEquals("iml content url expands to Magento root", "file://" + expectedMagento, content.getAttributeValue("url"));
			A.assertEquals("iml excludeFolder expands", "file://" + expectedMagento + "/pub/media", content.getChildren("excludeFolder").get(0).getAttributeValue("url"));
			A.assertTrue("no unexpanded $df.m$ remains in iml", !JDOMUtil.write(imlEl).contains("$df.m$"));
			modReplace.substitute(imlEl, false);
			A.assertEquals("iml save collapses content url to file://$df.m$", "file://$df.m$", content.getAttributeValue("url"));
			for (Element excl : content.getChildren("excludeFolder")) {
				String u = excl.getAttributeValue("url");
				A.assertTrue("iml save keeps $df.m$ in excludeFolder: " + u, u.startsWith("file://$df.m$/"));
			}
			A.assertEquals(
				"iml save keeps $df.m$ in sourceFolder",
				"file://$df.m$/var",
				content.getChild("sourceFolder").getAttributeValue("url")
			);
			ExpandMacroToPathMap projExpand = new ExpandMacroToPathMap();
			PathMacroManager.addFileHierarchyReplacements(projExpand, "PROJECT_DIR", projectUnix);
			macros.forEach(projExpand::addMacroExpand);
			ReplacePathToMacroMap projReplace = new ReplacePathToMacroMap();
			PathMacroManager.addFileHierarchyReplacements(projReplace, "PROJECT_DIR", projectUnix, null);
			macros.forEach((k, v) -> projReplace.addMacroReplacement(v, k));
			String phpToolExpanded = projExpand.substitute("$df.m$/vendor/bin/phpmd", false);
			A.assertEquals("php.xml tool_path expands", expectedMagento + "/vendor/bin/phpmd", phpToolExpanded);
			A.assertEquals("php.xml tool_path collapses to $df.m$", "$df.m$/vendor/bin/phpmd", projReplace.substitute(phpToolExpanded, false));
			String schemaExpanded = projExpand.substitute("file://$df.m$/vendor/example/core/composer.json", false);
			A.assertEquals("jsonSchemas.xml path expands", "file://" + expectedMagento + "/vendor/example/core/composer.json", schemaExpanded);
			A.assertEquals(
				"jsonSchemas.xml path collapses to file://$df.m$",
				"file://$df.m$/vendor/example/core/composer.json",
				projReplace.substitute(schemaExpanded, false)
			);
		}
		finally {
			MacroPathResolver.shared().evict(tmp.resolve("example.com/.idea/example.com--1/.idea/misc.xml").toString());
			deleteTree(tmp);
		}
	}
}
