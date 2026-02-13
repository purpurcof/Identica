package me.whereareiam.identica.model.conflict;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Context passed to conflict resolvers.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class ConflictContext {
	/**
	 * Conflict key identifier (e.g. {@code username}, {@code email}).
	 */
	private @NotNull String key;

	/**
	 * Candidate value that is in conflict.
	 */
	private @NotNull String candidate;

	private @Nullable Account incomingAccount;
	private @Nullable AccountProviderLink incomingLink;
	private @Nullable AccountProviderProfile incomingProfile;

	private @Nullable Account existingAccount;
	private @Nullable AccountProviderLink existingLink;
	private @Nullable AccountProviderProfile existingProfile;

	/**
	 * Additional eligibility-specific attributes.
	 */
	@Builder.Default
	private @NotNull Map<String, Object> extras = new HashMap<>();

	/**
	 * Read a typed extra value.
	 *
	 * @param key extra key
	 * @param type expected type
	 * @return extra value or {@code null}
	 */
	public @Nullable <T> T getExtra(@NotNull String key, @NotNull Class<T> type) {
		Object value = extras.get(key);
		if (type.isInstance(value)) return type.cast(value);
		return null;
	}

	/**
	 * Store an extra value.
	 *
	 * @param key extra key
	 * @param value extra value
	 */
	public void putExtra(@NotNull String key, @Nullable Object value) {
		if (key.isBlank()) return;
		if (value == null) {
			extras.remove(key);
			return;
		}
		extras.put(key, value);
	}
}
