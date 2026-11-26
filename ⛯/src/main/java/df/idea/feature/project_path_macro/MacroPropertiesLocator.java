// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.project_path_macro;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class MacroPropertiesLocator {
	public static final String PROPERTIES_FILE_NAME = ".properties";
	private static final String IDEA_DIR_NAME = ".idea";
	/**
	 * - @used-by non-instantiable utility class
	 */
	private MacroPropertiesLocator() {}
	/**
	 * - @used-by {@link MacroPathResolver#resolveForProject(String)}
	 * - @used-by {@link MacroPropertiesLocatorTest#testBlankAndInvalidPathsReturnEmpty()}
	 * - @used-by {@link MacroPropertiesLocatorTest#testFileBasedIprProject()}
	 * - @used-by {@link MacroPropertiesLocatorTest#testMultiProjectMiscXml()}
	 * - @used-by {@link MacroPropertiesLocatorTest#testSingleProjectMiscXml()}
	 * - @used-by {@link MacroPropertiesLocatorTest#testTwoLevelNestedMultiProjectMiscXml()}
	 */
	public static @NotNull List<Path> findCandidateFiles(@Nullable String projectFilePath) {
		List<Path> r = List.of();
		if (null != projectFilePath && !projectFilePath.isBlank()) {
			Path normalized = null;
			try {
				normalized = Path.of(projectFilePath).toAbsolutePath().normalize();
			}
			catch (InvalidPathException ignored) {
			}
			if (null != normalized) {
				List<Path> innerToOuter = new ArrayList<>();
				Path fileName = normalized.getFileName();
				String lowerName = null == fileName ? "" : fileName.toString().toLowerCase(Locale.ROOT);
				Path cursor;
				if (IDEA_DIR_NAME.equalsIgnoreCase(lowerName)) {
					cursor = normalized;
				}
				else if (lowerName.endsWith(".ipr")) {
					Path iprDir = normalized.getParent();
					if (null != iprDir) {
						innerToOuter.add(iprDir.resolve(PROPERTIES_FILE_NAME));
					}
					cursor = iprDir;
				}
				else if (!Files.isDirectory(normalized)) {
					cursor = normalized.getParent();
				}
				else {
					Path nestedIdea = normalized.resolve(IDEA_DIR_NAME);
					if (Files.isDirectory(nestedIdea)) {
						innerToOuter.add(nestedIdea.resolve(PROPERTIES_FILE_NAME));
					}
					cursor = normalized;
				}
				while (null != cursor) {
					Path dirName = cursor.getFileName();
					if (null != dirName && IDEA_DIR_NAME.equalsIgnoreCase(dirName.toString())) {
						innerToOuter.add(cursor.resolve(PROPERTIES_FILE_NAME));
					}
					cursor = cursor.getParent();
				}
				if (!innerToOuter.isEmpty()) {
					Collections.reverse(innerToOuter);
					r = List.copyOf(new LinkedHashSet<>(innerToOuter));
				}
			}
		}
		return r;
	}
}
