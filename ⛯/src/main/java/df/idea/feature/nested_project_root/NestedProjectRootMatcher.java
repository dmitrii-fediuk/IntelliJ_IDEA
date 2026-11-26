// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.nested_project_root;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
final class NestedProjectRootMatcher {
	/**
	 * - @used-by non-instantiable utility class
	 */
	private NestedProjectRootMatcher() {}
	/**
	 * - @used-by {@link NestedProjectRootMatcher#isStrictlyCovered(String, Iterable, boolean)}
	 * - @used-by {@link NestedProjectRootMatcherTest#testCaseSensitivity()}
	 * - @used-by {@link NestedProjectRootMatcherTest#testStrictDescendant()}
	 */
	static boolean isStrictDescendant(
		@Nullable String candidateUrl,
		@Nullable String ancestorUrl,
		boolean caseSensitive
	) {
		boolean r = false;
		String candidate = normalizeUrl(candidateUrl);
		String ancestor = normalizeUrl(ancestorUrl);
		if (null != candidate && null != ancestor) {
			int ancestorLen = ancestor.length();
			if (candidate.length() > ancestorLen && candidate.regionMatches(!caseSensitive, 0, ancestor, 0, ancestorLen)) {
				r = ancestor.endsWith("/") || '/' == candidate.charAt(ancestorLen);
			}
		}
		return r;
	}
	/**
	 * - @used-by {@link NestedProjectRootCleaner#findRedundantProjectRoots(EntityStorage, ProjectRootEntityAdapter, boolean)}
	 * - @used-by {@link NestedProjectRootMatcherTest#testCoveredSet()}
	 */
	static boolean isStrictlyCovered(
		@Nullable String projectRootUrl,
		@NotNull Iterable<String> contentRootUrls,
		boolean caseSensitive
	) {
		boolean r = false;
		String normalizedProjectRoot = normalizeUrl(projectRootUrl);
		if (null != normalizedProjectRoot) {
			for (String contentRootUrl : contentRootUrls) {
				if (isStrictDescendant(normalizedProjectRoot, contentRootUrl, caseSensitive)) {
					r = true;
					break;
				}
			}
		}
		return r;
	}
	/**
	 * - @used-by {@link NestedProjectRootCleaner#collectContentRootUrls(EntityStorage)}
	 * - @used-by {@link NestedProjectRootMatcher#isStrictDescendant(String, String, boolean)}
	 * - @used-by {@link NestedProjectRootMatcher#isStrictlyCovered(String, Iterable, boolean)}
	 * - @used-by {@link NestedProjectRootMatcherTest#testNormalization()}
	 */
	static @Nullable String normalizeUrl(@Nullable String rawUrl) {
		String r = null;
		if (null != rawUrl && !rawUrl.isBlank()) {
			String normalized = rawUrl.trim().replace('\\', '/');
			int schemeEnd = normalized.indexOf("://");
			if (0 < schemeEnd) {
				int pathStart = schemeEnd + 3;
				int end = normalized.length();
				while (pathStart + 1 < end && '/' == normalized.charAt(end - 1)) {
					end--;
				}
				if (pathStart < end) {
					r = normalized.substring(0, schemeEnd).toLowerCase(Locale.ROOT)
						+ normalized.substring(schemeEnd, end);
				}
			}
		}
		return r;
	}
}
