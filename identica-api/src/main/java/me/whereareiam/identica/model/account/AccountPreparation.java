package me.whereareiam.identica.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.model.Session;
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
	 * Builds a session snapshot from this preparation.
	 *
	 * <p>This method assumes the caller already verified that the preparation
	 * decision is allowed.</p>
	 *
	 * <pre>{@code
	 * if (preparation.getDecision().isDenied()) {
	 *     return;
	 * }
	 * Session session = preparation.toSession(ip);
	 * identityService.openSession(session);
	 * }</pre>
	 *
	 * @param ip connection IP address
	 * @return built session instance
	 */
	public @NotNull Session toSession(@Nullable String ip) {
		AccountProviderLink link = provider.getLink();
		AccountProviderProfile profile = provider.getProfile();

		String providerUsername = profile.getProviderUsername();
		String originalUsername = providerUsername.isBlank()
				? account.getUsername()
				: providerUsername;

		String resolvedEffective = effectiveUsername;
		if (resolvedEffective == null || resolvedEffective.isBlank())
			resolvedEffective = account.getUsername();

		return Session.builder()
				.uniqueId(account.getUniqueId())
				.providerId(link.getProviderId())
				.providerSubject(link.getProviderSubject())
				.originalUsername(originalUsername)
				.effectiveUsername(resolvedEffective)
				.ip(ip)
				.createdAt(System.currentTimeMillis())
				.build();
	}

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
