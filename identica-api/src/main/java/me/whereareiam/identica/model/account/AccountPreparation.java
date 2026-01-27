package me.whereareiam.identica.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Result of preparing an account based on provider identity data.
 */
@Getter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class AccountPreparation {
	private final @Nullable String effectiveUsername;
	private final @NotNull Account account;
	private final @NotNull Provider provider;

	private final @NotNull AccountDecision decision;
	private final boolean created;

	/**
	 * Provider-related data captured during account preparation.
	 */
	@Getter
	@ToString
	@Builder(toBuilder = true)
	@AllArgsConstructor
	public static class Provider {
		private final @NotNull AccountProviderLink link;
		private final @NotNull AccountProviderProfile profile;
	}
}
