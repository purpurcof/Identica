package me.whereareiam.identica.provider.credential.account;

import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Service for managing credential-provider accounts.
 */
public interface CredentialAccountService {
	/**
	 * Find a credential account by provider subject.
	 *
	 * @param providerSubject provider subject to search by
	 * @return account when found, otherwise empty
	 */
	@NotNull Optional<CredentialAccount> find(@Nullable String providerSubject);

	/**
	 * Register a new credential using a pre-hashed password and hashing method.
	 *
	 * @param providerSubject provider subject to register
	 * @param passwordHash hashed password
	 * @param hashingMethod hashing method id
	 * @param reason password change reason
	 * @return newly created account or empty when the account already exists
	 */
	@NotNull Optional<CredentialAccount> register(
			@NotNull String providerSubject,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason
	);

	/**
	 * Update the credential password hash and hashing method.
	 *
	 * @param credential credential to update
	 * @param passwordHash hashed password
	 * @param hashingMethod hashing method id
	 * @param reason password change reason
	 * @return {@code true} when the password was updated
	 */
	boolean updatePassword(
			@NotNull CredentialAccount credential,
			@NotNull String passwordHash,
			@NotNull String hashingMethod,
			@NotNull PasswordChangeReason reason
	);

	/**
	 * Delete a credential by provider subject.
	 *
	 * @param providerSubject provider subject to delete
	 */
	void delete(@NotNull String providerSubject);
}
