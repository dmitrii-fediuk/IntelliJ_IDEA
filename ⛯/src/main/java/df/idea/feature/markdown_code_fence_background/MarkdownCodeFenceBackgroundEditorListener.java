// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.markdown_code_fence_background;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.util.Disposer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Suppress Markdown code fence background renderer in IntelliJ IDEA 2026.3+":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/3
public final class MarkdownCodeFenceBackgroundEditorListener implements EditorFactoryListener {
	private static final Logger LOG = Logger.getInstance(MarkdownCodeFenceBackgroundEditorListener.class);
	private final Map<Editor, Disposable> subscriptions = Collections.synchronizedMap(new IdentityHashMap<>());
	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		attachEditor(event.getEditor());
	}
	@Override
	public void editorReleased(@NotNull EditorFactoryEvent event) {
		detachEditor(event.getEditor());
	}
	int activeSubscriptionCount() {
		return subscriptions.size();
	}
	void attachEditor(@Nullable Editor editor) {
		if (null != editor && !editor.isDisposed()) {
			try {
				Disposable subscription = MarkdownCodeFenceBackgroundSuppressor.attach(editor.getMarkupModel());
				if (null != subscription) {
					Disposable previous = subscriptions.put(editor, subscription);
					if (null != previous) {
						Disposer.dispose(previous);
					}
				}
			}
			catch (LinkageError | RuntimeException e) {
				LOG.warn("Failed to attach Markdown code fence background suppressor to editor", e);
			}
		}
	}
	void detachEditor(@Nullable Editor editor) {
		if (null != editor) {
			Disposable subscription = subscriptions.remove(editor);
			if (null != subscription) {
				Disposer.dispose(subscription);
			}
		}
	}
}
