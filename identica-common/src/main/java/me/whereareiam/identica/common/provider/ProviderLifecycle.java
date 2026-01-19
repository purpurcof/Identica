package me.whereareiam.identica.common.provider;

import com.google.inject.Singleton;
import me.whereareiam.identica.logging.Logger;

@Singleton
public class ProviderLifecycle {
	public void disable(InternalProvider internal) {
		if (internal.getProvider() == null) return;
		try {
			internal.getProvider().onDisable();
			internal.setState(ProviderState.DISABLED);
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to disable provider %s: %s", internal.getDescriptor().getId(), e.getMessage());
		}
	}

	public void unload(InternalProvider internal) {
		if (internal.getProvider() == null) return;
		try {
			internal.getProvider().onUnload();
			internal.setState(ProviderState.UNLOADED);
			if (internal.getClassLoader() instanceof AutoCloseable closeable) {
				closeable.close();
			}
		} catch (Exception e) {
			internal.setState(ProviderState.FAILED);
			Logger.warn("Failed to unload provider %s: %s", internal.getDescriptor().getId(), e.getMessage());
		}
	}
}
