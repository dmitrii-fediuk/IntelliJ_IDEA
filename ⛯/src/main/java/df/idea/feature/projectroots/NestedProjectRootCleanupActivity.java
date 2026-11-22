// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
package df.idea.feature.projectroots;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.platform.backend.workspace.WorkspaceModel;
import com.intellij.platform.workspace.storage.VersionedStorageChange;
import java.util.concurrent.CancellationException;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.SharedFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public final class NestedProjectRootCleanupActivity implements ProjectActivity {
	private static final Logger LOG = Logger.getInstance(NestedProjectRootCleanupActivity.class);
	@Override
	public @Nullable Object execute(
		@NotNull Project project,
		@NotNull Continuation<? super Unit> continuation
	) {
		Object r = Unit.INSTANCE;
		if (!project.isDisposed() && !project.isDefault()) {
			ProjectRootEntityAdapter adapter = ProjectRootEntityAdapter.Reflective.tryBind();
			if (null != adapter) {
				r = observeAndClean(
					project,
					WorkspaceModel.getInstance(project),
					adapter,
					SystemInfoRt.isFileSystemCaseSensitive,
					continuation
				);
			}
		}
		return r;
	}
	static @Nullable Object observeAndClean(
		@NotNull Project project,
		@Nullable WorkspaceModel workspaceModel,
		@NotNull ProjectRootEntityAdapter adapter,
		boolean caseSensitive,
		@NotNull Continuation<? super Unit> continuation
	) {
		Object r = Unit.INSTANCE;
		if (null != workspaceModel && !project.isDisposed()) {
			try {
				Flow<VersionedStorageChange> eventLog = workspaceModel.getEventLog();
				Flow<VersionedStorageChange> observed;
				if (eventLog instanceof SharedFlow<VersionedStorageChange> sharedFlow) {
					observed = FlowKt.onSubscription(
						sharedFlow,
						(collector, cont) -> safeReconcile(project, workspaceModel, adapter, caseSensitive, cont)
					);
				}
				else {
					observed = FlowKt.onStart(
						eventLog,
						(collector, cont) -> safeReconcile(project, workspaceModel, adapter, caseSensitive, cont)
					);
				}
				r = observed.collect(
					(change, emitCont) -> handleEvent(project, workspaceModel, adapter, caseSensitive, change, emitCont),
					continuation
				);
			}
			catch (CancellationException e) {
				throw e;
			}
			catch (LinkageError | RuntimeException e) {
				if (!project.isDisposed()) {
					LOG.warn("Skipped nested ProjectRootEntity cleanup due to incompatible IDE API", e);
				}
			}
		}
		return r;
	}
	private static @Nullable Object handleEvent(
		@NotNull Project project,
		@NotNull WorkspaceModel workspaceModel,
		@NotNull ProjectRootEntityAdapter adapter,
		boolean caseSensitive,
		@Nullable VersionedStorageChange change,
		@NotNull Continuation<? super Unit> continuation
	) {
		Object r = Unit.INSTANCE;
		if (null != change && !project.isDisposed()) {
			try {
				if (NestedProjectRootCleaner.hasRelevantChanges(change, adapter)) {
					r = safeReconcile(project, workspaceModel, adapter, caseSensitive, continuation);
				}
			}
			catch (CancellationException e) {
				throw e;
			}
			catch (LinkageError | RuntimeException e) {
				if (!project.isDisposed()) {
					LOG.warn("Failed to inspect WorkspaceModel change for nested ProjectRootEntity", e);
				}
			}
		}
		return r;
	}
	private static @Nullable Object safeReconcile(
		@NotNull Project project,
		@NotNull WorkspaceModel workspaceModel,
		@NotNull ProjectRootEntityAdapter adapter,
		boolean caseSensitive,
		@NotNull Continuation<? super Unit> continuation
	) {
		Object r = Unit.INSTANCE;
		if (!project.isDisposed()) {
			try {
				r = NestedProjectRootCleaner.reconcile(workspaceModel, adapter, caseSensitive, continuation);
			}
			catch (CancellationException e) {
				throw e;
			}
			catch (LinkageError | RuntimeException e) {
				if (!project.isDisposed()) {
					LOG.warn("Failed to reconcile nested ProjectRootEntity instances", e);
				}
			}
		}
		return r;
	}
}
