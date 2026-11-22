// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
package df.idea.feature.pathmacros;
import df.idea.support.Assertions;
import java.nio.file.Path;
import java.util.List;
public final class MacroPropertiesLocatorTest {
	private static final Assertions A = new Assertions();
	public static void main(String[] args) {
		testSingleProjectMiscXml();
		testMultiProjectMiscXml();
		testTwoLevelNestedMultiProjectMiscXml();
		testFileBasedIprProject();
		testBlankAndInvalidPathsReturnEmpty();
		System.out.println("MACRO_LOCATOR_TESTS_PASSED=" + A.passedCount());
	}
	private static void testBlankAndInvalidPathsReturnEmpty() {
		A.assertEquals("null path returns empty", List.of(), MacroPropertiesLocator.findCandidateFiles(null));
		A.assertEquals("blank path returns empty", List.of(), MacroPropertiesLocator.findCandidateFiles("   "));
		A.assertEquals("invalid NUL char returns empty", List.of(), MacroPropertiesLocator.findCandidateFiles("C:/bad\0path/misc.xml"));
	}
	private static void testFileBasedIprProject() {
		List<Path> candidates = MacroPropertiesLocator.findCandidateFiles("C:/work/legacy/demo.ipr");
		A.assertEquals(
			"ipr project checks sibling .properties",
			List.of(Path.of("C:/work/legacy/.properties").toAbsolutePath().normalize()),
			candidates
		);
	}
	private static void testMultiProjectMiscXml() {
		List<Path> candidates = MacroPropertiesLocator.findCandidateFiles(
			"C:/work/clients/example.com/.idea/example.com--1/.idea/misc.xml"
		);
		A.assertEquals(
			"multi-project finds outer then inner .idea/.properties",
			List.of(
				Path.of("C:/work/clients/example.com/.idea/.properties").toAbsolutePath().normalize(),
				Path.of("C:/work/clients/example.com/.idea/example.com--1/.idea/.properties").toAbsolutePath().normalize()
			),
			candidates
		);
	}
	private static void testSingleProjectMiscXml() {
		List<Path> candidates = MacroPropertiesLocator.findCandidateFiles("C:/work/app/.idea/misc.xml");
		A.assertEquals(
			"single project finds its own .idea/.properties",
			List.of(Path.of("C:/work/app/.idea/.properties").toAbsolutePath().normalize()),
			candidates
		);
	}
	private static void testTwoLevelNestedMultiProjectMiscXml() {
		List<Path> candidates = MacroPropertiesLocator.findCandidateFiles(
			"C:/work/clients/example.org/s/192.0.2.1/.idea/1/sub/.idea/misc.xml"
		);
		A.assertEquals(
			"two-level nested project finds outer then inner .idea/.properties",
			List.of(
				Path.of("C:/work/clients/example.org/s/192.0.2.1/.idea/.properties").toAbsolutePath().normalize(),
				Path.of("C:/work/clients/example.org/s/192.0.2.1/.idea/1/sub/.idea/.properties").toAbsolutePath().normalize()
			),
			candidates
		);
	}
}
