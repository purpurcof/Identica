package me.whereareiam.identica.identity.account;

import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.AccountOperationRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for account lookup and lifecycle operations.
 */
@SuppressWarnings("unused")
public interface AccountService {
	/**
	 * Find an account by its unique id.
	 *
	 * @param uniqueId account id
	 * @return account or empty if not found
	 */
	@NotNull Optional<Account> find(@NotNull UUID uniqueId);

	/**
	 * Find accounts by username.
	 *
	 * @param username account username
	 * @return matching accounts
	 */
	@NotNull List<Account> find(@NotNull String username);

	/**
	 * Create a new account.
	 *
	 * @param account account to create
	 * @return created account
	 */
	@NotNull Account create(@NotNull Account account);

	/**
	 * Clear account-owned data while preserving the account UUID.
	 *
	 * @param request clear request
	 */
	void clear(@NotNull AccountOperationRequest request);

	/**
	 * Delete the account and its UUID completely.
	 *
	 * @param request delete request
	 */
	void delete(@NotNull AccountOperationRequest request);
}
