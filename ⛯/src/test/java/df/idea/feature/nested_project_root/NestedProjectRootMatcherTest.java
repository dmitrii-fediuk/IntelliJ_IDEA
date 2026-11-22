// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.nested_project_root;
import df.idea.support.Assertions;
import java.util.Collections;
import java.util.List;
public final class NestedProjectRootMatcherTest {
	private static final Assertions A = new Assertions();
	public static void main(String[] args) {
		testCaseSensitivity();
		testCoveredSet();
		testNormalization();
		testStrictDescendant();
		System.out.println("NESTED_PROJECT_ROOT_MATCHER_TESTS_PASSED (" + A.passedCount() + " assertions)");
	}
	private static void testCaseSensitivity() {
		A.assertEquals(
			"case-insensitive Windows match allows different casing",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://c:/Work/Example.com/.idea/example.com--1",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"case-insensitive Windows equal path is not strict descendant",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://c:/Work/Example.com",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"case-sensitive POSIX rejects mismatched directory casing",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file:///Work/Example/.idea/p1",
				"file:///work/example",
				true
			)
		);
		A.assertEquals(
			"case-sensitive POSIX matches exact path casing",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file:///work/example/.idea/p1",
				"file:///work/example",
				true
			)
		);
		A.assertEquals(
			"mixed-case scheme still matches on case-sensitive platforms",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"FILE:///work/example/.idea/p1",
				"file:///work/example",
				true
			)
		);
	}
	private static void testCoveredSet() {
		A.assertEquals(
			"empty content root set covers nothing",
			false,
			NestedProjectRootMatcher.isStrictlyCovered(
				"file://C:/work/example.com/.idea/p1",
				Collections.emptyList(),
				false
			)
		);
		List<String> roots = List.of(
			"file://C:/work/example.com",
			"file://C:/work/example.com/nested/app"
		);
		A.assertEquals(
			"nested .idea project is covered by outer content root",
			true,
			NestedProjectRootMatcher.isStrictlyCovered(
				"file://C:/work/example.com/.idea/example.com--1",
				roots,
				false
			)
		);
		A.assertEquals(
			"nested module root covered strictly by outer content root",
			true,
			NestedProjectRootMatcher.isStrictlyCovered(
				"file://C:/work/example.com/nested/app",
				roots,
				false
			)
		);
		A.assertEquals(
			"outer content root itself is not strictly covered",
			false,
			NestedProjectRootMatcher.isStrictlyCovered(
				"file://C:/work/example.com",
				roots,
				false
			)
		);
		A.assertEquals(
			"disjoint project root is not covered",
			false,
			NestedProjectRootMatcher.isStrictlyCovered(
				"file://C:/work/other-repo",
				roots,
				false
			)
		);
		A.assertEquals(
			"null project root is never covered",
			false,
			NestedProjectRootMatcher.isStrictlyCovered(null, roots, false)
		);
	}
	private static void testNormalization() {
		A.assertEquals("null URL normalizes to null", null, NestedProjectRootMatcher.normalizeUrl(null));
		A.assertEquals("blank URL normalizes to null", null, NestedProjectRootMatcher.normalizeUrl("   "));
		A.assertEquals("missing scheme separator returns null", null, NestedProjectRootMatcher.normalizeUrl("C:/work/repo"));
		A.assertEquals("empty scheme returns null", null, NestedProjectRootMatcher.normalizeUrl("://C:/work/repo"));
		A.assertEquals("empty path after scheme returns null", null, NestedProjectRootMatcher.normalizeUrl("file://"));
		A.assertEquals(
			"strips single trailing slash",
			"file://C:/work/repo",
			NestedProjectRootMatcher.normalizeUrl("file://C:/work/repo/")
		);
		A.assertEquals(
			"strips multiple trailing slashes",
			"file://C:/work/repo",
			NestedProjectRootMatcher.normalizeUrl("file://C:/work/repo///")
		);
		A.assertEquals(
			"preserves POSIX root slash",
			"file:///",
			NestedProjectRootMatcher.normalizeUrl("file:///")
		);
		A.assertEquals(
			"collapses extra slashes on POSIX root",
			"file:///",
			NestedProjectRootMatcher.normalizeUrl("file:////")
		);
		A.assertEquals(
			"normalizes Windows drive root consistently",
			"file://C:",
			NestedProjectRootMatcher.normalizeUrl("file://C:/")
		);
		A.assertEquals(
			"replaces backslashes and lowercases scheme",
			"file://C:/work/repo/.idea/p1",
			NestedProjectRootMatcher.normalizeUrl("FILE://C:\\work\\repo\\.idea\\p1\\")
		);
	}
	private static void testStrictDescendant() {
		A.assertEquals(
			"direct child under content root is strict descendant",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com/.idea",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"deeply nested child under content root is strict descendant",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com/.idea/example.com--1",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"equal paths are not strict descendants",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"equal paths differing only by trailing slash are not strict descendants",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com/",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"trailing slash on ancestor still matches nested child",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com/.idea/p1",
				"file://C:/work/example.com/",
				false
			)
		);
		A.assertEquals(
			"sibling prefix trap with hyphen is rejected",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com-2",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"sibling prefix trap with dot is rejected",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com.bak",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"parent is not a descendant of child",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"Windows drive root covers nested path",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/example.com",
				"file://C:/",
				false
			)
		);
		A.assertEquals(
			"Windows drive root does not cover itself",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/",
				"file://C:/",
				false
			)
		);
		A.assertEquals(
			"Windows drive root does not cover different drive",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://D:/work/example.com",
				"file://C:/",
				false
			)
		);
		A.assertEquals(
			"POSIX root covers nested path",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file:///opt/example",
				"file:///",
				true
			)
		);
		A.assertEquals(
			"POSIX root does not cover itself",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"file:///",
				"file:///",
				true
			)
		);
		A.assertEquals(
			"different schemes do not match",
			false,
			NestedProjectRootMatcher.isStrictDescendant(
				"jar://C:/work/example.com/app.jar",
				"file://C:/work/example.com",
				false
			)
		);
		A.assertEquals(
			"spaces and non-BMP Unicode paths match cleanly",
			true,
			NestedProjectRootMatcher.isStrictDescendant(
				"file://C:/work/IntelliJ IDEA/\u2C0F/\u227B/\u2655/\u26EF/\uD835\uDC0F/.idea/sub",
				"file://C:/work/IntelliJ IDEA/\u2C0F/\u227B/\u2655/\u26EF/\uD835\uDC0F",
				false
			)
		);
	}
}
