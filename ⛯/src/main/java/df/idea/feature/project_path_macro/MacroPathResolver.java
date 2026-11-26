// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.project_path_macro;
import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class MacroPathResolver {
	private static final Logger LOG = Logger.getInstance(MacroPathResolver.class);
	private static final Pattern VAR_REF_PATTERN = Pattern.compile(
		"(?:\\$\\{([A-Za-z0-9_.-]+)\\}|\\$([A-Za-z0-9_.-]+)\\$)"
	);
	private static final MacroPathResolver SHARED = new MacroPathResolver(LOG::warn);
	private final Consumer<String> warningSink;
	private final ConcurrentHashMap<String, CachedMacros> cache = new ConcurrentHashMap<>();
	/**
	 * - @used-by {@link IdePathMacroIntegrationTest#testMultipleNestedVariablesChooseLongestMatchOnSave()}
	 * - @used-by {@link MacroPathResolverTest#testCacheHitInvalidationAndEviction()}
	 * - @used-by {@link MacroPathResolverTest#testCrossVariableCompositionAndForwardRefs()}
	 * - @used-by {@link MacroPathResolverTest#testCycleAndUndefinedReferenceHandling()}
	 * - @used-by {@link MacroPathResolverTest#testDirectoryNamedPropertiesIgnoredSafely()}
	 * - @used-by {@link MacroPathResolverTest#testLayeredOuterAndInnerInheritanceOverrideAndUnset()}
	 * - @used-by {@link MacroPathResolverTest#testRelativeAndAbsoluteResolutionWithUnicode()}
	 */
	MacroPathResolver(@NotNull Consumer<String> warningSink) {
		this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
	}
	/**
	 * - @used-by {@link IdePathMacroIntegrationTest#testNestedImlAndProjectXmlRoundTrip()}
	 * - @used-by {@link MacroPathResolverTest#testCacheHitInvalidationAndEviction()}
	 * - @used-by {@link ProjectPathMacroCacheInvalidator#projectClosed(Project)}
	 */
	public void evict(@Nullable String projectFilePath) {
		if (null != projectFilePath && !projectFilePath.isBlank()) {
			cache.remove(normalizeCacheKey(projectFilePath));
		}
	}
	/**
	 * - @used-by {@link IdePathMacroIntegrationTest#testMultipleNestedVariablesChooseLongestMatchOnSave()}
	 * - @used-by {@link MacroPathResolverTest#testCacheHitInvalidationAndEviction()}
	 * - @used-by {@link MacroPathResolverTest#testCrossVariableCompositionAndForwardRefs()}
	 * - @used-by {@link MacroPathResolverTest#testCycleAndUndefinedReferenceHandling()}
	 * - @used-by {@link MacroPathResolverTest#testDirectoryNamedPropertiesIgnoredSafely()}
	 * - @used-by {@link MacroPathResolverTest#testLayeredOuterAndInnerInheritanceOverrideAndUnset()}
	 * - @used-by {@link MacroPathResolverTest#testRelativeAndAbsoluteResolutionWithUnicode()}
	 * - @used-by {@link ProjectPropertiesPathMacroContributor#getProjectPathMacros(String)}
	 */
	public @NotNull Map<String, String> resolveForProject(@Nullable String projectFilePath) {
		Map<String, String> r = Map.of();
		if (null != projectFilePath && !projectFilePath.isBlank()) {
			String cacheKey = normalizeCacheKey(projectFilePath);
			List<Path> candidates = MacroPropertiesLocator.findCandidateFiles(projectFilePath);
			if (candidates.isEmpty()) {
				cache.remove(cacheKey);
			}
			else {
				List<FileStamp> currentStamps = captureStamps(candidates);
				CachedMacros existing = cache.get(cacheKey);
				if (null != existing && existing.stamps().equals(currentStamps)) {
					r = existing.macros();
				}
				else {
					r = loadAndResolve(currentStamps);
					cache.put(cacheKey, new CachedMacros(currentStamps, r));
				}
			}
		}
		return r;
	}
	/**
	 * - @used-by {@link IdePathMacroIntegrationTest#testNestedImlAndProjectXmlRoundTrip()}
	 * - @used-by {@link ProjectPathMacroCacheInvalidator#projectClosed(Project)}
	 * - @used-by {@link ProjectPropertiesPathMacroContributor#getProjectPathMacros(String)}
	 */
	public static @NotNull MacroPathResolver shared() {
		return SHARED;
	}
	/**
	 * - @used-by {@link MacroPathResolverTest#testCacheHitInvalidationAndEviction()}
	 */
	int cacheSize() {
		return cache.size();
	}
	/**
	 * - @used-by {@link MacroPathResolver#normalizeMacroPath(String, Path, Path, String)}
	 */
	static @NotNull String trimTrailingSlash(@NotNull String path) {
		String r = path;
		while (1 < r.length() && r.endsWith("/")) {
			if (3 == r.length() && Character.isLetter(r.charAt(0)) && ':' == r.charAt(1)) {
				break;
			}
			r = r.substring(0, r.length() - 1);
		}
		return r;
	}
	/**
	 * - @used-by {@link MacroPathResolver#resolveForProject(String)}
	 */
	private static @NotNull List<FileStamp> captureStamps(@NotNull List<Path> candidates) {
		List<FileStamp> stamps = new ArrayList<>(candidates.size());
		for (Path candidate : candidates) {
			stamps.add(FileStamp.capture(candidate));
		}
		return List.copyOf(stamps);
	}
	/**
	 * - @used-by {@link MacroPathResolver#resolveKey(String, Map, Map, Set, Set)}
	 */
	private @Nullable String expandReferences(
		@NotNull RawEntry entry,
		@NotNull Map<String, RawEntry> rawEntries,
		@NotNull Map<String, String> resolvedValues,
		@NotNull Set<String> failedKeys,
		@NotNull Set<String> visiting
	) {
		String r;
		Matcher matcher = VAR_REF_PATTERN.matcher(entry.rawValue());
		if (!matcher.find()) {
			r = entry.rawValue();
		}
		else {
			StringBuilder sb = new StringBuilder();
			boolean ok = true;
			matcher.reset();
			while (matcher.find()) {
				String refName = null != matcher.group(1) ? matcher.group(1) : matcher.group(2);
				if (!rawEntries.containsKey(refName)) {
					warningSink.accept(
						entry.sourceFile() + ": macro '" + entry.key() + "' references undefined macro '" + refName + "'"
					);
					ok = false;
					break;
				}
				String refValue = resolveKey(refName, rawEntries, resolvedValues, failedKeys, visiting);
				if (null == refValue) {
					ok = false;
					break;
				}
				matcher.appendReplacement(sb, Matcher.quoteReplacement(refValue));
			}
			if (!ok) {
				r = null;
			}
			else {
				matcher.appendTail(sb);
				r = sb.toString();
			}
		}
		return r;
	}
	/**
	 * - @used-by {@link MacroPathResolver#resolveForProject(String)}
	 */
	private @NotNull Map<String, String> loadAndResolve(@NotNull List<FileStamp> stamps) {
		Map<String, String> r = Map.of();
		Map<String, RawEntry> mergedRaw = new LinkedHashMap<>();
		for (FileStamp stamp : stamps) {
			if (!stamp.exists()) {
				continue;
			}
			Path file = stamp.path();
			Path baseDir = file.getParent();
			if (null == baseDir) {
				continue;
			}
			try {
				MacroPropertiesParser.ParseResult parsed = MacroPropertiesParser.parseFile(file);
				for (String warning : parsed.warnings()) {
					warningSink.accept(warning);
				}
				for (Map.Entry<String, String> entry : parsed.entries().entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					if (value.isEmpty()) {
						mergedRaw.remove(key);
					}
					else {
						mergedRaw.put(key, new RawEntry(key, value, baseDir, file));
					}
				}
			}
			catch (IOException e) {
				warningSink.accept("Failed to read macro properties file '" + file + "': " + e.getMessage());
			}
		}
		if (!mergedRaw.isEmpty()) {
			Map<String, String> resolvedValues = new HashMap<>();
			Set<String> failedKeys = new LinkedHashSet<>();
			Map<String, String> orderedResult = new LinkedHashMap<>();
			for (String key : mergedRaw.keySet()) {
				String resolved = resolveKey(key, mergedRaw, resolvedValues, failedKeys, new LinkedHashSet<>());
				if (null != resolved && !resolved.isEmpty()) {
					orderedResult.put(key, resolved);
				}
			}
			if (!orderedResult.isEmpty()) {
				r = Collections.unmodifiableMap(orderedResult);
			}
		}
		return r;
	}
	/**
	 * - @used-by {@link MacroPathResolver#evict(String)}
	 * - @used-by {@link MacroPathResolver#resolveForProject(String)}
	 */
	private static @NotNull String normalizeCacheKey(@NotNull String projectFilePath) {
		return projectFilePath.replace('\\', '/');
	}
	/**
	 * - @used-by {@link MacroPathResolver#resolveKey(String, Map, Map, Set, Set)}
	 */
	private @Nullable String normalizeMacroPath(
		@NotNull String rawPath,
		@NotNull Path baseDir,
		@NotNull Path sourceFile,
		@NotNull String key
	) {
		String r = null;
		try {
			Path parsed = Path.of(rawPath);
			Path resolved = parsed.isAbsolute()
				? parsed.toAbsolutePath().normalize()
				: baseDir.resolve(parsed).toAbsolutePath().normalize();
			r = trimTrailingSlash(resolved.toString().replace('\\', '/'));
		}
		catch (InvalidPathException e) {
			warningSink.accept(
				sourceFile + ": macro '" + key + "' has invalid path '" + rawPath + "' (" + e.getMessage() + ")"
			);
		}
		return r;
	}
	/**
	 * - @used-by {@link MacroPathResolver#expandReferences(RawEntry, Map, Map, Set, Set)}
	 * - @used-by {@link MacroPathResolver#loadAndResolve(List)}
	 */
	private @Nullable String resolveKey(
		@NotNull String key,
		@NotNull Map<String, RawEntry> rawEntries,
		@NotNull Map<String, String> resolvedValues,
		@NotNull Set<String> failedKeys,
		@NotNull Set<String> visiting
	) {
		String r = null;
		if (resolvedValues.containsKey(key)) {
			r = resolvedValues.get(key);
		}
		else if (!failedKeys.contains(key)) {
			RawEntry entry = rawEntries.get(key);
			if (null != entry) {
				if (!visiting.add(key)) {
					warningSink.accept(
						entry.sourceFile() + ": circular macro reference involving '" + key + "' (" + String.join(" -> ", visiting) + " -> " + key + ")"
					);
					failedKeys.add(key);
				}
				else {
					try {
						String expanded = expandReferences(entry, rawEntries, resolvedValues, failedKeys, visiting);
						if (null == expanded || expanded.isBlank()) {
							failedKeys.add(key);
						}
						else {
							String normalizedPath = normalizeMacroPath(expanded, entry.baseDir(), entry.sourceFile(), key);
							if (null == normalizedPath || normalizedPath.isEmpty()) {
								failedKeys.add(key);
							}
							else {
								resolvedValues.put(key, normalizedPath);
								r = normalizedPath;
							}
						}
					}
					finally {
						visiting.remove(key);
					}
				}
			}
		}
		return r;
	}
	private record CachedMacros(
		@NotNull List<FileStamp> stamps,
		@NotNull Map<String, String> macros
	) {}
	private record FileStamp(
		@NotNull Path path,
		boolean exists,
		@Nullable FileTime lastModifiedTime,
		long size
	) {
		/**
		 * - @used-by {@link MacroPathResolver#captureStamps(List)}
		 */
		static @NotNull FileStamp capture(@NotNull Path path) {
			FileStamp r = new FileStamp(path, false, null, -1L);
			try {
				BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
				if (attrs.isRegularFile()) {
					r = new FileStamp(path, true, attrs.lastModifiedTime(), attrs.size());
				}
			}
			catch (IOException ignored) {
			}
			return r;
		}
	}
	private record RawEntry(
		@NotNull String key,
		@NotNull String rawValue,
		@NotNull Path baseDir,
		@NotNull Path sourceFile
	) {}
}
