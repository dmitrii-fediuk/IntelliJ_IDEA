// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.project_path_macro;
import com.intellij.openapi.components.impl.ProjectWidePathMacroContributor;
import com.intellij.openapi.diagnostic.Logger;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class ProjectPropertiesPathMacroContributor implements ProjectWidePathMacroContributor {
	private static final Logger LOG = Logger.getInstance(ProjectPropertiesPathMacroContributor.class);
	@Override
	public @NotNull Map<String, String> getProjectPathMacros(@NotNull String projectFilePath) {
		Map<String, String> r = Map.of();
		try {
			r = MacroPathResolver.shared().resolveForProject(projectFilePath);
		}
		catch (RuntimeException e) {
			LOG.warn("Failed to resolve project path macros for '" + projectFilePath + "'", e);
		}
		return r;
	}
}
