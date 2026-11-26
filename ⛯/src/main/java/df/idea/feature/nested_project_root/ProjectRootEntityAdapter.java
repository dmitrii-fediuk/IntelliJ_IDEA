// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.nested_project_root;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.platform.workspace.storage.WorkspaceEntity;
import com.intellij.platform.workspace.storage.url.VirtualFileUrl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
interface ProjectRootEntityAdapter {
	/**
	 * - @used-by {@link IdeProjectRootIntegrationTest#main(String[])}
	 * - @used-by {@link IdeProjectRootIntegrationTest#sortedProjectRootUrls(ImmutableEntityStorage, ProjectRootEntityAdapter)}
	 * - @used-by {@link NestedProjectRootCleaner#findRedundantProjectRoots(EntityStorage, ProjectRootEntityAdapter, boolean)}
	 * - @used-by {@link NestedProjectRootCleaner#hasRelevantChanges(VersionedStorageChange, ProjectRootEntityAdapter)}
	 * - @used-by {@link NestedProjectRootCleanerTest#listProjectRootUrls(ImmutableEntityStorage, ProjectRootEntityAdapter)}
	 * - @used-by {@link NestedProjectRootCleanerTest#testResilienceAndCancellation()}
	 * - @used-by {@link ProjectRootEntityAdapter.Reflective#entityClass()}
	 */
	@NotNull Class<? extends WorkspaceEntity> entityClass();
	/**
	 * - @used-by {@link IdeProjectRootIntegrationTest#sortedProjectRootUrls(ImmutableEntityStorage, ProjectRootEntityAdapter)}
	 * - @used-by {@link NestedProjectRootCleaner#findRedundantProjectRoots(EntityStorage, ProjectRootEntityAdapter, boolean)}
	 * - @used-by {@link NestedProjectRootCleanerTest#listProjectRootUrls(ImmutableEntityStorage, ProjectRootEntityAdapter)}
	 * - @used-by {@link NestedProjectRootCleanerTest#testResilienceAndCancellation()}
	 * - @used-by {@link ProjectRootEntityAdapter.Reflective#getRootUrl(WorkspaceEntity)}
	 */
	@Nullable String getRootUrl(@NotNull WorkspaceEntity entity);
	final class Reflective implements ProjectRootEntityAdapter {
		static final String ENTITY_CLASS_NAME = "com.intellij.workspaceModel.ide.ProjectRootEntity";
		private static final Logger LOG = Logger.getInstance(Reflective.class);
		private final Class<? extends WorkspaceEntity> entityClass;
		private final Method getRoot;
		/**
		 * - @used-by {@link ProjectRootEntityAdapter.Reflective#tryBind(ClassLoader, String)}
		 */
		private Reflective(@NotNull Class<? extends WorkspaceEntity> entityClass, @NotNull Method getRoot) {
			this.entityClass = entityClass;
			this.getRoot = getRoot;
		}
		/**
		 * - @used-by {@link ProjectRootEntityAdapter#entityClass()}
		 * @see ProjectRootEntityAdapter#entityClass()
		 */
		@Override
		public @NotNull Class<? extends WorkspaceEntity> entityClass() {
			return entityClass;
		}
		/**
		 * - @used-by {@link ProjectRootEntityAdapter#getRootUrl(WorkspaceEntity)}
		 * @see ProjectRootEntityAdapter#getRootUrl(WorkspaceEntity)
		 */
		@Override
		public @Nullable String getRootUrl(@NotNull WorkspaceEntity entity) {
			String r = null;
			if (entityClass.isInstance(entity)) {
				Object rawRoot = invoke(getRoot, entity);
				if (rawRoot instanceof VirtualFileUrl virtualFileUrl) {
					r = virtualFileUrl.getUrl();
				}
			}
			return r;
		}
		/**
		 * - @used-by {@link IdeProjectRootIntegrationTest#main(String[])}
		 * - @used-by {@link NestedProjectRootCleanerTest#testDisposedAndNullGuards()}
		 * - @used-by {@link NestedProjectRootCleanerTest#testEventRelevance()}
		 * - @used-by {@link NestedProjectRootCleanerTest#testPlainFlowFallback()}
		 * - @used-by {@link NestedProjectRootCleanerTest#testResilienceAndCancellation()}
		 * - @used-by {@link NestedProjectRootCleanupActivity#execute(Project, Continuation)}
		 */
		static @Nullable ProjectRootEntityAdapter tryBind() {
			return tryBind(Reflective.class.getClassLoader(), ENTITY_CLASS_NAME);
		}
		/**
		 * - @used-by {@link NestedProjectRootCleanerTest#testReflectiveAdapterDegradation()}
		 * - @used-by {@link ProjectRootEntityAdapter.Reflective#tryBind()}
		 * - @uses com.intellij.workspaceModel.ide.ProjectRootEntity#getRoot()
		 */
		static @Nullable ProjectRootEntityAdapter tryBind(
			@NotNull ClassLoader classLoader,
			@NotNull String entityClassName
		) {
			ProjectRootEntityAdapter r = null;
			try {
				Class<?> rawClass = Class.forName(entityClassName, false, classLoader);
				if (WorkspaceEntity.class.isAssignableFrom(rawClass)) {
					Method getRootMethod = rawClass.getMethod("getRoot");
					if (VirtualFileUrl.class.isAssignableFrom(getRootMethod.getReturnType())) {
						@SuppressWarnings("unchecked")
						Class<? extends WorkspaceEntity> typedClass = (Class<? extends WorkspaceEntity>) rawClass;
						r = new Reflective(typedClass, getRootMethod);
					}
				}
			}
			catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
				LOG.debug("ProjectRootEntity API is unavailable in this IDE build: " + e.getMessage());
			}
			return r;
		}
		/**
		 * - @used-by {@link IdeProjectRootIntegrationTest#createUrl(VirtualFileUrlManager, String)}
		 * - @used-by {@link NestedProjectRootCleanerTest#createUrl(VirtualFileUrlManager, String)}
		 * - @used-by {@link ProjectRootEntityAdapter.Reflective#getRootUrl(WorkspaceEntity)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.IdeUpdateSourceIntegrationTest#verifyPlatformUpdateSourceContract(String)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.PluginUpdateSourceClient.Reflective#invoke(Method, Object, Object...)}
		 */
		private static @Nullable Object invoke(@NotNull Method method, @NotNull Object target) {
			try {
				return method.invoke(target);
			}
			catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof CancellationException cancellation) {
					throw cancellation;
				}
				if (cause instanceof Error error) {
					throw error;
				}
				if (cause instanceof RuntimeException runtimeException) {
					throw runtimeException;
				}
				throw new IllegalStateException("Unexpected checked exception invoking " + method.getName(), cause);
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot invoke " + method.getName(), e);
			}
		}
	}
}
