// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.project_path_macro;
import df.idea.support.Assertions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public final class MacroPathResolverTest {
	private static final Assertions A = new Assertions();
	public static void main(String[] args) throws Exception {
		testRelativeAndAbsoluteResolutionWithUnicode();
		testCrossVariableCompositionAndForwardRefs();
		testLayeredOuterAndInnerInheritanceOverrideAndUnset();
		testCycleAndUndefinedReferenceHandling();
		testCacheHitInvalidationAndEviction();
		testDirectoryNamedPropertiesIgnoredSafely();
		System.out.println("MACRO_RESOLVER_TESTS_PASSED=" + A.passedCount());
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
	private static void testCacheHitInvalidationAndEviction() throws IOException {
		Path tmp = Files.createTempDirectory("df-macro-cache-");
		try {
			Path idea = Files.createDirectories(tmp.resolve("proj/.idea"));
			Path miscXml = idea.resolve("misc.xml");
			Path props = idea.resolve(".properties");
			MacroPathResolver resolver = new MacroPathResolver(w -> {});
			String projectPath = miscXml.toString();
			Map<String, String> empty = resolver.resolveForProject(projectPath);
			A.assertEquals("absent .properties returns empty map", Map.of(), empty);
			A.assertEquals("cache records absent state", 1, resolver.cacheSize());
			Files.writeString(props, "df.m=../v1\n", StandardCharsets.UTF_8);
			Map<String, String> first = resolver.resolveForProject(projectPath);
			String projUnix = tmp.resolve("proj").toAbsolutePath().normalize().toString().replace('\\', '/');
			A.assertEquals("created .properties loaded", projUnix + "/v1", first.get("df.m"));
			Map<String, String> second = resolver.resolveForProject(projectPath);
			A.assertTrue("unchanged .properties hits cache (same instance)", first == second);
			Files.writeString(props, "df.m=../v2-longer\n", StandardCharsets.UTF_8);
			Files.setLastModifiedTime(props, FileTime.from(Instant.now().plusSeconds(5)));
			Map<String, String> third = resolver.resolveForProject(projectPath);
			A.assertEquals("edited .properties reloads", projUnix + "/v2-longer", third.get("df.m"));
			Files.delete(props);
			Map<String, String> fourth = resolver.resolveForProject(projectPath);
			A.assertEquals("deleted .properties clears macros", Map.of(), fourth);
			resolver.evict(projectPath.replace('\\', '/'));
			A.assertEquals("evict clears cache entry", 0, resolver.cacheSize());
		}
		finally {
			deleteTree(tmp);
		}
	}
	private static void testCrossVariableCompositionAndForwardRefs() throws IOException {
		Path tmp = Files.createTempDirectory("df-macro-comp-");
		try {
			Path idea = Files.createDirectories(tmp.resolve("proj/.idea"));
			Path miscXml = idea.resolve("misc.xml");
			Files.writeString(
				idea.resolve(".properties"),
				"df.v=$df.m$/vendor\n"
				+ "df.m=${df.w}/example.com/2026-10-26/⛯\n"
				+ "df.w=../Ⰿ/≻/♕/⛯/𝐖\n",
				StandardCharsets.UTF_8
			);
			List<String> warnings = new ArrayList<>();
			MacroPathResolver resolver = new MacroPathResolver(warnings::add);
			Map<String, String> macros = resolver.resolveForProject(miscXml.toString());
			String projUnix = tmp.resolve("proj").toAbsolutePath().normalize().toString().replace('\\', '/');
			A.assertEquals("composition produced zero warnings", 0, warnings.size());
			A.assertEquals("df.w resolved", projUnix + "/Ⰿ/≻/♕/⛯/𝐖", macros.get("df.w"));
			A.assertEquals("df.m resolved via ${df.w} forward ref", projUnix + "/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯", macros.get("df.m"));
			A.assertEquals("df.v resolved via $df.m$ forward ref", projUnix + "/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯/vendor", macros.get("df.v"));
		}
		finally {
			deleteTree(tmp);
		}
	}
	private static void testCycleAndUndefinedReferenceHandling() throws IOException {
		Path tmp = Files.createTempDirectory("df-macro-err-");
		try {
			Path idea = Files.createDirectories(tmp.resolve("proj/.idea"));
			Path miscXml = idea.resolve("misc.xml");
			Files.writeString(
				idea.resolve(".properties"),
				"df.ok=../valid\n"
				+ "df.a=${df.b}/a\n"
				+ "df.b=${df.a}/b\n"
				+ "df.bad=${df.missing}/x\n",
				StandardCharsets.UTF_8
			);
			List<String> warnings = new ArrayList<>();
			MacroPathResolver resolver = new MacroPathResolver(warnings::add);
			Map<String, String> macros = resolver.resolveForProject(miscXml.toString());
			String projUnix = tmp.resolve("proj").toAbsolutePath().normalize().toString().replace('\\', '/');
			A.assertEquals("valid macro survives alongside cyclic/undefined entries", Map.of("df.ok", projUnix + "/valid"), macros);
			A.assertTrue("warnings recorded for cycle and undefined ref", 2 <= warnings.size());
		}
		finally {
			deleteTree(tmp);
		}
	}
	private static void testDirectoryNamedPropertiesIgnoredSafely() throws IOException {
		Path tmp = Files.createTempDirectory("df-macro-dir-");
		try {
			Path idea = Files.createDirectories(tmp.resolve("proj/.idea"));
			Files.createDirectories(idea.resolve(".properties"));
			Path miscXml = idea.resolve("misc.xml");
			MacroPathResolver resolver = new MacroPathResolver(w -> {});
			A.assertEquals("directory named .properties does not throw", Map.of(), resolver.resolveForProject(miscXml.toString()));
		}
		finally {
			deleteTree(tmp);
		}
	}
	private static void testLayeredOuterAndInnerInheritanceOverrideAndUnset() throws IOException {
		Path tmp = Files.createTempDirectory("df-macro-layer-");
		try {
			Path outerIdea = Files.createDirectories(tmp.resolve("client/.idea"));
			Path innerIdea = Files.createDirectories(outerIdea.resolve("sub-5/.idea"));
			Path miscXml = innerIdea.resolve("misc.xml");
			Files.writeString(
				outerIdea.resolve(".properties"),
				"df.m=../magento\n"
				+ "df.shared=../shared\n"
				+ "df.drop=../obsolete\n",
				StandardCharsets.UTF_8
			);
			Files.writeString(
				innerIdea.resolve(".properties"),
				"df.sub=${df.m}/app/code\n"
				+ "df.shared=../../../custom-shared\n"
				+ "df.drop=\n",
				StandardCharsets.UTF_8
			);
			List<String> warnings = new ArrayList<>();
			MacroPathResolver resolver = new MacroPathResolver(warnings::add);
			Map<String, String> macros = resolver.resolveForProject(miscXml.toString());
			String clientUnix = tmp.resolve("client").toAbsolutePath().normalize().toString().replace('\\', '/');
			A.assertEquals("layered resolution zero warnings", 0, warnings.size());
			A.assertEquals("outer df.m inherited", clientUnix + "/magento", macros.get("df.m"));
			A.assertEquals("inner df.sub references outer df.m", clientUnix + "/magento/app/code", macros.get("df.sub"));
			A.assertEquals("inner df.shared overrides outer", clientUnix + "/custom-shared", macros.get("df.shared"));
			A.assertEquals("inner empty df.drop unsets outer", null, macros.get("df.drop"));
		}
		finally {
			deleteTree(tmp);
		}
	}
	private static void testRelativeAndAbsoluteResolutionWithUnicode() throws IOException {
		Path tmp = Files.createTempDirectory("df-macro-rel-");
		try {
			Path outerIdea = Files.createDirectories(tmp.resolve("example.com/.idea"));
			Path innerIdea = Files.createDirectories(outerIdea.resolve("example.com--1/.idea"));
			Path miscXml = innerIdea.resolve("misc.xml");
			Files.writeString(miscXml, "<project/>", StandardCharsets.UTF_8);
			Path props = outerIdea.resolve(".properties");
			Files.writeString(
				props,
				"df.m=../Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯\n"
				+ "df.abs=C:\\work\\shared\\lib\\\n"
				+ "df.root=..\n",
				StandardCharsets.UTF_8
			);
			List<String> warnings = new ArrayList<>();
			MacroPathResolver resolver = new MacroPathResolver(warnings::add);
			Map<String, String> macros = resolver.resolveForProject(miscXml.toString());
			String rootUnix = tmp.resolve("example.com").toAbsolutePath().normalize().toString().replace('\\', '/');
			A.assertEquals("zero warnings", 0, warnings.size());
			A.assertEquals(
				"df.m resolved relative to outer .idea with non-BMP Unicode",
				rootUnix + "/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯",
				macros.get("df.m")
			);
			A.assertEquals(
				"df.abs normalized to forward slashes without trailing slash",
				"C:/work/shared/lib",
				macros.get("df.abs")
			);
			A.assertEquals("df.root resolved to client root", rootUnix, macros.get("df.root"));
		}
		finally {
			deleteTree(tmp);
		}
	}
}
