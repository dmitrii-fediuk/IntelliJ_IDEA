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
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntityModifications;
import com.intellij.platform.workspace.jps.entities.ModuleEntity;
import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder;
import com.intellij.platform.workspace.jps.entities.ModuleEntityModifications;
import com.intellij.platform.workspace.storage.EntityChange;
import com.intellij.platform.workspace.storage.EntitySource;
import com.intellij.platform.workspace.storage.ImmutableEntityStorage;
import com.intellij.platform.workspace.storage.MutableEntityStorage;
import com.intellij.platform.workspace.storage.VersionedStorageChange;
import com.intellij.platform.workspace.storage.impl.url.VirtualFileUrlManagerImpl;
import com.intellij.platform.workspace.storage.url.VirtualFileUrl;
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager;
import com.intellij.workspaceModel.ide.ProjectRootEntity;
import com.intellij.workspaceModel.ide.ProjectRootEntityModifications;
import com.intellij.workspaceModel.ide.ProjectRootEntitySource;
import df.idea.support.Assertions;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.SharedFlowKt;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Remove nested ProjectRootEntity instances covered by module content roots":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
public final class IdeProjectRootIntegrationTest {
	private static final Assertions A = new Assertions();
	private static final EntitySource TEST_SOURCE = new EntitySource() {
		@Override
		public @Nullable VirtualFileUrl getVirtualFileUrl() {
			return null;
		}
	};
	public static void main(String[] args) throws Exception {
		ProjectRootEntityAdapter adapter = ProjectRootEntityAdapter.Reflective.tryBind();
		A.assertTrue("ProjectRootEntityAdapter binds in active IDE runtime", null != adapter);
		A.assertEquals("bound entity class is ProjectRootEntity", ProjectRootEntity.class, adapter.entityClass());
		verifyModulelessProjectUntouched(adapter);
		verifyMultiProjectStorageCleanup(adapter);
		verifyNoHardInternalReferenceInProductionBytecode();
		verifyPlatformContributorContract();
		verifyReactiveEventLogCycle(adapter);
		System.out.println("IDE_PROJECT_ROOT_INTEGRATION_PASSED (" + A.passedCount() + " assertions)");
	}
	private static ContentRootEntity addModuleWithContent(
		MutableEntityStorage storage,
		VirtualFileUrlManager urlManager,
		String moduleName,
		String contentUrl,
		@Nullable String excludedUrl
	) throws Exception {
		ContentRootEntityBuilder contentRoot = ContentRootEntityModifications.createContentRootEntity(
			createUrl(urlManager, contentUrl),
			Collections.emptyList(),
			TEST_SOURCE,
			builder -> {
				if (null != excludedUrl) {
					try {
						builder.setExcludedUrls(List.of(
							ExcludeUrlEntityModifications.createExcludeUrlEntity(
								createUrl(urlManager, excludedUrl),
								TEST_SOURCE
							)
						));
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				return Unit.INSTANCE;
			}
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
		ModuleEntity added = storage.addEntity(module);
		return added.getContentRoots().get(0);
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
	private static Project fakeProject() {
		return (Project) Proxy.newProxyInstance(
			IdeProjectRootIntegrationTest.class.getClassLoader(),
			new Class<?>[]{Project.class},
			(proxy, method, args) -> {
				Object r = null;
				if ("isDisposed".equals(method.getName()) || "isDefault".equals(method.getName())) {
					r = false;
				}
				else if ("toString".equals(method.getName())) {
					r = "IdeIntegrationFakeProject";
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
			IdeProjectRootIntegrationTest.class.getClassLoader(),
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
	private static List<String> sortedContentRootUrls(ImmutableEntityStorage storage) {
		List<String> r = new ArrayList<>();
		var it = storage.entities(ContentRootEntity.class).iterator();
		while (it.hasNext()) {
			r.add(it.next().getUrl().getUrl());
		}
		Collections.sort(r);
		return r;
	}
	private static List<String> sortedProjectRootUrls(ImmutableEntityStorage storage, ProjectRootEntityAdapter adapter) {
		List<String> r = new ArrayList<>();
		var it = storage.entities(adapter.entityClass()).iterator();
		while (it.hasNext()) {
			r.add(adapter.getRootUrl(it.next()));
		}
		Collections.sort(r);
		return r;
	}
	private static void verifyModulelessProjectUntouched(ProjectRootEntityAdapter adapter) throws Exception {
		VirtualFileUrlManager urlManager = new VirtualFileUrlManagerImpl();
		MutableEntityStorage storage = MutableEntityStorage.create();
		addProjectRoot(storage, urlManager, "file://C:/work/plain-notes");
		int removed = NestedProjectRootCleaner.removeRedundantProjectRoots(storage, adapter, false);
		A.assertEquals("moduleless project removes 0 ProjectRootEntity instances", 0, removed);
		A.assertEquals(
			"moduleless project retains its ProjectRootEntity",
			List.of("file://C:/work/plain-notes"),
			sortedProjectRootUrls(storage.toSnapshot(), adapter)
		);
	}
	private static void verifyMultiProjectStorageCleanup(ProjectRootEntityAdapter adapter) throws Exception {
		VirtualFileUrlManager urlManager = new VirtualFileUrlManagerImpl();
		MutableEntityStorage storage = MutableEntityStorage.create();
		addModuleWithContent(storage, urlManager, "_", "file://C:/work/example.com", "file://C:/work/example.com/nested");
		addModuleWithContent(storage, urlManager, "App", "file://C:/work/example.com/nested/app", null);
		addProjectRoot(storage, urlManager, "file://C:/work/example.com/.idea/example.com--1");
		addProjectRoot(storage, urlManager, "file://C:/work/standalone");
		addProjectRoot(storage, urlManager, "file://C:/work/example.com");
		addProjectRoot(storage, urlManager, "file://C:/work/example.com-other");
		addProjectRoot(storage, urlManager, "file://C:/work/example.com/nested/app");
		int removed = NestedProjectRootCleaner.removeRedundantProjectRoots(storage, adapter, false);
		A.assertEquals("removes exactly 2 strictly nested ProjectRootEntity instances", 2, removed);
		ImmutableEntityStorage after = storage.toSnapshot();
		A.assertEquals(
			"retains equal, sibling-prefix, and disjoint ProjectRootEntity instances",
			List.of(
				"file://C:/work/example.com",
				"file://C:/work/example.com-other",
				"file://C:/work/standalone"
			),
			sortedProjectRootUrls(after, adapter)
		);
		A.assertEquals(
			"preserves both outer and nested module ContentRootEntity instances",
			List.of(
				"file://C:/work/example.com",
				"file://C:/work/example.com/nested/app"
			),
			sortedContentRootUrls(after)
		);
		int secondPass = NestedProjectRootCleaner.removeRedundantProjectRoots(storage, adapter, false);
		A.assertEquals("second cleanup pass is idempotent (0 removed)", 0, secondPass);
	}
	private static void verifyNoHardInternalReferenceInProductionBytecode() throws Exception {
		ClassLoader cl = NestedProjectRootCleanupActivity.class.getClassLoader();
		// 2026-11-22
		// "Standardize df.idea feature package names with snake_case word separators":
		// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
		String[] classes = {
			"df/idea/feature/nested_project_root/NestedProjectRootMatcher.class",
			"df/idea/feature/nested_project_root/ProjectRootEntityAdapter.class",
			"df/idea/feature/nested_project_root/ProjectRootEntityAdapter$Reflective.class",
			"df/idea/feature/nested_project_root/NestedProjectRootCleaner.class",
			"df/idea/feature/nested_project_root/NestedProjectRootCleanupActivity.class"
		};
		for (String resource : classes) {
			try (InputStream is = cl.getResourceAsStream(resource)) {
				A.assertTrue("production class bytecode readable: " + resource, null != is);
				String bytes = new String(is.readAllBytes(), StandardCharsets.ISO_8859_1);
				A.assertEquals(
					"no direct JVM descriptor reference to @Internal ProjectRootEntity in " + resource,
					false,
					bytes.contains("Lcom/intellij/workspaceModel/ide/ProjectRootEntity;")
				);
			}
		}
	}
	private static void verifyPlatformContributorContract() throws Exception {
		ClassLoader cl = IdeProjectRootIntegrationTest.class.getClassLoader();
		Class<?> contributorClass = Class.forName(
			"com.intellij.workspaceModel.ide.ProjectRootEntityWorkspaceFileIndexContributor",
			false,
			cl
		);
		Object contributor = contributorClass.getDeclaredConstructor().newInstance();
		Object entityClass = contributorClass.getMethod("getEntityClass").invoke(contributor);
		A.assertEquals(
			"ProjectRootEntityWorkspaceFileIndexContributor targets ProjectRootEntity",
			ProjectRootEntity.class,
			entityClass
		);
		Class<?> synchronizerClass = Class.forName(
			"com.intellij.openapi.project.impl.ProjectRootsSynchronizer",
			false,
			cl
		);
		A.assertTrue(
			"ProjectRootsSynchronizer implements ProjectActivity",
			com.intellij.openapi.startup.ProjectActivity.class.isAssignableFrom(synchronizerClass)
		);
	}
	private static void verifyReactiveEventLogCycle(ProjectRootEntityAdapter adapter) throws Exception {
		VirtualFileUrlManager urlManager = new VirtualFileUrlManagerImpl();
		MutableEntityStorage initial = MutableEntityStorage.create();
		addModuleWithContent(initial, urlManager, "_", "file://C:/work/example.com", null);
		addProjectRoot(initial, urlManager, "file://C:/work/example.com/.idea/example.com--1");
		addProjectRoot(initial, urlManager, "file://C:/work/late-repo/.idea/late-repo--1");
		AtomicReference<ImmutableEntityStorage> snapRef = new AtomicReference<>(initial.toSnapshot());
		MutableSharedFlow<VersionedStorageChange> updatesFlow = SharedFlowKt.MutableSharedFlow(
			1,
			8,
			BufferOverflow.SUSPEND
		);
		AtomicInteger updateCount = new AtomicInteger();
		List<String> updateDescriptions = Collections.synchronizedList(new ArrayList<>());
		AtomicReference<CountDownLatch> updateLatchRef = new AtomicReference<>(new CountDownLatch(1));
		WorkspaceModel workspaceModel = (WorkspaceModel) Proxy.newProxyInstance(
			IdeProjectRootIntegrationTest.class.getClassLoader(),
			new Class<?>[]{WorkspaceModel.class},
			(proxy, method, args) -> {
				Object r = null;
				if ("getCurrentSnapshot".equals(method.getName())) {
					r = snapRef.get();
				}
				else if ("getEventLog".equals(method.getName())) {
					r = FlowKt.asSharedFlow(updatesFlow);
				}
				else if ("update".equals(method.getName())) {
					updateCount.incrementAndGet();
					updateDescriptions.add((String) args[0]);
					@SuppressWarnings("unchecked")
					Function1<MutableEntityStorage, Unit> updater = (Function1<MutableEntityStorage, Unit>) args[1];
					ImmutableEntityStorage before = snapRef.get();
					List<ProjectRootEntity> beforeRoots = new ArrayList<>();
					before.entities(ProjectRootEntity.class).iterator().forEachRemaining(beforeRoots::add);
					MutableEntityStorage mutable = MutableEntityStorage.from(before);
					updater.invoke(mutable);
					ImmutableEntityStorage after = mutable.toSnapshot();
					snapRef.set(after);
					List<String> afterUrls = sortedProjectRootUrls(after, adapter);
					List<EntityChange<ProjectRootEntity>> removals = new ArrayList<>();
					for (ProjectRootEntity oldRoot : beforeRoots) {
						if (!afterUrls.contains(oldRoot.getRoot().getUrl())) {
							removals.add(new EntityChange.Removed<>(oldRoot));
						}
					}
					if (!removals.isEmpty()) {
						updatesFlow.tryEmit(fakeStorageChange(before, after, Map.of(ProjectRootEntity.class, removals)));
					}
					updateLatchRef.get().countDown();
					r = Unit.INSTANCE;
				}
				return r;
			}
		);
		Project project = fakeProject();
		BuildersKt.runBlocking(
			EmptyCoroutineContext.INSTANCE,
			(scope, cont) -> {
				Job job = BuildersKt.launch(
					scope,
					Dispatchers.getDefault(),
					CoroutineStart.UNDISPATCHED,
					(childScope, childCont) -> NestedProjectRootCleanupActivity.observeAndClean(
						project,
						workspaceModel,
						adapter,
						false,
						childCont
					)
				);
				try {
					A.assertEquals("initial onSubscription cleanup completed", true, updateLatchRef.get().await(5L, TimeUnit.SECONDS));
					A.assertEquals("initial onSubscription cleanup ran once", 1, updateCount.get());
					A.assertEquals(
						"initial cleanup removes nested root and preserves uncovered late-repo root",
						List.of("file://C:/work/late-repo/.idea/late-repo--1"),
						sortedProjectRootUrls(snapRef.get(), adapter)
					);
					CountDownLatch reAddLatch = new CountDownLatch(1);
					updateLatchRef.set(reAddLatch);
					ImmutableEntityStorage beforeReAdd = snapRef.get();
					MutableEntityStorage reAddBuilder = MutableEntityStorage.from(beforeReAdd);
					ProjectRootEntity reAdded = addProjectRoot(
						reAddBuilder,
						urlManager,
						"file://C:/work/example.com/.idea/example.com--1"
					);
					ImmutableEntityStorage afterReAdd = reAddBuilder.toSnapshot();
					snapRef.set(afterReAdd);
					updatesFlow.tryEmit(
						fakeStorageChange(
							beforeReAdd,
							afterReAdd,
							Map.of(ProjectRootEntity.class, List.of(new EntityChange.Added<>(reAdded)))
						)
					);
					A.assertEquals("re-added ProjectRootEntity cleaned asynchronously", true, reAddLatch.await(5L, TimeUnit.SECONDS));
					A.assertEquals("re-added ProjectRootEntity cleaned immediately with no feedback loop", 2, updateCount.get());
					A.assertEquals(
						"re-added nested root removed again",
						List.of("file://C:/work/late-repo/.idea/late-repo--1"),
						sortedProjectRootUrls(snapRef.get(), adapter)
					);
					CountDownLatch lateJpsLatch = new CountDownLatch(1);
					updateLatchRef.set(lateJpsLatch);
					ImmutableEntityStorage beforeLateJps = snapRef.get();
					MutableEntityStorage lateJpsBuilder = MutableEntityStorage.from(beforeLateJps);
					ContentRootEntity lateContent = addModuleWithContent(
						lateJpsBuilder,
						urlManager,
						"late-outer",
						"file://C:/work/late-repo",
						null
					);
					ImmutableEntityStorage afterLateJps = lateJpsBuilder.toSnapshot();
					snapRef.set(afterLateJps);
					updatesFlow.tryEmit(
						fakeStorageChange(
							beforeLateJps,
							afterLateJps,
							Map.of(ContentRootEntity.class, List.of(new EntityChange.Added<>(lateContent)))
						)
					);
					A.assertEquals("late JPS ContentRootEntity cleaned newly covered root", true, lateJpsLatch.await(5L, TimeUnit.SECONDS));
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					job.cancel(new CancellationException("end reactive integration test"));
				}
				return job.join(cont);
			}
		);
		A.assertEquals("total cleanup updates equals 3 without feedback loops", 3, updateCount.get());
		A.assertEquals("all nested ProjectRootEntity instances removed", Collections.emptyList(), sortedProjectRootUrls(snapRef.get(), adapter));
		A.assertEquals(
			"update description matches contract",
			NestedProjectRootCleaner.UPDATE_DESCRIPTION,
			updateDescriptions.get(0)
		);
	}
}
