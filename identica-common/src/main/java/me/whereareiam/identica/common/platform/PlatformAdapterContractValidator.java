package me.whereareiam.identica.common.platform;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.type.platform.PlatformAdapterRole;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;

/**
 * Validates that a platform has bound every required adapter role.
 */
@RequiredArgsConstructor
public final class PlatformAdapterContractValidator {
	private final String platformName;

	@Inject
	void validate(@NotNull Map<PlatformAdapterRole, Object> adapters) {
		EnumSet<PlatformAdapterRole> missing = EnumSet.allOf(PlatformAdapterRole.class);
		missing.removeAll(adapters.keySet());
		if (!missing.isEmpty()) {
			throw new IllegalStateException("Missing required " + platformName + " platform adapters: " + missing);
		}
	}
}
