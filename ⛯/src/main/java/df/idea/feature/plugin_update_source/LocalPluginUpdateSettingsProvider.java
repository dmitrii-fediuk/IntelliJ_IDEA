// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// 2026-11-22
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.plugin_update_source;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.updateSettings.impl.UpdateSettingsProvider;
import java.util.List;
import org.jetbrains.annotations.NotNull;
// 2026-11-22
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
public final class LocalPluginUpdateSettingsProvider implements UpdateSettingsProvider {
	private static final Logger LOG = Logger.getInstance(LocalPluginUpdateSettingsProvider.class);
	@Override
	public @NotNull List<String> getPluginRepositories() {
		List<String> r = List.of();
		try {
			String url = LocalPluginRepositoryLocator.discoverLocalRepositoryUrl();
			if (null != url) {
				r = List.of(url);
			}
		}
		catch (RuntimeException e) {
			LOG.warn("Failed to resolve local plugin update repository URL", e);
		}
		return r;
	}
}
