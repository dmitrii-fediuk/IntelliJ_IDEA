// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
package df.idea.feature.pathmacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import org.jetbrains.annotations.NotNull;
public final class ProjectPathMacroCacheInvalidator implements ProjectCloseListener {
	@Override
	public void projectClosed(@NotNull Project project) {
		MacroPathResolver.shared().evict(project.getProjectFilePath());
	}
}
