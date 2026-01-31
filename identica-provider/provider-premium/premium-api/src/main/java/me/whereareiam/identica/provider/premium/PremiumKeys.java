package me.whereareiam.identica.provider.premium;

import me.whereareiam.identica.Key;

/**
 * Keys used by the premium provider to store profile observation data.
 */
public final class PremiumKeys {
	/**
	 * Platform profile UUID observed during profile request handling.
	 */
	public static final Key<String> PLATFORM_PROFILE_ID = Key.create(
			"premium.platformProfileId",
			String.class
	);
}
