package me.whereareiam.identica.account;

import me.whereareiam.identica.model.account.AccountPreparation;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Service for account resolution and preparation during authentication.
 */
@SuppressWarnings("unused")
public interface AccountService {
	/**
	 * Resolves or reserves an Identica account id for a profile rewrite request.
	 *
	 * @param request profile rewrite request
	 * @return resolved unique id or {@code null} when unavailable
	 */
	@Nullable UUID reserveAccountId(@NotNull ProfileRequest request);

	/**
	 * Clears any reservation entries for a profile request.
	 *
	 * @param request profile rewrite request
	 */
	void clearReservation(@NotNull ProfileRequest request);

	/**
	 * Prepares an account based on provider identity data.
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
}
