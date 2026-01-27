package me.whereareiam.identica.service;

import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.account.AccountPreparation;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Central account management service.
 *
 * <p>This service handles account lifecycle and session management.
 * For low-level persistence access, use the persistence services directly.</p>
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface AccountService {
	/**
	 * Prepare an account based on provider identity data.
	 *
	 * <pre>{@code
	 * AccountPreparation prep = accountService.prepareAccount(profile);
	 * if (prep.getDecision().isDenied()) {
	 *     return;
	 * }
	 * }</pre>
	 *
	 * @param profile provider profile data
	 * @return prepared account state
	 */
	default @NotNull AccountPreparation prepareAccount(@NotNull AccountProviderProfile profile) {
		return prepareAccount(profile, null);
	}

	/**
	 * Prepare an account based on provider identity data.
	 *
	 * <pre>{@code
	 * AccountPreparation prep = accountService.prepareAccount(profile, existingId);
	 * if (prep.getDecision().isDenied()) {
	 *     return;
	 * }
	 * }</pre>
	 *
	 * @param profile provider profile data
	 * @param existingId existing Identica id when known
	 * @return prepared account state
	 */
	@NotNull AccountPreparation prepareAccount(
			@NotNull AccountProviderProfile profile,
			@Nullable UUID existingId
	);

	/**
	 * Start a new session for the prepared account.
	 *
	 * @param preparation prepared account
	 * @param ip connection IP address
	 * @return opened session or {@code null} when preparation denied
	 */
	@Nullable Session startSession(@NotNull AccountPreparation preparation, @Nullable String ip);

}
