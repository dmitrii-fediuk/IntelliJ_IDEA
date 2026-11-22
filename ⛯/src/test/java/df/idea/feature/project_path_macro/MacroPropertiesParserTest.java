// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.project_path_macro;
import df.idea.support.Assertions;
import java.util.Map;
public final class MacroPropertiesParserTest {
	private static final Assertions A = new Assertions();
	public static void main(String[] args) {
		testUtf8AndNonBmpParsing();
		testUtf8BomAndCommentsAndWhitespace();
		testWindowsBackslashesNotCorrupted();
		testKeyValidationAndReservedNames();
		testEmptyValuePreservedForUnsetAndDuplicateOverride();
		System.out.println("MACRO_PARSER_TESTS_PASSED=" + A.passedCount());
	}
	private static void testEmptyValuePreservedForUnsetAndDuplicateOverride() {
		String text = "df.m=../first\n"
			+ "df.m=../second\n"
			+ "df.unset=\n";
		MacroPropertiesParser.ParseResult res = MacroPropertiesParser.parseText(text, ".properties");
		A.assertEquals("duplicate key takes last value", "../second", res.entries().get("df.m"));
		A.assertEquals("empty value preserved for unset", "", res.entries().get("df.unset"));
	}
	private static void testKeyValidationAndReservedNames() {
		String text = "df.m=../ok\n"
			+ "bad..name=../nope\n"
			+ "bad space=../nope\n"
			+ "MODULE_DIR=../nope\n"
			+ "PROJECT_DIR=../nope\n"
			+ "missing_equals_line\n"
			+ "=no_key\n";
		MacroPropertiesParser.ParseResult res = MacroPropertiesParser.parseText(text, ".properties");
		A.assertEquals("only valid df.m kept", Map.of("df.m", "../ok"), res.entries());
		A.assertEquals("six invalid lines produced six warnings", 6, res.warnings().size());
	}
	private static void testUtf8AndNonBmpParsing() {
		String text = "df.m=../Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯\n"
			+ "df.p=../Ⰿ/≻/♕/⛯/𝐏\n"
			+ "df-k_1=../ꘖ/KB\n";
		MacroPropertiesParser.ParseResult res = MacroPropertiesParser.parseText(text, ".properties");
		A.assertEquals("no warnings on valid UTF-8", 0, res.warnings().size());
		A.assertEquals("df.m parsed verbatim", "../Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯", res.entries().get("df.m"));
		A.assertEquals("df.p parsed with non-BMP 𝐏", "../Ⰿ/≻/♕/⛯/𝐏", res.entries().get("df.p"));
		A.assertEquals("df-k_1 parsed", "../ꘖ/KB", res.entries().get("df-k_1"));
	}
	private static void testUtf8BomAndCommentsAndWhitespace() {
		String text = "\uFEFF# leading comment\r\n"
			+ "  ! second comment\r\n"
			+ "\r\n"
			+ "  df.m = ../path with spaces/dir # not comment  \r\n";
		MacroPropertiesParser.ParseResult res = MacroPropertiesParser.parseText(text, ".properties");
		A.assertEquals("BOM + comments have zero warnings", 0, res.warnings().size());
		A.assertEquals(
			"trimmed key and value preserve internal spaces and #",
			"../path with spaces/dir # not comment",
			res.entries().get("df.m")
		);
	}
	private static void testWindowsBackslashesNotCorrupted() {
		String text = "df.u=C:\\users\\test\\foo\\new\n"
			+ "df.rel=..\\bar\\baz\n";
		MacroPropertiesParser.ParseResult res = MacroPropertiesParser.parseText(text, ".properties");
		A.assertEquals("no warnings on Windows backslashes", 0, res.warnings().size());
		A.assertEquals("C:\\users\\test\\foo\\new preserved verbatim", "C:\\users\\test\\foo\\new", res.entries().get("df.u"));
		A.assertEquals("..\\bar\\baz preserved verbatim", "..\\bar\\baz", res.entries().get("df.rel"));
	}
}
