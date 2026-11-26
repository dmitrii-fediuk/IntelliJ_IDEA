// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
package df.idea.support;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class Assertions {
	private int passed = 0;
	/**
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testContentRootItselfReturnsFullPath()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testDirectExcludedNestedModuleRoot()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testExternalFileOutsideModulesReturnsNull()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testInjectedVirtualFileWindowUnwrapsDelegate()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoDirectory()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoExcludedSubfolders()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedMagentoFile()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterExcludedNonMagentoFile()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testNestedOuterModuleFile()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testSourceRootInsideModule()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testThreeLevelNestedModules()}
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#testUnexcludedMavenGradleNestedModule()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.IdeMarkdownCodeFenceIntegrationTest#verifyRealMarkupModelInterception()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#testAttachAndListenerEvents()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#testEditorListenerLifecycle()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#testSuppressExistingAndErrorResilience()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#testSuppressIfTargetAndIdempotency()}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyModulelessProjectUntouched(ProjectRootEntityAdapter)}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyMultiProjectStorageCleanup(ProjectRootEntityAdapter)}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyNoHardInternalReferenceInProductionBytecode()}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyPlatformContributorContract()}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyReactiveEventLogCycle(ProjectRootEntityAdapter)}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testDisposedAndNullGuards()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testEventRelevance()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testPlainFlowFallback()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testReflectiveAdapterDegradation()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testResilienceAndCancellation()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootMatcherTest#testCaseSensitivity()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootMatcherTest#testCoveredSet()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootMatcherTest#testNormalization()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootMatcherTest#testStrictDescendant()}
	 * - @used-by {@link df.idea.feature.plugin_update_source.IdeUpdateSourceIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.plugin_update_source.IdeUpdateSourceIntegrationTest#verifyPlatformUpdateSourceContract(String)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testLocatePluginArtifactFallback(Path)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testMissingUpdateXmlReturnsNull(Path)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#testMultiInstanceIsolation(Path)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#testReconcileGuardsAndLifecycle(Path)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#testReflectiveBridgeAcross2026_3And2026_2_5(Path)}
	 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#testDotMacroMatchesCollectorPattern()}
	 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#testMultipleNestedVariablesChooseLongestMatchOnSave()}
	 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#testNestedImlAndProjectXmlRoundTrip()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCacheHitInvalidationAndEviction()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCrossVariableCompositionAndForwardRefs()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCycleAndUndefinedReferenceHandling()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testDirectoryNamedPropertiesIgnoredSafely()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testLayeredOuterAndInnerInheritanceOverrideAndUnset()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testRelativeAndAbsoluteResolutionWithUnicode()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesLocatorTest#testBlankAndInvalidPathsReturnEmpty()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesLocatorTest#testFileBasedIprProject()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesLocatorTest#testMultiProjectMiscXml()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesLocatorTest#testSingleProjectMiscXml()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesLocatorTest#testTwoLevelNestedMultiProjectMiscXml()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesParserTest#testEmptyValuePreservedForUnsetAndDuplicateOverride()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesParserTest#testKeyValidationAndReservedNames()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesParserTest#testUtf8AndNonBmpParsing()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesParserTest#testUtf8BomAndCommentsAndWhitespace()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesParserTest#testWindowsBackslashesNotCorrupted()}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#main(String[])}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyDescriptor(PluginMainDescriptor, Path)}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyViaIdeaProperties()}
	 */
	public void assertEquals(String label, @Nullable Object expected, @Nullable Object actual) {
		if (!Objects.equals(expected, actual)) {
			throw new AssertionError("FAIL [" + label + "]: expected=<" + expected + "> actual=<" + actual + ">");
		}
		passed++;
		System.out.println("PASS: " + label + " -> " + actual);
	}
	/**
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.IdeMarkdownCodeFenceIntegrationTest#verifyNoMarkdownPluginCouplingInProductionBytecode()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.IdeMarkdownCodeFenceIntegrationTest#verifyRealMarkupModelInterception()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#testAttachAndListenerEvents()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#testSuppressIfTargetAndIdempotency()}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#testTargetMatching()}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyNoHardInternalReferenceInProductionBytecode()}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#verifyPlatformContributorContract()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testDisposedAndNullGuards()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testEventRelevance()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testPlainFlowFallback()}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#testResilienceAndCancellation()}
	 * - @used-by {@link df.idea.feature.plugin_update_source.IdeUpdateSourceIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.plugin_update_source.IdeUpdateSourceIntegrationTest#verifyPlatformUpdateSourceContract(String)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testDirectoryLayout(Path)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testLocatePluginArtifactFallback(Path)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#testSiblingFileInNonBmpPath(Path)}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#testReflectiveBridgeAcross2026_3And2026_2_5(Path)}
	 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#testDotMacroMatchesCollectorPattern()}
	 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#testNestedImlAndProjectXmlRoundTrip()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCacheHitInvalidationAndEviction()}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#testCycleAndUndefinedReferenceHandling()}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#main(String[])}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyDescriptor(PluginMainDescriptor, Path)}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyViaIdeaProperties()}
	 */
	public void assertTrue(String label, boolean condition) {
		if (!condition) {
			throw new AssertionError("FAIL [" + label + "]");
		}
		passed++;
		System.out.println("PASS: " + label);
	}
	/**
	 * - @used-by {@link df.idea.feature.copy_reference.ExclusionAwareVirtualFileQualifiedNameProviderTest#main(String[])}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.IdeMarkdownCodeFenceIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.markdown_code_fence_background.MarkdownCodeFenceBackgroundSuppressorTest#main(String[])}
	 * - @used-by {@link df.idea.feature.nested_project_root.IdeProjectRootIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootCleanerTest#main(String[])}
	 * - @used-by {@link df.idea.feature.nested_project_root.NestedProjectRootMatcherTest#main(String[])}
	 * - @used-by {@link df.idea.feature.plugin_update_source.IdeUpdateSourceIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalPluginRepositoryLocatorTest#main(String[])}
	 * - @used-by {@link df.idea.feature.plugin_update_source.LocalUpdateSourceRegistrarTest#main(String[])}
	 * - @used-by {@link df.idea.feature.project_path_macro.IdePathMacroIntegrationTest#main(String[])}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPathResolverTest#main(String[])}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesLocatorTest#main(String[])}
	 * - @used-by {@link df.idea.feature.project_path_macro.MacroPropertiesParserTest#main(String[])}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#main(String[])}
	 * - @used-by {@link df.idea.plugin.PluginDescriptorVerificationTest#verifyViaIdeaProperties()}
	 */
	public int passedCount() {
		return passed;
	}
}
