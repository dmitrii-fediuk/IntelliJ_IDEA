// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
package df.idea.feature.projectroots;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
final class NestedProjectRootMatcher {
	private NestedProjectRootMatcher() {}
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
