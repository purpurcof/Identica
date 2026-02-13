package me.whereareiam.identica.identity.account;

import me.whereareiam.identica.model.auth.request.ProfileRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Service for account registration reservation operations.
 */
@SuppressWarnings("unused")
public interface RegistrationAccountService {
	/**
	 * Resolves or reserves an Identica account id for a resolver rewrite request.
	 *
	 * @param request resolver rewrite request
	 * @return resolved unique id or {@code null} when unavailable
	 */
	@Nullable UUID reserve(@NotNull ProfileRequest request);

	/**
	 * Clears any reservation entries for a resolver request.
	 *
	 * @param request resolver rewrite request
	 */
	void clearReservation(@NotNull ProfileRequest request);
}
