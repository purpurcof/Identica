package me.whereareiam.identica.type.provider.capability;

/**
 * Runtime scope declared by a provider capability bootstrap.
 */
public enum ProviderCapabilityScope {
	/**
	 * Installs shared runtime services once for the whole Identica runtime.
	 */
	GLOBAL,

	/**
	 * Installs provider-local bindings in the provider child injector.
	 */
	LOCAL
}
