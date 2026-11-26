// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.markdown_code_fence_background;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import df.idea.support.Assertions;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Suppress Markdown code fence background renderer in IntelliJ IDEA 2026.3+":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/3
public final class MarkdownCodeFenceBackgroundSuppressorTest {
	private static final Assertions A = new Assertions();
	private static final TextAttributesKey TARGET_KEY = TextAttributesKey.createTextAttributesKey(
		MarkdownCodeFenceBackgroundSuppressor.TARGET_KEY_NAME
	);
	private static final TextAttributesKey CODE_FENCE_KEY = TextAttributesKey.createTextAttributesKey(
		"MARKDOWN_CODE_FENCE"
	);
	private static final TextAttributesKey INJECTED_KEY = TextAttributesKey.createTextAttributesKey(
		"INJECTED_LANGUAGE_FRAGMENT"
	);
	private static final CustomHighlighterRenderer DUMMY_RENDERER = (editor, highlighter, graphics) -> {};
	/**
	 * - @used-by build.ps1
	 */
	public static void main(String[] args) {
		testTargetMatching();
		testSuppressIfTargetAndIdempotency();
		testSuppressExistingAndErrorResilience();
		testAttachAndListenerEvents();
		testEditorListenerLifecycle();
		System.out.println(
			"MARKDOWN_CODE_FENCE_SUPPRESSOR_TESTS_PASSED=" + A.passedCount() + " assertions"
		);
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testEditorListenerLifecycle()}
	 */
	private static @NotNull Editor fakeEditor(@Nullable MarkupModel markupModel, boolean disposed) {
		return (Editor) Proxy.newProxyInstance(
			Editor.class.getClassLoader(),
			new Class<?>[]{Editor.class},
			(proxy, method, args) -> {
				Object r = null;
				String name = method.getName();
				if ("getMarkupModel".equals(name)) {
					r = markupModel;
				}
				else if ("isDisposed".equals(name)) {
					r = disposed;
				}
				else if ("equals".equals(name)) {
					r = proxy == args[0];
				}
				else if ("hashCode".equals(name)) {
					r = System.identityHashCode(proxy);
				}
				else if ("toString".equals(name)) {
					r = "FakeEditor";
				}
				return r;
			}
		);
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testEditorListenerLifecycle()}
	 */
	private static @NotNull EditorFactoryEvent fakeEditorEvent(@NotNull Editor editor) {
		return new EditorFactoryEvent(new StubEditorFactory(), editor);
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testAttachAndListenerEvents()}
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testEditorListenerLifecycle()}
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testSuppressExistingAndErrorResilience()}
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testSuppressIfTargetAndIdempotency()}
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testTargetMatching()}
	 */
	private static @NotNull RangeHighlighterEx fakeHighlighter(
		@Nullable TextAttributesKey key,
		@Nullable CustomHighlighterRenderer initialRenderer,
		boolean valid,
		@Nullable AtomicInteger setCalls,
		boolean throwOnSet,
		@Nullable Runnable onRendererChanged
	) {
		AtomicReference<CustomHighlighterRenderer> rendererRef = new AtomicReference<>(initialRenderer);
		return (RangeHighlighterEx) Proxy.newProxyInstance(
			RangeHighlighterEx.class.getClassLoader(),
			new Class<?>[]{RangeHighlighterEx.class},
			(proxy, method, args) -> {
				Object r = null;
				String name = method.getName();
				if ("getTextAttributesKey".equals(name)) {
					r = key;
				}
				else if ("getCustomRenderer".equals(name)) {
					r = rendererRef.get();
				}
				else if ("isValid".equals(name)) {
					r = valid;
				}
				else if ("setCustomRenderer".equals(name)) {
					if (null != setCalls) {
						setCalls.incrementAndGet();
					}
					if (throwOnSet) {
						throw new IllegalStateException("simulated setCustomRenderer failure");
					}
					CustomHighlighterRenderer next = (CustomHighlighterRenderer) args[0];
					CustomHighlighterRenderer prev = rendererRef.getAndSet(next);
					if (prev != next && null != onRendererChanged) {
						onRendererChanged.run();
					}
				}
				else if ("equals".equals(name)) {
					r = proxy == args[0];
				}
				else if ("hashCode".equals(name)) {
					r = System.identityHashCode(proxy);
				}
				return r;
			}
		);
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testAttachAndListenerEvents()}
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testEditorListenerLifecycle()}
	 */
	private static @NotNull MarkupModelEx fakeMarkupModelEx(
		@NotNull List<RangeHighlighter> highlighters,
		@NotNull List<MarkupModelListener> listeners,
		boolean throwOnAddListener
	) {
		return (MarkupModelEx) Proxy.newProxyInstance(
			MarkupModelEx.class.getClassLoader(),
			new Class<?>[]{MarkupModelEx.class},
			(proxy, method, args) -> {
				Object r = null;
				String name = method.getName();
				if ("getAllHighlighters".equals(name)) {
					r = highlighters.toArray(new RangeHighlighter[0]);
				}
				else if ("addMarkupModelListener".equals(name)) {
					if (throwOnAddListener) {
						throw new IllegalStateException("simulated addMarkupModelListener failure");
					}
					Disposable parent = (Disposable) args[0];
					MarkupModelListener listener = (MarkupModelListener) args[1];
					listeners.add(listener);
					Disposer.register(parent, () -> listeners.remove(listener));
				}
				return r;
			}
		);
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testAttachAndListenerEvents()}
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#testSuppressExistingAndErrorResilience()}
	 */
	private static @NotNull MarkupModel fakePlainMarkupModel(@NotNull List<RangeHighlighter> highlighters) {
		return (MarkupModel) Proxy.newProxyInstance(
			MarkupModel.class.getClassLoader(),
			new Class<?>[]{MarkupModel.class},
			(proxy, method, args) -> "getAllHighlighters".equals(method.getName())
				? highlighters.toArray(new RangeHighlighter[0])
				: null
		);
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#main(String[])}
	 */
	private static void testAttachAndListenerEvents() {
		List<RangeHighlighter> existing = new ArrayList<>();
		RangeHighlighterEx preExistingTarget = fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, null, false, null);
		RangeHighlighterEx preExistingInjected = fakeHighlighter(
			INJECTED_KEY,
			DUMMY_RENDERER,
			true,
			null,
			false,
			null
		);
		existing.add(preExistingTarget);
		existing.add(preExistingInjected);
		List<MarkupModelListener> listeners = new ArrayList<>();
		MarkupModelEx markupModel = fakeMarkupModelEx(existing, listeners, false);
		Disposable subscription = MarkdownCodeFenceBackgroundSuppressor.attach(markupModel);
		A.assertTrue("attach returns non-null subscription on MarkupModelEx", null != subscription);
		A.assertEquals("attach registers exactly 1 listener", 1, listeners.size());
		A.assertEquals("attach strips pre-existing target renderer", null, preExistingTarget.getCustomRenderer());
		A.assertEquals("attach preserves non-target renderer", DUMMY_RENDERER, preExistingInjected.getCustomRenderer());
		AtomicInteger twoStepCalls = new AtomicInteger();
		AtomicReference<RangeHighlighterEx> twoStepHolder = new AtomicReference<>();
		RangeHighlighterEx twoStep = fakeHighlighter(
			TARGET_KEY,
			null,
			true,
			twoStepCalls,
			false,
			() -> {
				for (MarkupModelListener l : new ArrayList<>(listeners)) {
					l.attributesChanged(twoStepHolder.get(), true, false);
				}
			}
		);
		twoStepHolder.set(twoStep);
		for (MarkupModelListener l : new ArrayList<>(listeners)) {
			l.afterAdded(twoStep);
		}
		A.assertEquals("afterAdded without renderer performs zero set calls", 0, twoStepCalls.get());
		twoStep.setCustomRenderer(DUMMY_RENDERER);
		A.assertEquals("two-step setCustomRenderer triggers exactly 2 calls (set + clear)", 2, twoStepCalls.get());
		A.assertEquals("two-step target renderer cleared without recursion", null, twoStep.getCustomRenderer());
		RangeHighlighterEx addedWithRenderer = fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, null, false, null);
		for (MarkupModelListener l : new ArrayList<>(listeners)) {
			l.afterAdded(addedWithRenderer);
		}
		A.assertEquals("afterAdded clears renderer when already present", null, addedWithRenderer.getCustomRenderer());
		AtomicInteger nonRendererAttrCalls = new AtomicInteger();
		RangeHighlighterEx ignoredFlag = fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, nonRendererAttrCalls, false, null);
		for (MarkupModelListener l : new ArrayList<>(listeners)) {
			l.attributesChanged(ignoredFlag, false, true);
		}
		A.assertEquals("attributesChanged with renderersChanged=false skips clearing", 0, nonRendererAttrCalls.get());
		Disposer.dispose(subscription);
		A.assertEquals("disposing subscription unregisters listener", 0, listeners.size());
		A.assertEquals(
			"attach returns null for non-MarkupModelEx",
			null,
			MarkdownCodeFenceBackgroundSuppressor.attach(fakePlainMarkupModel(existing))
		);
		A.assertEquals(
			"attach returns null when addMarkupModelListener throws",
			null,
			MarkdownCodeFenceBackgroundSuppressor.attach(fakeMarkupModelEx(existing, new ArrayList<>(), true))
		);
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#main(String[])}
	 */
	private static void testEditorListenerLifecycle() {
		MarkdownCodeFenceBackgroundEditorListener editorListener = new MarkdownCodeFenceBackgroundEditorListener();
		List<RangeHighlighter> highlighters = new ArrayList<>();
		RangeHighlighterEx target = fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, null, false, null);
		highlighters.add(target);
		List<MarkupModelListener> listeners = new ArrayList<>();
		MarkupModelEx markupModel = fakeMarkupModelEx(highlighters, listeners, false);
		Editor editor = fakeEditor(markupModel, false);
		Editor disposedEditor = fakeEditor(markupModel, true);
		editorListener.editorCreated(fakeEditorEvent(disposedEditor));
		A.assertEquals("disposed editor is ignored", 0, editorListener.activeSubscriptionCount());
		editorListener.editorCreated(fakeEditorEvent(editor));
		A.assertEquals("active editor registers 1 subscription", 1, editorListener.activeSubscriptionCount());
		A.assertEquals("active editor registers 1 markup listener", 1, listeners.size());
		A.assertEquals("editorCreated clears existing target renderer", null, target.getCustomRenderer());
		editorListener.editorCreated(fakeEditorEvent(editor));
		A.assertEquals("duplicate editorCreated keeps 1 subscription", 1, editorListener.activeSubscriptionCount());
		A.assertEquals("duplicate editorCreated disposes prior listener", 1, listeners.size());
		editorListener.editorReleased(fakeEditorEvent(editor));
		A.assertEquals("editorReleased removes subscription", 0, editorListener.activeSubscriptionCount());
		A.assertEquals("editorReleased disposes markup listener", 0, listeners.size());
		editorListener.detachEditor(null);
		editorListener.attachEditor(null);
		A.assertEquals("null editor calls keep subscription count 0", 0, editorListener.activeSubscriptionCount());
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#main(String[])}
	 */
	private static void testSuppressExistingAndErrorResilience() {
		List<RangeHighlighter> list = new ArrayList<>();
		RangeHighlighterEx t1 = fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, null, false, null);
		RangeHighlighterEx t2 = fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, null, true, null);
		RangeHighlighterEx other = fakeHighlighter(CODE_FENCE_KEY, DUMMY_RENDERER, true, null, false, null);
		list.add(t1);
		list.add(t2);
		list.add(other);
		int count = MarkdownCodeFenceBackgroundSuppressor.suppressExisting(fakePlainMarkupModel(list));
		A.assertEquals("suppressExisting counts only successfully cleared targets", 1, count);
		A.assertEquals("t1 renderer cleared", null, t1.getCustomRenderer());
		A.assertEquals("throwing target retains renderer without propagating exception", DUMMY_RENDERER, t2.getCustomRenderer());
		A.assertEquals("non-target CODE_FENCE keeps renderer", DUMMY_RENDERER, other.getCustomRenderer());
		A.assertEquals("suppressExisting on null returns 0", 0, MarkdownCodeFenceBackgroundSuppressor.suppressExisting(null));
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#main(String[])}
	 */
	private static void testSuppressIfTargetAndIdempotency() {
		AtomicInteger calls = new AtomicInteger();
		RangeHighlighterEx target = fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, calls, false, null);
		A.assertTrue("first suppressIfTarget clears renderer", MarkdownCodeFenceBackgroundSuppressor.suppressIfTarget(target));
		A.assertEquals("renderer is null after suppress", null, target.getCustomRenderer());
		A.assertEquals("setCustomRenderer invoked once", 1, calls.get());
		A.assertTrue("second suppressIfTarget is idempotent no-op", !MarkdownCodeFenceBackgroundSuppressor.suppressIfTarget(target));
		A.assertEquals("setCustomRenderer not invoked again", 1, calls.get());
		AtomicBoolean nonTargetTouched = new AtomicBoolean(false);
		RangeHighlighterEx unrelated = fakeHighlighter(
			INJECTED_KEY,
			DUMMY_RENDERER,
			true,
			null,
			false,
			() -> nonTargetTouched.set(true)
		);
		A.assertTrue("unrelated key is not suppressed", !MarkdownCodeFenceBackgroundSuppressor.suppressIfTarget(unrelated));
		A.assertTrue("unrelated renderer untouched", !nonTargetTouched.get());
	}
	/**
	 * - @used-by {@link MarkdownCodeFenceBackgroundSuppressorTest#main(String[])}
	 */
	private static void testTargetMatching() {
		A.assertTrue("null highlighter is not target", !MarkdownCodeFenceBackgroundSuppressor.isTargetHighlighter(null));
		A.assertTrue(
			"invalid target highlighter is skipped",
			!MarkdownCodeFenceBackgroundSuppressor.isTargetHighlighter(
				fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, false, null, false, null)
			)
		);
		A.assertTrue(
			"target highlighter with null renderer is skipped",
			!MarkdownCodeFenceBackgroundSuppressor.isTargetHighlighter(
				fakeHighlighter(TARGET_KEY, null, true, null, false, null)
			)
		);
		A.assertTrue(
			"highlighter with null key is skipped",
			!MarkdownCodeFenceBackgroundSuppressor.isTargetHighlighter(
				fakeHighlighter(null, DUMMY_RENDERER, true, null, false, null)
			)
		);
		A.assertTrue(
			"INJECTED_LANGUAGE_FRAGMENT is not target",
			!MarkdownCodeFenceBackgroundSuppressor.isTargetHighlighter(
				fakeHighlighter(INJECTED_KEY, DUMMY_RENDERER, true, null, false, null)
			)
		);
		A.assertTrue(
			"MARKDOWN_CODE_FENCE is not target",
			!MarkdownCodeFenceBackgroundSuppressor.isTargetHighlighter(
				fakeHighlighter(CODE_FENCE_KEY, DUMMY_RENDERER, true, null, false, null)
			)
		);
		A.assertTrue(
			"valid MARKDOWN_CODE_FENCE_BACKGROUND with renderer matches",
			MarkdownCodeFenceBackgroundSuppressor.isTargetHighlighter(
				fakeHighlighter(TARGET_KEY, DUMMY_RENDERER, true, null, false, null)
			)
		);
	}
	private static final class StubEditorFactory extends EditorFactory {
		/**
		 * - @used-by {@link EditorFactory#addEditorFactoryListener(EditorFactoryListener)}
		 * @see EditorFactory#addEditorFactoryListener(EditorFactoryListener)
		 */
		@SuppressWarnings("removal")
		@Override
		public void addEditorFactoryListener(@NotNull EditorFactoryListener listener) {}
		/**
		 * - @used-by {@link EditorFactory#addEditorFactoryListener(EditorFactoryListener, Disposable)}
		 * @see EditorFactory#addEditorFactoryListener(EditorFactoryListener, Disposable)
		 */
		@Override
		public void addEditorFactoryListener(@NotNull EditorFactoryListener listener, @NotNull Disposable parentDisposable) {}
		/**
		 * - @used-by {@link EditorFactory#createDocument(CharSequence)}
		 * @see EditorFactory#createDocument(CharSequence)
		 */
		@Override
		public @NotNull Document createDocument(@NotNull CharSequence text) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createDocument(char[])}
		 * @see EditorFactory#createDocument(char[])
		 */
		@Override
		public @NotNull Document createDocument(char[] text) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createDocument(boolean)}
		 * @see EditorFactory#createDocument(boolean)
		 */
		@Override
		public @NotNull Document createDocument(boolean allowUpdatesWithoutWriteAction) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createDocument(CharSequence, boolean, boolean)}
		 * @see EditorFactory#createDocument(CharSequence, boolean, boolean)
		 */
		@Override
		public @NotNull Document createDocument(@NotNull CharSequence text, boolean acceptsSlashR, boolean allowUpdatesWithoutWriteAction) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createEditor(Document)}
		 * @see EditorFactory#createEditor(Document)
		 */
		@Override
		public Editor createEditor(@NotNull Document document) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createEditor(Document, Project)}
		 * @see EditorFactory#createEditor(Document, Project)
		 */
		@Override
		public Editor createEditor(@NotNull Document document, @Nullable Project project) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createEditor(Document, Project, EditorKind)}
		 * @see EditorFactory#createEditor(Document, Project, EditorKind)
		 */
		@Override
		public Editor createEditor(@NotNull Document document, @Nullable Project project, @NotNull EditorKind kind) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createEditor(Document, Project, FileType, boolean)}
		 * @see EditorFactory#createEditor(Document, Project, FileType, boolean)
		 */
		@Override
		public Editor createEditor(@NotNull Document document, @Nullable Project project, @NotNull FileType fileType, boolean isViewer) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createEditor(Document, Project, VirtualFile, boolean)}
		 * @see EditorFactory#createEditor(Document, Project, VirtualFile, boolean)
		 */
		@Override
		public Editor createEditor(@NotNull Document document, @Nullable Project project, @NotNull VirtualFile file, boolean isViewer) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createEditor(Document, Project, VirtualFile, boolean, EditorKind)}
		 * @see EditorFactory#createEditor(Document, Project, VirtualFile, boolean, EditorKind)
		 */
		@Override
		public Editor createEditor(
			@NotNull Document document,
			@Nullable Project project,
			@NotNull VirtualFile file,
			boolean isViewer,
			@NotNull EditorKind kind
		) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createViewer(Document)}
		 * @see EditorFactory#createViewer(Document)
		 */
		@Override
		public Editor createViewer(@NotNull Document document) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createViewer(Document, Project)}
		 * @see EditorFactory#createViewer(Document, Project)
		 */
		@Override
		public Editor createViewer(@NotNull Document document, @Nullable Project project) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#createViewer(Document, Project, EditorKind)}
		 * @see EditorFactory#createViewer(Document, Project, EditorKind)
		 */
		@Override
		public Editor createViewer(@NotNull Document document, @Nullable Project project, @NotNull EditorKind kind) {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#editors(Document, Project)}
		 * @see EditorFactory#editors(Document, Project)
		 */
		@Override
		public @NotNull Stream<Editor> editors(@NotNull Document document, @Nullable Project project) {
			return Stream.empty();
		}
		/**
		 * - @used-by {@link EditorFactory#getAllEditors()}
		 * @see EditorFactory#getAllEditors()
		 */
		@Override
		public Editor @NotNull [] getAllEditors() {
			return new Editor[0];
		}
		/**
		 * - @used-by {@link EditorFactory#getEditorList()}
		 * @see EditorFactory#getEditorList()
		 */
		@Override
		public @NotNull List<Editor> getEditorList() {
			return List.of();
		}
		/**
		 * - @used-by {@link EditorFactory#getEventMulticaster()}
		 * @see EditorFactory#getEventMulticaster()
		 */
		@Override
		public @NotNull EditorEventMulticaster getEventMulticaster() {
			throw new UnsupportedOperationException();
		}
		/**
		 * - @used-by {@link EditorFactory#refreshAllEditors()}
		 * @see EditorFactory#refreshAllEditors()
		 */
		@Override
		public void refreshAllEditors() {}
		/**
		 * - @used-by {@link EditorFactory#releaseEditor(Editor)}
		 * @see EditorFactory#releaseEditor(Editor)
		 */
		@Override
		public void releaseEditor(@NotNull Editor editor) {}
		/**
		 * - @used-by {@link EditorFactory#removeEditorFactoryListener(EditorFactoryListener)}
		 * @see EditorFactory#removeEditorFactoryListener(EditorFactoryListener)
		 */
		@SuppressWarnings("removal")
		@Override
		public void removeEditorFactoryListener(@NotNull EditorFactoryListener listener) {}
	}
}
