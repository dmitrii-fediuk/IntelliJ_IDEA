# `Markdown Code Fence Background` Feature (`df.idea.feature.markdowncodefence`)

Suppresses the hardcoded full-width `MARKDOWN_CODE_FENCE_BACKGROUND` custom background renderer introduced in `IntelliJ IDEA 2026.3`, restoring plain fenced code blocks to the editor background while keeping caret-row highlighting and language-injected code fences (`INJECTED_LANGUAGE_FRAGMENT`) intact.  
Part of the unified [«Dmitrii Fediuk»](../.md) (`df.idea`) plugin.

## 1. Problem and Root Cause

In `IntelliJ IDEA 2026.3`, the bundled Markdown plugin registers `CodeFenceBackgroundHighlightingPassFactory` in `intellij.markdown.backend.xml` with no Registry switch:

1. For every `MarkdownCodeFence`, `CodeFenceBackgroundPass.doApplyInformationToEditor()` adds a `RangeHighlighter` on `editor.getMarkupModel()` keyed by `MarkdownHighlighterColors.CODE_FENCE_BACKGROUND` (`MARKDOWN_CODE_FENCE_BACKGROUND`) and calls `setCustomRenderer(new MarkdownCodeFenceBackgroundRenderer())`.  
1. `MarkdownCodeFenceBackgroundRenderer` (`CustomHighlighterOrder.BEFORE_BACKGROUND`) resolves its fill color via `scheme.getAttributes(CODE_FENCE)?.backgroundColor ?: scheme.getAttributes(EditorColors.createInjectedLanguageFragmentKey(MarkdownLanguage.INSTANCE))?.backgroundColor`.  
1. When a color scheme restores `MARKDOWN_CODE_FENCE` to its `2026.2.5` fallback (`DEFAULT_STRING`, no background color), the renderer falls back to `Markdown:INJECTED_LANGUAGE_FRAGMENT` → `INJECTED_LANGUAGE_FRAGMENT` (`#EDFCED` in `Default`/`IntelliJ Light`), painting a full-width pale-green rectangle over plain fenced code blocks without an injected language.  
1. Setting an explicit white `#FFFFFF` background on `MARKDOWN_CODE_FENCE` does not reproduce `2026.2.5` cleanly because `MarkdownHighlightingAnnotator` applies `MARKDOWN_CODE_FENCE` at `HighlightSeverity.INFORMATION` (`HighlighterLayer.ADDITIONAL_SYNTAX = 3000`, above `HighlighterLayer.CARET_ROW = 2000`), covering the active caret-row highlight under fence text.

## 2. Algorithm and Architecture

The feature is isolated in `df.idea.feature.markdowncodefence` using only public `IntelliJ Platform` APIs with zero compile-time dependencies on Markdown plugin internals or `@ApiStatus.Internal` types:

1. **Highlighter matching and renderer clearing**: [MarkdownCodeFenceBackgroundSuppressor.java](../../⛯/src/main/java/df/idea/feature/markdowncodefence/MarkdownCodeFenceBackgroundSuppressor.java) checks that a `RangeHighlighter` is valid (`isValid()`), has a non-null `CustomHighlighterRenderer`, and carries a `TextAttributesKey` whose external name equals `MARKDOWN_CODE_FENCE_BACKGROUND`.  
   Matching highlighters have their custom renderer cleared via `setCustomRenderer(null)`, leaving the highlighter valid and terminating any re-entrant `attributesChanged` callback immediately because `getCustomRenderer()` is then `null`.  
1. **Markup model observation**: `attach(MarkupModel)` clears any pre-existing matching highlighters on `MarkupModelEx` and registers a `MarkupModelListener` scoped to a dedicated `Disposable`.  
   Both `afterAdded` and `attributesChanged` (when `renderersChanged` is `true`, matching `RangeHighlighterImpl.setCustomRenderer` invoking `fireChanged(true, false, false)`) delegate to `suppressIfTarget`.  
1. **Editor lifecycle management**: [MarkdownCodeFenceBackgroundEditorListener.java](../../⛯/src/main/java/df/idea/feature/markdowncodefence/MarkdownCodeFenceBackgroundEditorListener.java) implements the public `com.intellij.editorFactoryListener` (`EditorFactoryListener`) extension point registered in [plugin.xml](../../⛯/src/main/resources/META-INF/plugin.xml).  
   Each created non-disposed editor receives an independent `Disposable` tracked in a synchronized `IdentityHashMap` and disposed on `editorReleased`, preventing listener leaks across tab opens and closes.

## 3. Effects and Invariants

1. Plain Markdown fenced code blocks (` ``` ` without a language) render with the normal editor background and active caret-row highlight, matching `IntelliJ IDEA 2026.2.5`.  
1. Language-injected code fences (such as ` ```python ` or ` ```json `) retain standard `InjectedGeneralHighlightingPass` highlighting (`INJECTED_LANGUAGE_FRAGMENT`), and non-Markdown editors and unrelated custom renderers (such as terminal block renderers) remain untouched.  
1. On `IntelliJ IDEA 2026.2.5` (where `MARKDOWN_CODE_FENCE_BACKGROUND` is never emitted) or when the Markdown plugin is disabled, the listener is a safe no-op with zero `LinkageError`.

## 4. Tests

1. [MarkdownCodeFenceBackgroundSuppressorTest.java](../../⛯/src/test/java/df/idea/feature/markdowncodefence/MarkdownCodeFenceBackgroundSuppressorTest.java) — unit assertions covering target key matching, invalid/null guards, idempotency, two-step `addRangeHighlighter` + `setCustomRenderer` interception, `renderersChanged=false` skipping, error resilience, non-`MarkupModelEx` degradation, duplicate `editorCreated` handling, and `editorReleased` disposal.  
1. [IdeMarkdownCodeFenceIntegrationTest.java](../../⛯/src/test/java/df/idea/feature/markdowncodefence/IdeMarkdownCodeFenceIntegrationTest.java) — end-to-end integration assertions against actual `DocumentImpl`, `MarkupModelImpl`, `RangeHighlighterImpl`, `HighlighterLayer`, and `Disposer` verifying synchronous interception of `MARKDOWN_CODE_FENCE_BACKGROUND`, preservation of `INJECTED_LANGUAGE_FRAGMENT`, `MARKDOWN_CODE_FENCE`, and keyless renderers, clean listener disposal, and zero Markdown plugin or `@ApiStatus.Internal` bytecode references in production classes.
