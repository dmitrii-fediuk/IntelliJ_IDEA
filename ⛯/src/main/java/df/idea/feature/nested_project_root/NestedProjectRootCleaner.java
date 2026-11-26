// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.nested_project_root;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.platform.backend.workspace.WorkspaceModel;
import com.intellij.platform.workspace.jps.entities.ContentRootEntity;
import com.intellij.platform.workspace.storage.EntityChange;
import com.intellij.platform.workspace.storage.EntityStorage;
import com.intellij.platform.workspace.storage.MutableEntityStorage;
import com.intellij.platform.workspace.storage.VersionedStorageChange;
import com.intellij.platform.workspace.storage.WorkspaceEntity;
import com.intellij.platform.workspace.storage.url.VirtualFileUrl;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
final class NestedProjectRootCleaner {
	static final String UPDATE_DESCRIPTION = "Remove nested ProjectRootEntity covered by module content roots";
	private static final Logger LOG = Logger.getInstance(NestedProjectRootCleaner.class);
	private NestedProjectRootCleaner() {}
	static @NotNull Set<String> collectContentRootUrls(@NotNull EntityStorage storage) {
		Set<String> r = new LinkedHashSet<>();
		Iterator<ContentRootEntity> iterator = storage.entities(ContentRootEntity.class).iterator();
		while (iterator.hasNext()) {
			ContentRootEntity contentRoot = iterator.next();
			VirtualFileUrl url = contentRoot.getUrl();
			if (null != url) {
				String normalized = NestedProjectRootMatcher.normalizeUrl(url.getUrl());
				if (null != normalized) {
					r.add(normalized);
				}
			}
		}
		return r;
	}
	static @NotNull List<WorkspaceEntity> findRedundantProjectRoots(
		@NotNull EntityStorage storage,
		@NotNull ProjectRootEntityAdapter adapter,
		boolean caseSensitive
	) {
		List<WorkspaceEntity> r = new ArrayList<>();
		Set<String> contentRootUrls = collectContentRootUrls(storage);
		if (!contentRootUrls.isEmpty()) {
			Iterator<? extends WorkspaceEntity> iterator = storage.entities(adapter.entityClass()).iterator();
			while (iterator.hasNext()) {
				WorkspaceEntity entity = iterator.next();
				String rootUrl = adapter.getRootUrl(entity);
				if (NestedProjectRootMatcher.isStrictlyCovered(rootUrl, contentRootUrls, caseSensitive)) {
					r.add(entity);
				}
			}
		}
		return r;
	}
	static boolean hasRelevantChanges(
		@NotNull VersionedStorageChange change,
		@NotNull ProjectRootEntityAdapter adapter
	) {
		return hasAddedOrReplaced(change.getChanges(adapter.entityClass()))
			|| hasAddedOrReplaced(change.getChanges(ContentRootEntity.class));
	}
	static @Nullable Object reconcile(
		@NotNull WorkspaceModel workspaceModel,
		@NotNull ProjectRootEntityAdapter adapter,
		boolean caseSensitive,
		@NotNull Continuation<? super Unit> continuation
	) {
		Object r = Unit.INSTANCE;
		if (!findRedundantProjectRoots(workspaceModel.getCurrentSnapshot(), adapter, caseSensitive).isEmpty()) {
			r = workspaceModel.update(
				UPDATE_DESCRIPTION,
				storage -> {
					try {
						int removed = removeRedundantProjectRoots(storage, adapter, caseSensitive);
						if (0 < removed) {
							LOG.info(
								"Removed " + removed
								+ " nested ProjectRootEntity instance(s) covered by module content roots."
							);
						}
					}
					catch (CancellationException e) {
						throw e;
					}
					catch (LinkageError | RuntimeException e) {
						LOG.warn("Failed to remove nested ProjectRootEntity instances from storage", e);
					}
					return Unit.INSTANCE;
				},
				continuation
			);
		}
		return r;
	}
	static int removeRedundantProjectRoots(
		@NotNull MutableEntityStorage storage,
		@NotNull ProjectRootEntityAdapter adapter,
		boolean caseSensitive
	) {
		int r = 0;
		List<WorkspaceEntity> redundant = findRedundantProjectRoots(storage, adapter, caseSensitive);
		for (WorkspaceEntity entity : redundant) {
			if (storage.removeEntity(entity)) {
				r++;
			}
		}
		return r;
	}
	private static boolean hasAddedOrReplaced(@NotNull List<? extends EntityChange<?>> changes) {
		boolean r = false;
		for (EntityChange<?> entityChange : changes) {
			if (null != entityChange.getNewEntity()) {
				r = true;
				break;
			}
		}
		return r;
	}
}
