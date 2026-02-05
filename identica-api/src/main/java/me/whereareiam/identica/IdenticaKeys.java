package me.whereareiam.identica;

import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

/**
 * Shared keys used across Identica contexts.
 */
public final class IdenticaKeys {
	/**
	 * Stores the resolved flow for the current authentication attempt.
	 */
	public static final @NotNull Key<AuthFlowType> CURRENT_FLOW =
			Key.create("identica:current_flow", AuthFlowType.class);

	/**
	 * Stores the profile provider id observed during profile rewrite.
	 */
	public static final @NotNull Key<String> PROFILE_PROVIDER_ID =
			Key.create("identica:profile_provider_id", String.class);

	/**
	 * Stores the profile provider subject observed during profile rewrite.
	 */
	public static final @NotNull Key<String> PROFILE_PROVIDER_SUBJECT =
			Key.create("identica:profile_provider_subject", String.class);
}
