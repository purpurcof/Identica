package me.whereareiam.identica.provider.capability.restriction.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Shared runtime settings for the restriction capability.
 */
@Getter
@Setter
@ToString
public class RestrictionSettings extends ConfigDocument {
	private @NotNull Duration toggleTtl;
	private @NotNull Replication replication;

	/**
	 * Returns toggle TTL in milliseconds with validation.
	 *
	 * @return toggle TTL in milliseconds
	 */
	public long toggleTtlMillis() {
		if (toggleTtl.isZero() || toggleTtl.isNegative())
			throw new IllegalStateException("providers.capabilities.restriction.settings.toggleTtl must be positive");

		return toggleTtl.toMillis();
	}

	@Getter
	@Setter
	@ToString
	public static class Replication {
		private @NotNull String toggleNamespace;
	}
}
