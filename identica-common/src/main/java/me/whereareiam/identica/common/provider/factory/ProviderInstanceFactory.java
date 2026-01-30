package me.whereareiam.identica.common.provider.factory;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.logging.Logger;

@Singleton
public class ProviderInstanceFactory {
	public IdenticaProvider instantiateProvider(Class<?> providerClass) {
		try {
			Object instance = providerClass.getDeclaredConstructor().newInstance();
			return (IdenticaProvider) instance;
		} catch (NoSuchMethodException e) {
			return null;
		} catch (Exception e) {
			Logger.warn("Failed to instantiate provider %s: %s", providerClass.getName(), e.getMessage());
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public IdenticaProvider createInjectedProvider(
			Injector providerInjector,
			Class<?> providerClass,
			IdenticaProvider fallback
	) {
		try {
			return providerInjector.getInstance((Class<? extends IdenticaProvider>) providerClass);
		} catch (Exception e) {
			Logger.warn("Failed to construct provider with injector %s: %s", providerClass.getName(), e.getMessage());
			if (fallback != null) {
				providerInjector.injectMembers(fallback);
				return fallback;
			}
			return null;
		}
	}
}
