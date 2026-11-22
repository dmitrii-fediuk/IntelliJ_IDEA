// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Suppress Markdown code fence background renderer in IntelliJ IDEA 2026.3+":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/3
package df.idea.feature.markdowncodefence;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
final class MarkdownCodeFenceBackgroundSuppressor {
	static final String TARGET_KEY_NAME = "MARKDOWN_CODE_FENCE_BACKGROUND";
	private static final Logger LOG = Logger.getInstance(MarkdownCodeFenceBackgroundSuppressor.class);
	private MarkdownCodeFenceBackgroundSuppressor() {}
	static @Nullable Disposable attach(@Nullable MarkupModel markupModel) {
		Disposable r = null;
		if (markupModel instanceof MarkupModelEx markupModelEx) {
			suppressExisting(markupModelEx);
			Disposable disposable = Disposer.newDisposable("df.idea.markdowncodefence");
			try {
				markupModelEx.addMarkupModelListener(disposable, createListener());
				r = disposable;
			}
			catch (LinkageError | RuntimeException e) {
				Disposer.dispose(disposable);
				LOG.warn("Failed to attach Markdown code fence background listener", e);
			}
		}
		return r;
	}
	static @NotNull MarkupModelListener createListener() {
		return new MarkupModelListener() {
			@Override
			public void afterAdded(@NotNull RangeHighlighterEx highlighter) {
				suppressIfTarget(highlighter);
			}
			@Override
			public void attributesChanged(
				@NotNull RangeHighlighterEx highlighter,
				boolean renderersChanged,
				boolean fontStyleOrColorChanged
			) {
				if (renderersChanged) {
					suppressIfTarget(highlighter);
				}
			}
		};
	}
	static boolean isTargetHighlighter(@Nullable RangeHighlighter highlighter) {
		boolean r = false;
		if (null != highlighter && highlighter.isValid() && null != highlighter.getCustomRenderer()) {
			TextAttributesKey key = highlighter.getTextAttributesKey();
			r = null != key && TARGET_KEY_NAME.equals(key.getExternalName());
		}
		return r;
	}
	static int suppressExisting(@Nullable MarkupModel markupModel) {
		int r = 0;
		if (null != markupModel) {
			try {
				for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
					if (suppressIfTarget(highlighter)) {
						r++;
					}
				}
			}
			catch (LinkageError | RuntimeException e) {
				LOG.warn("Failed to inspect existing editor highlighters for Markdown code fence background", e);
			}
		}
		return r;
	}
	static boolean suppressIfTarget(@Nullable RangeHighlighter highlighter) {
		boolean r = false;
		if (isTargetHighlighter(highlighter)) {
			try {
				highlighter.setCustomRenderer(null);
				r = true;
			}
			catch (LinkageError | RuntimeException e) {
				LOG.warn("Failed to clear Markdown code fence custom background renderer", e);
			}
		}
		return r;
	}
}
