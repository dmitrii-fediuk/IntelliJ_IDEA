// 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
// "Initialize standalone df.idea IntelliJ IDEA plugin repository":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
// "Standardize df.idea feature package names with snake_case word separators":
// https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/4
package df.idea.feature.plugin_update_source;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
interface PluginUpdateSourceClient {
	@Nullable String getCurrentSourceHost(@NotNull PluginId pluginId);
	boolean isSupported();
	boolean matchesCustomRepository(@NotNull PluginId pluginId, @NotNull String repositoryUrl);
	void setCustomRepositorySource(@NotNull PluginId pluginId, @NotNull String repositoryUrl);
	final class Reflective implements PluginUpdateSourceClient {
		static final String SERVICE_CLASS_NAME = "com.intellij.openapi.updateSettings.impl.PluginUpdateSourceService";
		private static final Logger LOG = Logger.getInstance(Reflective.class);
		private static final PluginUpdateSourceClient UNSUPPORTED_CLIENT = new PluginUpdateSourceClient() {
			@Override
			public @Nullable String getCurrentSourceHost(@NotNull PluginId pluginId) {
				return null;
			}
			@Override
			public boolean isSupported() {
				return false;
			}
			@Override
			public boolean matchesCustomRepository(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {
				return false;
			}
			@Override
			public void setCustomRepositorySource(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {}
		};
		private final Object service;
		private final Method getPluginUpdateSourceId;
		private final Method createCustomRepositorySource;
		private final Method setPluginUpdateSourceId;
		private final Method getPresentableName;
		private final Method canInstallUpdatesFrom;
		private Reflective(
			@NotNull Object service,
			@NotNull Method getPluginUpdateSourceId,
			@NotNull Method createCustomRepositorySource,
			@NotNull Method setPluginUpdateSourceId,
			@NotNull Method getPresentableName,
			@NotNull Method canInstallUpdatesFrom
		) {
			this.service = service;
			this.getPluginUpdateSourceId = getPluginUpdateSourceId;
			this.createCustomRepositorySource = createCustomRepositorySource;
			this.setPluginUpdateSourceId = setPluginUpdateSourceId;
			this.getPresentableName = getPresentableName;
			this.canInstallUpdatesFrom = canInstallUpdatesFrom;
		}
		@Override
		public @Nullable String getCurrentSourceHost(@NotNull PluginId pluginId) {
			String r = null;
			Object existing = invoke(getPluginUpdateSourceId, service, pluginId);
			if (null != existing) {
				Object name = invoke(getPresentableName, existing);
				r = null != name ? name.toString() : "";
			}
			return r;
		}
		@Override
		public boolean isSupported() {
			return true;
		}
		@Override
		public boolean matchesCustomRepository(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {
			boolean r = false;
			Object existing = invoke(getPluginUpdateSourceId, service, pluginId);
			if (null != existing) {
				Object candidate = invoke(createCustomRepositorySource, service, repositoryUrl);
				if (null != candidate) {
					r = Boolean.TRUE.equals(invoke(canInstallUpdatesFrom, existing, candidate));
				}
			}
			return r;
		}
		@Override
		public void setCustomRepositorySource(@NotNull PluginId pluginId, @NotNull String repositoryUrl) {
			Object customSource = invoke(createCustomRepositorySource, service, repositoryUrl);
			if (null != customSource) {
				invoke(setPluginUpdateSourceId, service, pluginId, customSource);
			}
		}
		static @Nullable PluginUpdateSourceClient tryBind(@NotNull Application application) {
			return tryBind(
				Reflective.class.getClassLoader(),
				SERVICE_CLASS_NAME,
				application::getService
			);
		}
		static @Nullable PluginUpdateSourceClient tryBind(
			@NotNull ClassLoader classLoader,
			@NotNull String serviceClassName,
			@NotNull Function<Class<?>, Object> serviceLookup
		) {
			PluginUpdateSourceClient r = null;
			try {
				Class<?> serviceClass = Class.forName(serviceClassName, false, classLoader);
				Method isSupportedMethod = serviceClass.getMethod("isFunctionalitySupported");
				Object supported = isSupportedMethod.invoke(null);
				if (!Boolean.TRUE.equals(supported)) {
					r = UNSUPPORTED_CLIENT;
				}
				else {
					Method getSource = serviceClass.getMethod("getPluginUpdateSourceId", PluginId.class);
					Method createCustom = serviceClass.getMethod(
						"createCustomRepositoryPluginUpdateSourceId",
						String.class
					);
					Class<?> sourceType = createCustom.getReturnType();
					Method setSource = serviceClass.getMethod(
						"setPluginUpdateSourceId",
						PluginId.class,
						sourceType
					);
					Method presentableName = sourceType.getMethod("getPresentableName");
					Method canInstallFrom = sourceType.getMethod("canInstallUpdatesFrom", sourceType);
					Object serviceInstance = serviceLookup.apply(serviceClass);
					if (null == serviceInstance) {
						LOG.debug("PluginUpdateSourceService is not registered in the current Application");
					}
					else {
						r = new Reflective(
							serviceInstance,
							getSource,
							createCustom,
							setSource,
							presentableName,
							canInstallFrom
						);
					}
				}
			}
			catch (InvocationTargetException e) {
				rethrowIfCancellation(e.getCause());
				LOG.debug("Failed to probe PluginUpdateSourceService availability", e.getCause());
			}
			catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | LinkageError e) {
				LOG.debug("PluginUpdateSourceService API is unavailable in this IDE build: " + e.getMessage());
			}
			return r;
		}
		private static @Nullable Object invoke(@NotNull Method method, @Nullable Object target, Object... args) {
			try {
				return method.invoke(target, args);
			}
			catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				rethrowIfCancellation(cause);
				if (cause instanceof Error error) {
					throw error;
				}
				if (cause instanceof RuntimeException runtimeException) {
					throw runtimeException;
				}
				throw new IllegalStateException("Unexpected checked exception invoking " + method.getName(), cause);
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot invoke " + method.getName(), e);
			}
		}
		private static void rethrowIfCancellation(@Nullable Throwable cause) {
			if (cause instanceof CancellationException cancellation) {
				throw cancellation;
			}
		}
	}
}
