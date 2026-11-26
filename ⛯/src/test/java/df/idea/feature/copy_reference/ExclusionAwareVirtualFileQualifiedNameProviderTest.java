// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.copy_reference;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileSystem;
import df.idea.support.Assertions;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class ExclusionAwareVirtualFileQualifiedNameProviderTest {
	private static final Assertions A = new Assertions();
	private static final VirtualFileSystem FAKE_FS = new VirtualFileSystem() {
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#addVirtualFileListener(VirtualFileListener)
		 */
		@Override
		public void addVirtualFileListener(@NotNull VirtualFileListener listener) {}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#findFileByPath(String)
		 */
		@Override
		public @Nullable VirtualFile findFileByPath(@NotNull String path) {
			return null;
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#getProtocol()
		 */
		@Override
		public @NotNull String getProtocol() {
			return "file";
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#isReadOnly()
		 */
		@Override
		public boolean isReadOnly() {
			return true;
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#refresh(boolean)
		 */
		@Override
		public void refresh(boolean asynchronous) {}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#refreshAndFindFileByPath(String)
		 */
		@Override
		public @Nullable VirtualFile refreshAndFindFileByPath(@NotNull String path) {
			return null;
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#removeVirtualFileListener(VirtualFileListener)
		 */
		@Override
		public void removeVirtualFileListener(@NotNull VirtualFileListener listener) {}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#copyFile(Object, VirtualFile, VirtualFile, String)
		 */
		@Override
		protected @NotNull VirtualFile copyFile(Object requestor, @NotNull VirtualFile virtualFile, @NotNull VirtualFile newParent, @NotNull String copyName) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#createChildDirectory(Object, VirtualFile, String)
		 */
		@Override
		protected @NotNull VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#createChildFile(Object, VirtualFile, String)
		 */
		@Override
		protected @NotNull VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#deleteFile(Object, VirtualFile)
		 */
		@Override
		protected void deleteFile(Object requestor, @NotNull VirtualFile vFile) {}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#moveFile(Object, VirtualFile, VirtualFile)
		 */
		@Override
		protected void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) {}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest}
		 * @see VirtualFileSystem#renameFile(Object, VirtualFile, String)
		 */
		@Override
		protected void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) {}
	};
	/**
	 * - @used-by build.ps1
	 */
	public static void main(String[] args) {
		testNestedMagentoFile();
		testNestedMagentoDirectory();
		testNestedMagentoExcludedSubfolders();
		testNestedOuterModuleFile();
		testNestedOuterExcludedNonMagentoFile();
		testDirectExcludedNestedModuleRoot();
		testUnexcludedMavenGradleNestedModule();
		testSourceRootInsideModule();
		testThreeLevelNestedModules();
		testContentRootItselfReturnsFullPath();
		testExternalFileOutsideModulesReturnsNull();
		testInjectedVirtualFileWindowUnwrapsDelegate();
		System.out.println("COPY_REFERENCE_UNIT_TESTS_PASSED=" + A.passedCount());
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testContentRootItselfReturnsFullPath()}
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testExternalFileOutsideModulesReturnsNull()}
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testInjectedVirtualFileWindowUnwrapsDelegate()}
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoDirectory()}
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoExcludedSubfolders()}
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoFile()}
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterExcludedNonMagentoFile()}
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterModuleFile()}
	 */
	private static Model nestedSiteModel() {
		Model r = new Model();
		r.addModule(
			"_",
			"C:/work/clients/example.com",
			List.of("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖"),
			List.of()
		);
		String magentoRoot = "C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯";
		r.addModule(
			"Magento",
			magentoRoot,
			List.of(
				magentoRoot + "/pub/media",
				magentoRoot + "/pub/opt",
				magentoRoot + "/pub/static",
				magentoRoot + "/var",
				magentoRoot + "/vendor/magento/magento2-base"
			),
			List.of(magentoRoot + "/var")
		);
		return r;
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testContentRootItselfReturnsFullPath() {
		Model m = nestedSiteModel();
		VirtualFile magentoRoot = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯", true);
		A.assertEquals(
			"content root itself returns full path",
			"C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯",
			m.resolve(magentoRoot)
		);
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testDirectExcludedNestedModuleRoot() {
		Model m = new Model();
		m.addModule("_", "C:/work/proj", List.of("C:/work/proj/Ⰿ/≻/♕/⛯"), List.of());
		m.addModule("Code", "C:/work/proj/Ⰿ/≻/♕/⛯", List.of(), List.of());
		VirtualFile py = m.file("C:/work/proj/Ⰿ/≻/♕/⛯/scripts/harness.py", false);
		A.assertEquals("direct-excluded nested module root", "scripts/harness.py", m.resolve(py));
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testExternalFileOutsideModulesReturnsNull() {
		Model m = nestedSiteModel();
		VirtualFile ext = m.file("C:/temp/scratch.txt", false);
		A.assertEquals("external file returns null", null, m.resolve(ext));
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testInjectedVirtualFileWindowUnwrapsDelegate() {
		Model m = nestedSiteModel();
		VirtualFile reg = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯/vendor/magento/module-inventory-swatches-frontend-ui/registration.php", false);
		VirtualFile injected = new FakeVirtualFileWindow(reg);
		A.assertEquals(
			"injected VirtualFileWindow unwraps delegate",
			"vendor/magento/module-inventory-swatches-frontend-ui/registration.php",
			m.resolve(injected)
		);
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testNestedMagentoDirectory() {
		Model m = nestedSiteModel();
		VirtualFile dir = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯/vendor/magento", true);
		A.assertEquals("nested Magento directory", "vendor/magento", m.resolve(dir));
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testNestedMagentoExcludedSubfolders() {
		Model m = nestedSiteModel();
		VirtualFile staticJs = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯/pub/static/frontend/app.js", false);
		VirtualFile varLog = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯/var/log/system.log", false);
		VirtualFile baseFile = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯/vendor/magento/magento2-base/composer.json", false);
		A.assertEquals("nested Magento pub/static excluded file", "pub/static/frontend/app.js", m.resolve(staticJs));
		A.assertEquals("nested Magento var excluded+source file", "var/log/system.log", m.resolve(varLog));
		A.assertEquals("nested Magento magento2-base excluded file", "vendor/magento/magento2-base/composer.json", m.resolve(baseFile));
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testNestedMagentoFile() {
		Model m = nestedSiteModel();
		VirtualFile reg = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/⛯/vendor/magento/module-inventory-swatches-frontend-ui/registration.php", false);
		A.assertEquals(
			"nested Magento registration.php",
			"vendor/magento/module-inventory-swatches-frontend-ui/registration.php",
			m.resolve(reg)
		);
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testNestedOuterExcludedNonMagentoFile() {
		Model m = nestedSiteModel();
		VirtualFile dump = m.file("C:/work/clients/example.com/Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/db/dump.sql", false);
		A.assertEquals(
			"nested outer excluded non-Magento file",
			"Ⰿ/≻/♕/⛯/𝐖/example.com/2026-10-26/db/dump.sql",
			m.resolve(dump)
		);
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testNestedOuterModuleFile() {
		Model m = nestedSiteModel();
		VirtualFile kb = m.file("C:/work/clients/example.com/ꘖ/KB/Overview.md", false);
		VirtualFile readme = m.file("C:/work/clients/example.com/readme.md", false);
		A.assertEquals("nested outer module KB file", "ꘖ/KB/Overview.md", m.resolve(kb));
		A.assertEquals("nested outer module readme", "readme.md", m.resolve(readme));
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testSourceRootInsideModule() {
		Model m = new Model();
		m.addModule("code", "C:/work/proj/code", List.of(), List.of("C:/work/proj/code/src"));
		VirtualFile javaFile = m.file("C:/work/proj/code/src/com/example/Main.java", false);
		A.assertEquals("sourceRoot inside module", "com/example/Main.java", m.resolve(javaFile));
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testThreeLevelNestedModules() {
		Model m = new Model();
		m.addModule("outer", "C:/work/proj", List.of("C:/work/proj/Ⰿ/≻/♕/⛯/𝐖"), List.of());
		m.addModule("middle", "C:/work/proj/Ⰿ/≻/♕/⛯/𝐖/site/2026-10-26/⛯", List.of(), List.of());
		m.addModule("inner", "C:/work/proj/Ⰿ/≻/♕/⛯/𝐖/site/2026-10-26/⛯/vendor/custom/pkg", List.of(), List.of());
		VirtualFile php = m.file("C:/work/proj/Ⰿ/≻/♕/⛯/𝐖/site/2026-10-26/⛯/vendor/custom/pkg/Block.php", false);
		A.assertEquals("three-level nested modules stop at excluded boundary", "vendor/custom/pkg/Block.php", m.resolve(php));
	}
	/**
	 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 */
	private static void testUnexcludedMavenGradleNestedModule() {
		Model m = new Model();
		m.addModule("root", "C:/work/app", List.of(), List.of());
		m.addModule("sub", "C:/work/app/sub", List.of(), List.of());
		VirtualFile readme = m.file("C:/work/app/sub/README.md", false);
		A.assertEquals("unexcluded Maven/Gradle nested module", "sub/README.md", m.resolve(readme));
	}
	private record ModuleSpec(
		String name,
		Module module,
		VirtualFile contentRoot,
		List<VirtualFile> excludeRoots,
		List<VirtualFile> sourceRoots
	) {}
	private static final class FakeVirtualFile extends VirtualFile {
		private static final long serialVersionUID = 1L;
		private final String path;
		private final @Nullable VirtualFile parent;
		private final boolean directory;
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest.Model#file(String, boolean)}
		 */
		FakeVirtualFile(String path, @Nullable VirtualFile parent, boolean directory) {
			this.path = path;
			this.parent = parent;
			this.directory = directory;
		}
		/**
		 * - @used-by {@link VirtualFile#contentsToByteArray()}
		 * @see VirtualFile#contentsToByteArray()
		 */
		@Override
		public byte @NotNull [] contentsToByteArray() {
			return new byte[0];
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProvider#resolveQualifiedName(ProjectFileIndex, VirtualFile, ExcludeRootsResolver)}
		 * - @used-by {@link Object#equals(Object)}
		 * - @used-by {@link df.idea.feature.markdown_code_fence_background.IdeMarkdownCodeFenceIntegrationTest#fakeApplication()}
		 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressor#isTargetHighlighter(RangeHighlighter)}
		 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#fakeEditor(MarkupModel, boolean)}
		 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#fakeHighlighter(TextAttributesKey, CustomHighlighterRenderer, boolean, AtomicInteger, boolean, Runnable)}
		 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#fakeMarkupModelEx(List, List, boolean)}
		 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#fakePlainMarkupModel(List)}
		 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#fakeProject()}
		 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#fakeStorageChange(ImmutableEntityStorage, ImmutableEntityStorage, Map)}
		 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyReactiveEventLogCycle(ProjectRootEntityAdapter)}
		 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#fakeProject(AtomicBoolean)}
		 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#fakeStorageChange(ImmutableEntityStorage, ImmutableEntityStorage, Map)}
		 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#fakeWorkspaceModel(AtomicReference, Flow, AtomicInteger, Runnable)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrar#isStaleLocalRepositoryUrl(String)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest.FakeClient#matchesCustomRepository(PluginId, String)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest.Simulated2026_3SourceImpl#canInstallUpdatesFrom(Simulated2026_3Source)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.PluginUpdateSourceClient.Reflective#matchesCustomRepository(PluginId, String)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.PluginUpdateSourceClient.Reflective#tryBind(ClassLoader, String, Function)}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolver#resolveForProject(String)}
		 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#main(String[])}
		 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyDescriptor(PluginMainDescriptor, Path)}
		 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyViaIdeaProperties()}
		 * @see Object#equals(Object)
		 */
		@Override
		public boolean equals(Object o) {
			return (o instanceof FakeVirtualFile other) && path.equals(other.path);
		}
		/**
		 * - @used-by {@link VirtualFile#getChildren()}
		 * @see VirtualFile#getChildren()
		 */
		@Override
		public VirtualFile[] getChildren() {
			return VirtualFile.EMPTY_ARRAY;
		}
		/**
		 * - @used-by {@link VirtualFile#getFileSystem()}
		 * @see VirtualFile#getFileSystem()
		 */
		@Override
		public @NotNull VirtualFileSystem getFileSystem() {
			return FAKE_FS;
		}
		/**
		 * - @used-by {@link VirtualFile#getInputStream()}
		 * @see VirtualFile#getInputStream()
		 */
		@Override
		public @NotNull InputStream getInputStream() {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link VirtualFile#getLength()}
		 * @see VirtualFile#getLength()
		 */
		@Override
		public long getLength() {
			return 0;
		}
		/**
		 * - @used-by {@link VirtualFile#getName()}
		 * @see VirtualFile#getName()
		 */
		@Override
		public @NotNull String getName() {
			int idx = path.lastIndexOf('/');
			return 0 <= idx ? path.substring(idx + 1) : path;
		}
		/**
		 * - @used-by {@link VirtualFile#getOutputStream(Object, long, long)}
		 * @see VirtualFile#getOutputStream(Object, long, long)
		 */
		@Override
		public @NotNull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link VirtualFile#getParent()}
		 * @see VirtualFile#getParent()
		 */
		@Override
		public @Nullable VirtualFile getParent() {
			return parent;
		}
		/**
		 * - @used-by {@link VirtualFile#getPath()}
		 * @see VirtualFile#getPath()
		 */
		@Override
		public @NotNull String getPath() {
			return path;
		}
		/**
		 * - @used-by {@link VirtualFile#getTimeStamp()}
		 * @see VirtualFile#getTimeStamp()
		 */
		@Override
		public long getTimeStamp() {
			return 0;
		}
		/**
		 * - @used-by {@link Object#hashCode()}
		 * @see Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return path.hashCode();
		}
		/**
		 * - @used-by {@link VirtualFile#isDirectory()}
		 * @see VirtualFile#isDirectory()
		 */
		@Override
		public boolean isDirectory() {
			return directory;
		}
		/**
		 * - @used-by {@link VirtualFile#isValid()}
		 * @see VirtualFile#isValid()
		 */
		@Override
		public boolean isValid() {
			return true;
		}
		/**
		 * - @used-by {@link VirtualFile#isWritable()}
		 * @see VirtualFile#isWritable()
		 */
		@Override
		public boolean isWritable() {
			return true;
		}
		/**
		 * - @used-by {@link VirtualFile#refresh(boolean, boolean, Runnable)}
		 * @see VirtualFile#refresh(boolean, boolean, Runnable)
		 */
		@Override
		public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {}
	}
	private static final class FakeVirtualFileWindow extends VirtualFile implements VirtualFileWindow {
		private static final long serialVersionUID = 1L;
		private final VirtualFile delegate;
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testInjectedVirtualFileWindowUnwrapsDelegate()}
		 */
		FakeVirtualFileWindow(VirtualFile delegate) {
			this.delegate = delegate;
		}
		/**
		 * - @used-by {@link VirtualFile#contentsToByteArray()}
		 * @see VirtualFile#contentsToByteArray()
		 */
		@Override
		public byte @NotNull [] contentsToByteArray() {
			return new byte[0];
		}
		/**
		 * - @used-by {@link VirtualFile#getChildren()}
		 * @see VirtualFile#getChildren()
		 */
		@Override
		public VirtualFile[] getChildren() {
			return VirtualFile.EMPTY_ARRAY;
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProvider#resolveQualifiedName(ProjectFileIndex, VirtualFile, ExcludeRootsResolver)}
		 * - @used-by {@link VirtualFileWindow#getDelegate()}
		 * @see VirtualFileWindow#getDelegate()
		 */
		@Override
		public @NotNull VirtualFile getDelegate() {
			return delegate;
		}
		/**
		 * - @used-by {@link VirtualFileWindow#getDocumentWindow()}
		 * @see VirtualFileWindow#getDocumentWindow()
		 */
		@Override
		public @NotNull DocumentWindow getDocumentWindow() {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link VirtualFile#getFileSystem()}
		 * @see VirtualFile#getFileSystem()
		 */
		@Override
		public @NotNull VirtualFileSystem getFileSystem() {
			return FAKE_FS;
		}
		/**
		 * - @used-by {@link VirtualFile#getInputStream()}
		 * @see VirtualFile#getInputStream()
		 */
		@Override
		public @NotNull InputStream getInputStream() {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link VirtualFile#getLength()}
		 * @see VirtualFile#getLength()
		 */
		@Override
		public long getLength() {
			return 0;
		}
		/**
		 * - @used-by {@link VirtualFile#getName()}
		 * @see VirtualFile#getName()
		 */
		@Override
		public @NotNull String getName() {
			return "injected.sql";
		}
		/**
		 * - @used-by {@link VirtualFile#getOutputStream(Object, long, long)}
		 * @see VirtualFile#getOutputStream(Object, long, long)
		 */
		@Override
		public @NotNull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link VirtualFile#getParent()}
		 * @see VirtualFile#getParent()
		 */
		@Override
		public @Nullable VirtualFile getParent() {
			return null;
		}
		/**
		 * - @used-by {@link VirtualFile#getPath()}
		 * @see VirtualFile#getPath()
		 */
		@Override
		public @NotNull String getPath() {
			return "/injected.sql";
		}
		/**
		 * - @used-by {@link VirtualFile#getTimeStamp()}
		 * @see VirtualFile#getTimeStamp()
		 */
		@Override
		public long getTimeStamp() {
			return 0;
		}
		/**
		 * - @used-by {@link VirtualFile#isDirectory()}
		 * @see VirtualFile#isDirectory()
		 */
		@Override
		public boolean isDirectory() {
			return false;
		}
		/**
		 * - @used-by {@link VirtualFile#isValid()}
		 * @see VirtualFile#isValid()
		 */
		@Override
		public boolean isValid() {
			return true;
		}
		/**
		 * - @used-by {@link VirtualFile#isWritable()}
		 * @see VirtualFile#isWritable()
		 */
		@Override
		public boolean isWritable() {
			return true;
		}
		/**
		 * - @used-by {@link VirtualFile#refresh(boolean, boolean, Runnable)}
		 * @see VirtualFile#refresh(boolean, boolean, Runnable)
		 */
		@Override
		public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {}
	}
	static final class Model {
		private final Map<String, FakeVirtualFile> files = new HashMap<>();
		private final List<ModuleSpec> modules = new ArrayList<>();
		private final Map<Module, VirtualFile[]> excludesByModule = new HashMap<>();
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#nestedSiteModel()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testDirectExcludedNestedModuleRoot()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testSourceRootInsideModule()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testThreeLevelNestedModules()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testUnexcludedMavenGradleNestedModule()}
		 */
		void addModule(String name, String contentRootPath, List<String> excludes, List<String> sources) {
			VirtualFile contentRoot = file(contentRootPath, true);
			List<VirtualFile> excludeFiles = excludes.stream().map(p -> file(p, true)).toList();
			List<VirtualFile> sourceFiles = sources.stream().map(p -> file(p, true)).toList();
			Module mod = (Module) Proxy.newProxyInstance(
				Module.class.getClassLoader(),
				new Class<?>[]{Module.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getName" -> name;
					case "toString" -> "Module(" + name + ")";
					case "equals" -> proxy == args[0];
					case "hashCode" -> System.identityHashCode(proxy);
					default -> throw new UnsupportedOperationException("Unexpected Module method: " + method.getName());
				}
			);
			ModuleSpec spec = new ModuleSpec(name, mod, contentRoot, excludeFiles, sourceFiles);
			modules.add(spec);
			excludesByModule.put(mod, excludeFiles.toArray(VirtualFile[]::new));
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testContentRootItselfReturnsFullPath()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testDirectExcludedNestedModuleRoot()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testExternalFileOutsideModulesReturnsNull()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testInjectedVirtualFileWindowUnwrapsDelegate()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoDirectory()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoExcludedSubfolders()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoFile()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterExcludedNonMagentoFile()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterModuleFile()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testSourceRootInsideModule()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testThreeLevelNestedModules()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testUnexcludedMavenGradleNestedModule()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest.Model#addModule(String, String, List, List)}
		 */
		VirtualFile file(String rawPath, boolean directory) {
			String normalized = normalize(rawPath);
			FakeVirtualFile r = files.get(normalized);
			if (null == r) {
				int slash = normalized.lastIndexOf('/');
				VirtualFile parent = 0 < slash ? file(normalized.substring(0, slash), true) : null;
				r = new FakeVirtualFile(normalized, parent, directory);
				files.put(normalized, r);
			}
			return r;
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testContentRootItselfReturnsFullPath()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testDirectExcludedNestedModuleRoot()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testExternalFileOutsideModulesReturnsNull()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testInjectedVirtualFileWindowUnwrapsDelegate()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoDirectory()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoExcludedSubfolders()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoFile()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterExcludedNonMagentoFile()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterModuleFile()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testSourceRootInsideModule()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testThreeLevelNestedModules()}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest#testUnexcludedMavenGradleNestedModule()}
		 * - @used-by {@link df.idea.feature.plugin_update_source.IdeUpdateSourceIntegrationTest#main(String[])}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocator#resolveRepositoryFile(Path)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testDirectoryLayout(Path)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testLocatePluginArtifactFallback(Path)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testMissingUpdateXmlReturnsNull(Path)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testSiblingFileInNonBmpPath(Path)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#testMultiInstanceIsolation(Path)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#testReconcileGuardsAndLifecycle(Path)}
		 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#testReflectiveBridgeAcross2026_3And2026_2_5(Path)}
		 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#testMultipleNestedVariablesChooseLongestMatchOnSave()}
		 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#testNestedImlAndProjectXmlRoundTrip()}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolver#normalizeMacroPath(String, Path, Path, String)}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCacheHitInvalidationAndEviction()}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCrossVariableCompositionAndForwardRefs()}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCycleAndUndefinedReferenceHandling()}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testDirectoryNamedPropertiesIgnoredSafely()}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testLayeredOuterAndInnerInheritanceOverrideAndUnset()}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testRelativeAndAbsoluteResolutionWithUnicode()}
		 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesLocator#findCandidateFiles(String)}
		 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyViaIdeaProperties()}
		 */
		@Nullable String resolve(VirtualFile vf) {
			ProjectFileIndex index = (ProjectFileIndex) Proxy.newProxyInstance(
				ProjectFileIndex.class.getClassLoader(),
				new Class<?>[]{ProjectFileIndex.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getModuleForFile" -> {
						VirtualFile f = (VirtualFile) args[0];
						boolean honorExclusion = 1 == args.length || (Boolean) args[1];
						ModuleSpec spec = findNearestModule(f, honorExclusion);
						yield null == spec ? null : spec.module();
					}
					case "getContentRootForFile" -> {
						VirtualFile f = (VirtualFile) args[0];
						boolean honorExclusion = 1 == args.length || (Boolean) args[1];
						ModuleSpec spec = findNearestModule(f, honorExclusion);
						yield null == spec ? null : spec.contentRoot();
					}
					case "getSourceRootForFile" -> findSourceRoot((VirtualFile) args[0]);
					default -> throw new UnsupportedOperationException("Unexpected ProjectFileIndex method: " + method.getName());
				}
			);
			return ExclusionAwareVirtualFileQualifiedNameProvider.resolveQualifiedName(
				index,
				vf,
				module -> excludesByModule.getOrDefault(module, VirtualFile.EMPTY_ARRAY)
			);
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest.Model#findSourceRoot(VirtualFile)}
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest.Model#resolve(VirtualFile)}
		 */
		private @Nullable ModuleSpec findNearestModule(VirtualFile file, boolean honorExclusion) {
			return modules.stream()
				.filter(s -> VfsUtilCore.isAncestor(s.contentRoot(), file, false))
				.max(Comparator.comparingInt(s -> s.contentRoot().getPath().length()))
				.filter(s -> !honorExclusion || !isExcludedInSpec(s, file))
				.orElse(null);
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest.Model#resolve(VirtualFile)}
		 */
		private @Nullable VirtualFile findSourceRoot(VirtualFile file) {
			ModuleSpec spec = findNearestModule(file, true);
			return null == spec ? null : spec.sourceRoots().stream()
				.filter(sr -> VfsUtilCore.isAncestor(sr, file, false))
				.max(Comparator.comparingInt(sr -> sr.getPath().length()))
				.orElse(null);
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest.Model#findNearestModule(VirtualFile, boolean)}
		 */
		private static boolean isExcludedInSpec(ModuleSpec spec, VirtualFile file) {
			boolean r = false;
			for (VirtualFile excl : spec.excludeRoots()) {
				if (VfsUtilCore.isAncestor(excl, file, false)) {
					r = true;
					break;
				}
			}
			return r;
		}
		/**
		 * - @used-by {@link ExclusionAwareVirtualFileQualifiedNameProviderTest.Model#file(String, boolean)}
		 */
		private static String normalize(String path) {
			String r = path.replace('\\', '/');
			while (1 < r.length() && r.endsWith("/")) {
				r = r.substring(0, r.length() - 1);
			}
			return r;
		}
	}
}
