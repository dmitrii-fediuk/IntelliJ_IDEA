// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.project_path_macro;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class ProjectPathMacroCacheInvalidator implements ProjectCloseListener {
	/**
	 * - @used-by META-INF/plugin.xml
	 * - @used-by {@link ProjectCloseListener#projectClosed(Project)}
	 * @see ProjectCloseListener#projectClosed(Project)
	 */
	@Override
	public void projectClosed(@NotNull Project project) {
		MacroPathResolver.shared().evict(project.getProjectFilePath());
	}
}
