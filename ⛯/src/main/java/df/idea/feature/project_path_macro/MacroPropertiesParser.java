// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.project_path_macro;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class MacroPropertiesParser {
	public static final Pattern VALID_MACRO_NAME = Pattern.compile("^[A-Za-z0-9_.-]+$");
	private static final Set<String> RESERVED_SYSTEM_MACROS = Set.of(
		"APPLICATION_HOME_DIR",
		"APPLICATION_CONFIG_DIR",
		"APPLICATION_PLUGINS_DIR",
		"PROJECT_DIR",
		"PROJECT_NAME",
		"MODULE_DIR",
		"MODULE_WORKING_DIR",
		"USER_HOME"
	);
	public record ParseResult(
		@NotNull Map<String, String> entries,
		@NotNull List<String> warnings
	) {}
	/**
	 * - @used-by non-instantiable utility class
	 */
	private MacroPropertiesParser() {}
	/**
	 * - @used-by {@link MacroPathResolver#loadAndResolve(List)}
	 */
	public static @NotNull ParseResult parseFile(@NotNull Path file) throws IOException {
		return parseText(Files.readString(file, StandardCharsets.UTF_8), file.toString());
	}
	/**
	 * - @used-by {@link MacroPropertiesParser#parseFile(Path)}
	 * - @used-by {@link MacroPropertiesParserTest#testEmptyValuePreservedForUnsetAndDuplicateOverride()}
	 * - @used-by {@link MacroPropertiesParserTest#testKeyValidationAndReservedNames()}
	 * - @used-by {@link MacroPropertiesParserTest#testUtf8AndNonBmpParsing()}
	 * - @used-by {@link MacroPropertiesParserTest#testUtf8BomAndCommentsAndWhitespace()}
	 * - @used-by {@link MacroPropertiesParserTest#testWindowsBackslashesNotCorrupted()}
	 */
	public static @NotNull ParseResult parseText(@NotNull String content, @NotNull String sourceLabel) {
		Map<String, String> entries = new LinkedHashMap<>();
		List<String> warnings = new ArrayList<>();
		String text = content.startsWith("\uFEFF") ? content.substring(1) : content;
		String[] lines = text.split("\\R", -1);
		for (int i = 0; i < lines.length; i++) {
			int lineNumber = i + 1;
			String rawLine = lines[i];
			String trimmed = rawLine.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
				continue;
			}
			int eq = rawLine.indexOf('=');
			if (0 >= eq) {
				warnings.add(sourceLabel + ":" + lineNumber + ": missing '=' separator");
				continue;
			}
			String key = rawLine.substring(0, eq).trim();
			String value = rawLine.substring(eq + 1).trim();
			String keyError = validateMacroName(key);
			if (null != keyError) {
				warnings.add(sourceLabel + ":" + lineNumber + ": ignoring macro '" + key + "' (" + keyError + ")");
				continue;
			}
			entries.put(key, value);
		}
		return new ParseResult(
			Collections.unmodifiableMap(entries),
			Collections.unmodifiableList(warnings)
		);
	}
	/**
	 * - @used-by unused
	 */
	static boolean isValidMacroName(@NotNull String name) {
		return null == validateMacroName(name);
	}
	/**
	 * - @used-by {@link MacroPropertiesParser#isValidMacroName(String)}
	 * - @used-by {@link MacroPropertiesParser#parseText(String, String)}
	 */
	private static String validateMacroName(@NotNull String name) {
		String r = null;
		if (name.isEmpty()) {
			r = "empty macro name";
		}
		else if (!VALID_MACRO_NAME.matcher(name).matches()) {
			r = "must match [A-Za-z0-9_.-]+";
		}
		else if (name.contains("..")) {
			r = "must not contain '..'";
		}
		else if (RESERVED_SYSTEM_MACROS.contains(name)) {
			r = "reserved IntelliJ system macro";
		}
		return r;
	}
}
