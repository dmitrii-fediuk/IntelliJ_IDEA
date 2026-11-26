// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.nested_project_root;
import com.intellij.openapi.project.Project;
import com.intellij.platform.backend.workspace.WorkspaceModel;
import com.intellij.platform.workspace.jps.entities.ContentRootEntity;
import com.intellij.platform.workspace.jps.entities.ContentRootEntityBuilder;
import com.intellij.platform.workspace.jps.entities.ContentRootEntityModifications;
import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder;
import com.intellij.platform.workspace.jps.entities.ModuleEntityModifications;
import com.intellij.platform.workspace.storage.EntityChange;
import com.intellij.platform.workspace.storage.EntitySource;
import com.intellij.platform.workspace.storage.ImmutableEntityStorage;
import com.intellij.platform.workspace.storage.MutableEntityStorage;
import com.intellij.platform.workspace.storage.VersionedStorageChange;
import com.intellij.platform.workspace.storage.WorkspaceEntity;
import com.intellij.platform.workspace.storage.impl.url.VirtualFileUrlManagerImpl;
import com.intellij.platform.workspace.storage.url.VirtualFileUrl;
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager;
import com.intellij.workspaceModel.ide.ProjectRootEntity;
import com.intellij.workspaceModel.ide.ProjectRootEntityModifications;
import com.intellij.workspaceModel.ide.ProjectRootEntitySource;
import df.idea.support.Assertions;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.channels.BufferOverflow;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.SharedFlowKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
public final class NestedProjectRootCleanerTest {
	private static final Assertions A = new Assertions();
	private static final EntitySource TEST_SOURCE = new EntitySource() {
		@Override
		public @Nullable VirtualFileUrl getVirtualFileUrl() {
			return null;
		}
	};
	public static void main(String[] args) throws Exception {
		testDisposedAndNullGuards();
		testEventRelevance();
		testPlainFlowFallback();
		testReflectiveAdapterDegradation();
		testResilienceAndCancellation();
		System.out.println("NESTED_PROJECT_ROOT_CLEANER_TESTS_PASSED (" + A.passedCount() + " assertions)");
	}
	private static void addModuleWithContent(
		MutableEntityStorage storage,
		VirtualFileUrlManager urlManager,
		String moduleName,
		String contentUrl
	) throws Exception {
		ContentRootEntityBuilder contentRoot = ContentRootEntityModifications.createContentRootEntity(
			createUrl(urlManager, contentUrl),
			Collections.emptyList(),
			TEST_SOURCE
		);
		ModuleEntityBuilder module = ModuleEntityModifications.createModuleEntity(
			moduleName,
			Collections.emptyList(),
			TEST_SOURCE,
			builder -> {
				builder.setContentRoots(List.of(contentRoot));
				return Unit.INSTANCE;
			}
		);
		storage.addEntity(module);
	}
	private static ProjectRootEntity addProjectRoot(
		MutableEntityStorage storage,
		VirtualFileUrlManager urlManager,
		String rootUrl
	) throws Exception {
		return storage.addEntity(
			ProjectRootEntityModifications.createProjectRootEntity(
				createUrl(urlManager, rootUrl),
				ProjectRootEntitySource.INSTANCE
			)
		);
	}
	private static VirtualFileUrl createUrl(VirtualFileUrlManager urlManager, String url) throws Exception {
		Method factory;
		try {
			factory = urlManager.getClass().getMethod("storeAndGet", String.class);
		}
		catch (NoSuchMethodException ignored) {
			factory = urlManager.getClass().getMethod("getOrCreateFromUrl", String.class);
		}
		return (VirtualFileUrl) factory.invoke(urlManager, url);
	}
	private static Project fakeProject(AtomicBoolean disposed) {
		return (Project) Proxy.newProxyInstance(
			NestedProjectRootCleanerTest.class.getClassLoader(),
			new Class<?>[]{Project.class},
			(proxy, method, args) -> {
				Object r = null;
				if ("isDisposed".equals(method.getName())) {
					r = disposed.get();
				}
				else if ("isDefault".equals(method.getName())) {
					r = false;
				}
				else if ("toString".equals(method.getName())) {
					r = "FakeProject";
				}
				return r;
			}
		);
	}
	private static VersionedStorageChange fakeStorageChange(
		ImmutableEntityStorage before,
		ImmutableEntityStorage after,
		Map<Class<?>, List<? extends EntityChange<?>>> changesByType
	) {
		return (VersionedStorageChange) Proxy.newProxyInstance(
			NestedProjectRootCleanerTest.class.getClassLoader(),
			new Class<?>[]{VersionedStorageChange.class},
			(proxy, method, args) -> {
				Object r = null;
				if ("getStorageBefore".equals(method.getName())) {
					r = before;
				}
				else if ("getStorageAfter".equals(method.getName())) {
					r = after;
				}
				else if ("getChangedReferences".equals(method.getName())) {
					r = Collections.emptyList();
				}
				else if ("getChanges".equals(method.getName())) {
					List<? extends EntityChange<?>> found = changesByType.get(args[0]);
					r = null != found ? found : Collections.emptyList();
				}
				return r;
			}
		);
	}
	private static WorkspaceModel fakeWorkspaceModel(
		AtomicReference<ImmutableEntityStorage> snapshotRef,
		Flow<VersionedStorageChange> eventLog,
		AtomicInteger updateCount,
		@Nullable Runnable onUpdated
	) {
		return (WorkspaceModel) Proxy.newProxyInstance(
			NestedProjectRootCleanerTest.class.getClassLoader(),
			new Class<?>[]{WorkspaceModel.class},
			(proxy, method, args) -> {
				Object r = null;
				if ("getCurrentSnapshot".equals(method.getName())) {
					r = snapshotRef.get();
				}
				else if ("getEventLog".equals(method.getName())) {
					r = eventLog;
				}
				else if ("update".equals(method.getName())) {
					updateCount.incrementAndGet();
					@SuppressWarnings("unchecked")
					Function1<MutableEntityStorage, Unit> updater = (Function1<MutableEntityStorage, Unit>) args[1];
					MutableEntityStorage mutable = MutableEntityStorage.from(snapshotRef.get());
					updater.invoke(mutable);
					snapshotRef.set(mutable.toSnapshot());
					if (null != onUpdated) {
						onUpdated.run();
					}
					r = Unit.INSTANCE;
				}
				return r;
			}
		);
	}
	private static List<String> listProjectRootUrls(ImmutableEntityStorage storage, ProjectRootEntityAdapter adapter) {
		List<String> r = new ArrayList<>();
		var it = storage.entities(adapter.entityClass()).iterator();
		while (it.hasNext()) {
			r.add(adapter.getRootUrl(it.next()));
		}
		Collections.sort(r);
		return r;
	}
	private static void testDisposedAndNullGuards() throws Exception {
		ProjectRootEntityAdapter adapter = ProjectRootEntityAdapter.Reflective.tryBind();
		A.assertTrue("reflective adapter binds to ProjectRootEntity", null != adapter);
		AtomicBoolean disposed = new AtomicBoolean(false);
		Project activeProject = fakeProject(disposed);
		Object nullModelResult = BuildersKt.runBlocking(
			EmptyCoroutineContext.INSTANCE,
			(scope, cont) -> NestedProjectRootCleanupActivity.observeAndClean(activeProject, null, adapter, false, cont)
		);
		A.assertEquals("null WorkspaceModel returns Unit cleanly", Unit.INSTANCE, nullModelResult);
		disposed.set(true);
		AtomicInteger updates = new AtomicInteger();
		AtomicReference<ImmutableEntityStorage> snap = new AtomicReference<>(MutableEntityStorage.create().toSnapshot());
		WorkspaceModel model = fakeWorkspaceModel(snap, FlowKt.flowOf(new VersionedStorageChange[0]), updates, null);
		Object disposedResult = BuildersKt.runBlocking(
			EmptyCoroutineContext.INSTANCE,
			(scope, cont) -> NestedProjectRootCleanupActivity.observeAndClean(activeProject, model, adapter, false, cont)
		);
		A.assertEquals("disposed project returns Unit without updating", Unit.INSTANCE, disposedResult);
		A.assertEquals("disposed project triggers zero updates", 0, updates.get());
	}
	private static void testEventRelevance() throws Exception {
		ProjectRootEntityAdapter adapter = ProjectRootEntityAdapter.Reflective.tryBind();
		A.assertTrue("adapter available for event relevance test", null != adapter);
		VirtualFileUrlManager urlManager = new VirtualFileUrlManagerImpl();
		MutableEntityStorage storage = MutableEntityStorage.create();
		addModuleWithContent(storage, urlManager, "outer", "file://C:/work/example.com");
		ProjectRootEntity rootEntity = addProjectRoot(storage, urlManager, "file://C:/work/example.com/.idea/p1");
		ContentRootEntity contentEntity = storage.entities(ContentRootEntity.class).iterator().next();
		ImmutableEntityStorage snap = storage.toSnapshot();
		VersionedStorageChange addedRoot = fakeStorageChange(
			snap,
			snap,
			Map.of(ProjectRootEntity.class, List.of(new EntityChange.Added<>(rootEntity)))
		);
		A.assertEquals("ProjectRootEntity Added is relevant", true, NestedProjectRootCleaner.hasRelevantChanges(addedRoot, adapter));
		VersionedStorageChange removedRoot = fakeStorageChange(
			snap,
			snap,
			Map.of(ProjectRootEntity.class, List.of(new EntityChange.Removed<>(rootEntity)))
		);
		A.assertEquals("ProjectRootEntity Removed is ignored (no loop)", false, NestedProjectRootCleaner.hasRelevantChanges(removedRoot, adapter));
		VersionedStorageChange addedContent = fakeStorageChange(
			snap,
			snap,
			Map.of(ContentRootEntity.class, List.of(new EntityChange.Added<>(contentEntity)))
		);
		A.assertEquals("ContentRootEntity Added is relevant", true, NestedProjectRootCleaner.hasRelevantChanges(addedContent, adapter));
		VersionedStorageChange removedContent = fakeStorageChange(
			snap,
			snap,
			Map.of(ContentRootEntity.class, List.of(new EntityChange.Removed<>(contentEntity)))
		);
		A.assertEquals("ContentRootEntity Removed is ignored", false, NestedProjectRootCleaner.hasRelevantChanges(removedContent, adapter));
		VersionedStorageChange emptyChange = fakeStorageChange(snap, snap, Collections.emptyMap());
		A.assertEquals("unrelated empty change is ignored", false, NestedProjectRootCleaner.hasRelevantChanges(emptyChange, adapter));
	}
	private static void testPlainFlowFallback() throws Exception {
		ProjectRootEntityAdapter adapter = ProjectRootEntityAdapter.Reflective.tryBind();
		A.assertTrue("adapter available for plain flow test", null != adapter);
		VirtualFileUrlManager urlManager = new VirtualFileUrlManagerImpl();
		MutableEntityStorage initial = MutableEntityStorage.create();
		addModuleWithContent(initial, urlManager, "outer", "file://C:/work/example.com");
		addProjectRoot(initial, urlManager, "file://C:/work/example.com/.idea/p1");
		addProjectRoot(initial, urlManager, "file://C:/work/external");
		ImmutableEntityStorage before = initial.toSnapshot();
		AtomicReference<ImmutableEntityStorage> snapRef = new AtomicReference<>(before);
		AtomicInteger updates = new AtomicInteger();
		Flow<VersionedStorageChange> plainFlow = FlowKt.flowOf(new VersionedStorageChange[0]);
		WorkspaceModel model = fakeWorkspaceModel(snapRef, plainFlow, updates, null);
		Project project = fakeProject(new AtomicBoolean(false));
		BuildersKt.runBlocking(
			EmptyCoroutineContext.INSTANCE,
			(scope, cont) -> NestedProjectRootCleanupActivity.observeAndClean(project, model, adapter, false, cont)
		);
		A.assertEquals("plain Flow onStart triggers single update", 1, updates.get());
		A.assertEquals(
			"plain Flow removes nested root and retains external root",
			List.of("file://C:/work/external"),
			listProjectRootUrls(snapRef.get(), adapter)
		);
	}
	private static void testReflectiveAdapterDegradation() {
		ClassLoader cl = NestedProjectRootCleanerTest.class.getClassLoader();
		A.assertEquals(
			"missing class degrades cleanly to null",
			null,
			ProjectRootEntityAdapter.Reflective.tryBind(cl, "com.example.MissingProjectRootEntity")
		);
		A.assertEquals(
			"non-WorkspaceEntity class degrades cleanly to null",
			null,
			ProjectRootEntityAdapter.Reflective.tryBind(cl, "java.lang.String")
		);
		A.assertEquals(
			"WorkspaceEntity without getRoot degrades cleanly to null",
			null,
			ProjectRootEntityAdapter.Reflective.tryBind(cl, ContentRootEntity.class.getName())
		);
	}
	private static void testResilienceAndCancellation() throws Exception {
		ProjectRootEntityAdapter realAdapter = ProjectRootEntityAdapter.Reflective.tryBind();
		A.assertTrue("real adapter available", null != realAdapter);
		AtomicBoolean shouldFail = new AtomicBoolean(true);
		ProjectRootEntityAdapter flakyAdapter = new ProjectRootEntityAdapter() {
			@Override
			public @NotNull Class<? extends WorkspaceEntity> entityClass() {
				return realAdapter.entityClass();
			}
			@Override
			public @Nullable String getRootUrl(@NotNull WorkspaceEntity entity) {
				if (shouldFail.get()) {
					throw new IllegalStateException("simulated transient failure");
				}
				return realAdapter.getRootUrl(entity);
			}
		};
		VirtualFileUrlManager urlManager = new VirtualFileUrlManagerImpl();
		MutableEntityStorage initial = MutableEntityStorage.create();
		addModuleWithContent(initial, urlManager, "outer", "file://C:/work/example.com");
		ProjectRootEntity nested = addProjectRoot(initial, urlManager, "file://C:/work/example.com/.idea/p1");
		AtomicReference<ImmutableEntityStorage> snapRef = new AtomicReference<>(initial.toSnapshot());
		MutableSharedFlow<VersionedStorageChange> sharedFlow = SharedFlowKt.MutableSharedFlow(
			0,
			4,
			BufferOverflow.SUSPEND
		);
		AtomicInteger updates = new AtomicInteger();
		CountDownLatch updatedLatch = new CountDownLatch(1);
		WorkspaceModel model = fakeWorkspaceModel(snapRef, FlowKt.asSharedFlow(sharedFlow), updates, updatedLatch::countDown);
		Project project = fakeProject(new AtomicBoolean(false));
		AtomicBoolean cancelPropagated = new AtomicBoolean(false);
		BuildersKt.runBlocking(
			EmptyCoroutineContext.INSTANCE,
			(scope, cont) -> {
				Job job = BuildersKt.launch(
					scope,
					Dispatchers.getDefault(),
					CoroutineStart.UNDISPATCHED,
					(childScope, childCont) -> NestedProjectRootCleanupActivity.observeAndClean(
						project,
						model,
						flakyAdapter,
						false,
						childCont
					)
				);
				job.invokeOnCompletion(cause -> {
					cancelPropagated.set(cause instanceof CancellationException);
					return Unit.INSTANCE;
				});
				try {
					A.assertEquals("initial transient failure does not crash collector", true, job.isActive());
					A.assertEquals("initial transient failure performed zero updates", 0, updates.get());
					shouldFail.set(false);
					VersionedStorageChange addedEvent = fakeStorageChange(
						snapRef.get(),
						snapRef.get(),
						Map.of(ProjectRootEntity.class, List.of(new EntityChange.Added<>(nested)))
					);
					boolean emitted = sharedFlow.tryEmit(addedEvent);
					A.assertEquals("sharedFlow accepted subsequent event", true, emitted);
					boolean reached = updatedLatch.await(5L, TimeUnit.SECONDS);
					A.assertEquals("collector processed recovery event within timeout", true, reached);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				finally {
					job.cancel(new CancellationException("project closed"));
				}
				return job.join(cont);
			}
		);
		A.assertEquals("collector stayed alive and cleaned on next event", 1, updates.get());
		A.assertEquals("nested root removed after recovery", Collections.emptyList(), listProjectRootUrls(snapRef.get(), realAdapter));
		A.assertEquals("coroutine cancellation propagates cleanly on project close", true, cancelPropagated.get());
	}
}
