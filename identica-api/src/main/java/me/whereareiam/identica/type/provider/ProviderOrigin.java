package me.whereareiam.identica.type.provider;

/**
 * Source used to select a provider for a connection.
 */
public enum ProviderOrigin {
	/**
	 * Provider selected from a configured entrypoint hostname.
	 */
	ENTRYPOINT,

	/**
	 * Provider selected explicitly by a user or administrator.
	 */
	MANUAL,

	/**
	 * Provider selected automatically by the platform or core journeyMode.
	 */
	AUTO
}
