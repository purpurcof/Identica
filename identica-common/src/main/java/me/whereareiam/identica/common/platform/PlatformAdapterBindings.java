package me.whereareiam.identica.common.platform;

import com.google.inject.Binder;
import com.google.inject.multibindings.MapBinder;
import me.whereareiam.identica.type.platform.PlatformAdapterRole;
import org.jetbrains.annotations.NotNull;

/**
 * Shared helper for declaring and validating required platform adapter role
 * bindings.
 */
public final class PlatformAdapterBindings {
	/**
	 * Creates the adapter role map binder for a platform and installs the shared
	 * completeness validator.
	 *
	 * @param binder Guice binder
	 * @param platformName human-readable platform name for error messages
	 * @return role map binder
	 */
	public static @NotNull MapBinder<PlatformAdapterRole, Object> configure(
			@NotNull Binder binder,
			@NotNull String platformName
	) {
		binder.requestInjection(new PlatformAdapterContractValidator(platformName));
		return MapBinder.newMapBinder(binder, PlatformAdapterRole.class, Object.class);
	}
}
