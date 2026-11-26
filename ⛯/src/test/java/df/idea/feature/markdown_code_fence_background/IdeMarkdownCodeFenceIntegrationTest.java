// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.markdown_code_fence_background;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.MarkupModelImpl;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Disposer;
import df.idea.support.Assertions;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jetbrains.annotations.NotNull;
// 2026-11-22
// "Suppress Markdown code fence background renderer in IntelliJ IDEA 2026.3+":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/3
public final class IdeMarkdownCodeFenceIntegrationTest {
	private static final Assertions A = new Assertions();
	private static final TextAttributesKey CODE_FENCE_BG_KEY = TextAttributesKey.createTextAttributesKey(
		MarkdownCodeFenceBackgroundSuppressor.TARGET_KEY_NAME
	);
	private static final TextAttributesKey CODE_FENCE_KEY = TextAttributesKey.createTextAttributesKey(
		"MARKDOWN_CODE_FENCE"
	);
	private static final TextAttributesKey INJECTED_KEY = TextAttributesKey.createTextAttributesKey(
		"INJECTED_LANGUAGE_FRAGMENT"
	);
	private static final CustomHighlighterRenderer FENCE_RENDERER = (editor, highlighter, graphics) -> {};
	private static final CustomHighlighterRenderer OTHER_RENDERER = (editor, highlighter, graphics) -> {};
	public static void main(String[] args) throws Exception {
		verifyRealMarkupModelInterception();
		verifyNoMarkdownPluginCouplingInProductionBytecode();
		System.out.println(
			"IDE_MARKDOWN_CODE_FENCE_INTEGRATION_PASSED=" + A.passedCount() + " assertions"
		);
	}
	private static @NotNull Application fakeApplication() {
		EditorColorsScheme scheme = (EditorColorsScheme) Proxy.newProxyInstance(
			EditorColorsScheme.class.getClassLoader(),
			new Class<?>[]{EditorColorsScheme.class},
			(proxy, method, args) -> null
		);
		EditorColorsManager colorsManager = new StubEditorColorsManager(scheme);
		return (Application) Proxy.newProxyInstance(
			Application.class.getClassLoader(),
			new Class<?>[]{Application.class},
			(proxy, method, args) -> {
				Object r = null;
				String name = method.getName();
				if ("getService".equals(name) && null != args && 0 < args.length && EditorColorsManager.class.equals(args[0])) {
					r = colorsManager;
				}
				else if ("isUnitTestMode".equals(name) || "isReadAccessAllowed".equals(name) || "isWriteAccessAllowed".equals(name)) {
					r = true;
				}
				else if ("isDisposed".equals(name) || "isHeadlessEnvironment".equals(name)) {
					r = "isHeadlessEnvironment".equals(name);
				}
				else if (boolean.class.equals(method.getReturnType())) {
					r = false;
				}
				return r;
			}
		);
	}
	private static void verifyNoMarkdownPluginCouplingInProductionBytecode() throws Exception {
		// 2026-11-22
		// "Standardize df.idea feature package names with snake_case word separators":
		// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
		List<String> classResources = List.of(
			"df/idea/feature/markdown_code_fence_background/MarkdownCodeFenceBackgroundSuppressor.class",
			"df/idea/feature/markdown_code_fence_background/MarkdownCodeFenceBackgroundSuppressor$1.class",
			"df/idea/feature/markdown_code_fence_background/MarkdownCodeFenceBackgroundEditorListener.class"
		);
		ClassLoader loader = MarkdownCodeFenceBackgroundSuppressor.class.getClassLoader();
		for (String resource : classResources) {
			try (InputStream in = loader.getResourceAsStream(resource)) {
				A.assertTrue("bytecode resource exists: " + resource, null != in);
				String latin1 = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
				A.assertTrue(
					"no org/intellij/plugins/markdown dependency in " + resource,
					!latin1.contains("org/intellij/plugins/markdown/")
				);
				A.assertTrue(
					"no com/intellij/markdown dependency in " + resource,
					!latin1.contains("com/intellij/markdown/")
				);
				A.assertTrue(
					"no ApiStatus$Internal annotation reference in " + resource,
					!latin1.contains("ApiStatus$Internal")
				);
			}
		}
	}
	private static void verifyRealMarkupModelInterception() {
		Disposable appDisposable = Disposer.newDisposable("df.idea.test.app");
		ApplicationManager.setApplication(fakeApplication(), appDisposable);
		DocumentEx document = new DocumentImpl("```\nTest\nTest\nTest\n```");
		TestMarkupModel markupModel = new TestMarkupModel(document);
		try {
			RangeHighlighter preExisting = markupModel.addRangeHighlighter(
				CODE_FENCE_BG_KEY,
				4,
				18,
				HighlighterLayer.ADDITIONAL_SYNTAX,
				HighlighterTargetArea.EXACT_RANGE
			);
			preExisting.setCustomRenderer(FENCE_RENDERER);
			A.assertEquals(
				"pre-existing target highlighter has renderer before attach",
				FENCE_RENDERER,
				preExisting.getCustomRenderer()
			);
			Disposable subscription = MarkdownCodeFenceBackgroundSuppressor.attach(markupModel);
			A.assertTrue("attach succeeded on real MarkupModelImpl", null != subscription);
			A.assertEquals(
				"pre-existing target highlighter renderer cleared on attach",
				null,
				preExisting.getCustomRenderer()
			);
			A.assertTrue("pre-existing target highlighter stays valid", preExisting.isValid());
			RangeHighlighter liveFenceBg = markupModel.addRangeHighlighter(
				CODE_FENCE_BG_KEY,
				4,
				18,
				HighlighterLayer.ADDITIONAL_SYNTAX,
				HighlighterTargetArea.EXACT_RANGE
			);
			A.assertEquals(
				"newly added target highlighter starts with null renderer",
				null,
				liveFenceBg.getCustomRenderer()
			);
			liveFenceBg.setCustomRenderer(FENCE_RENDERER);
			A.assertEquals(
				"real RangeHighlighterImpl.setCustomRenderer cleared synchronously via MarkupModelImpl",
				null,
				liveFenceBg.getCustomRenderer()
			);
			A.assertTrue("intercepted live target highlighter remains valid", liveFenceBg.isValid());
			RangeHighlighter injected = markupModel.addRangeHighlighter(
				INJECTED_KEY,
				4,
				18,
				HighlighterLayer.CARET_ROW - 1,
				HighlighterTargetArea.EXACT_RANGE
			);
			injected.setCustomRenderer(OTHER_RENDERER);
			A.assertEquals(
				"INJECTED_LANGUAGE_FRAGMENT highlighter keeps its custom renderer",
				OTHER_RENDERER,
				injected.getCustomRenderer()
			);
			RangeHighlighter codeFenceText = markupModel.addRangeHighlighter(
				CODE_FENCE_KEY,
				4,
				18,
				HighlighterLayer.ADDITIONAL_SYNTAX,
				HighlighterTargetArea.EXACT_RANGE
			);
			codeFenceText.setCustomRenderer(OTHER_RENDERER);
			A.assertEquals(
				"MARKDOWN_CODE_FENCE highlighter keeps its custom renderer",
				OTHER_RENDERER,
				codeFenceText.getCustomRenderer()
			);
			RangeHighlighter keyless = markupModel.addRangeHighlighter(
				null,
				0,
				3,
				HighlighterLayer.ADDITIONAL_SYNTAX,
				HighlighterTargetArea.EXACT_RANGE
			);
			keyless.setCustomRenderer(OTHER_RENDERER);
			A.assertEquals(
				"keyless highlighter (e.g. terminal block renderer) keeps its custom renderer",
				OTHER_RENDERER,
				keyless.getCustomRenderer()
			);
			Disposer.dispose(subscription);
			RangeHighlighter afterDispose = markupModel.addRangeHighlighter(
				CODE_FENCE_BG_KEY,
				4,
				18,
				HighlighterLayer.ADDITIONAL_SYNTAX,
				HighlighterTargetArea.EXACT_RANGE
			);
			afterDispose.setCustomRenderer(FENCE_RENDERER);
			A.assertEquals(
				"after Disposer.dispose listener no longer intercepts highlighters",
				FENCE_RENDERER,
				afterDispose.getCustomRenderer()
			);
		}
		finally {
			markupModel.dispose();
			Disposer.dispose(appDisposable);
		}
	}
	private static final class StubEditorColorsManager extends EditorColorsManager {
		private final EditorColorsScheme scheme;
		StubEditorColorsManager(@NotNull EditorColorsScheme scheme) {
			this.scheme = scheme;
		}
		@Override
		public void addColorScheme(@NotNull EditorColorsScheme scheme) {}
		@Override
		public @NotNull EditorColorsScheme getActiveVisibleScheme() {
			return scheme;
		}
		@Override
		public EditorColorsScheme @NotNull [] getAllSchemes() {
			return new EditorColorsScheme[]{scheme};
		}
		@Override
		public @NotNull EditorColorsScheme getGlobalScheme() {
			return scheme;
		}
		@Override
		public EditorColorsScheme getScheme(@NotNull String schemeName) {
			return scheme;
		}
		@Override
		public long getSchemeModificationCounter() {
			return 0L;
		}
		@Override
		public boolean isDefaultScheme(EditorColorsScheme scheme) {
			return true;
		}
		@Override
		public boolean isUseOnlyMonospacedFonts() {
			return false;
		}
		@Override
		public void setCurrentSchemeOnLafChange(@NotNull EditorColorsScheme scheme) {}
		@Override
		public void setGlobalScheme(EditorColorsScheme scheme) {}
		@Override
		public void setUseOnlyMonospacedFonts(boolean b) {}
	}
	private static final class TestMarkupModel extends MarkupModelImpl {
		private static final long serialVersionUID = 1L;
		TestMarkupModel(@NotNull DocumentEx document) {
			super(document);
		}
	}
}
