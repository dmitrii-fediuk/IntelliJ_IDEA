// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.copy_reference;
import com.intellij.ide.actions.VirtualFileQualifiedNameProvider;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public final class ExclusionAwareVirtualFileQualifiedNameProvider implements VirtualFileQualifiedNameProvider {
	@FunctionalInterface
	interface ExcludeRootsResolver {
		@NotNull VirtualFile[] getExcludeRoots(@NotNull Module module);
	}
	@Override
	public @Nullable String getQualifiedName(@NotNull Project project, @NotNull VirtualFile virtualFile) {
		return resolveQualifiedName(
			ProjectFileIndex.getInstance(project),
			virtualFile,
			module -> ModuleRootManager.getInstance(module).getExcludeRoots()
		);
	}
	static @Nullable String resolveQualifiedName(
		@NotNull ProjectFileIndex index,
		@NotNull VirtualFile virtualFile,
		@NotNull ExcludeRootsResolver excludeRootsResolver
	) {
		String r = null;
		VirtualFile targetFile = (virtualFile instanceof VirtualFileWindow window) ? window.getDelegate() : virtualFile;
		Module module = index.getModuleForFile(targetFile, false);
		if (null != module) {
			VirtualFile sourceRoot = index.getSourceRootForFile(targetFile);
			if (null != sourceRoot && !sourceRoot.equals(targetFile)) {
				r = VfsUtilCore.getRelativePath(targetFile, sourceRoot, '/');
			}
			if (null == r) {
				VirtualFile contentRoot = index.getContentRootForFile(targetFile, false);
				if (null != contentRoot) {
					VirtualFile outerMostRoot = contentRoot;
					VirtualFile currentRoot = contentRoot;
					while (true) {
						VirtualFile parent = currentRoot.getParent();
						if (null == parent || parent.equals(currentRoot)) {
							break;
						}
						VirtualFile parentContentRoot = index.getContentRootForFile(parent, true);
						if (null == parentContentRoot || !VfsUtilCore.isAncestor(parentContentRoot, currentRoot, true)) {
							break;
						}
						Module parentModule = index.getModuleForFile(parent, true);
						if (null != parentModule && isExcludedInModule(parentModule, currentRoot, excludeRootsResolver)) {
							break;
						}
						outerMostRoot = parentContentRoot;
						currentRoot = parentContentRoot;
					}
					if (!outerMostRoot.equals(targetFile)) {
						r = VfsUtilCore.getRelativePath(targetFile, outerMostRoot, '/');
					}
					if (null == r) {
						r = targetFile.getPath();
					}
				}
			}
		}
		return r;
	}
	private static boolean isExcludedInModule(
		@NotNull Module module,
		@NotNull VirtualFile candidate,
		@NotNull ExcludeRootsResolver excludeRootsResolver
	) {
		boolean r = false;
		for (VirtualFile excludeRoot : excludeRootsResolver.getExcludeRoots(module)) {
			if (null != excludeRoot && VfsUtilCore.isAncestor(excludeRoot, candidate, false)) {
				r = true;
				break;
			}
		}
		return r;
	}
}
