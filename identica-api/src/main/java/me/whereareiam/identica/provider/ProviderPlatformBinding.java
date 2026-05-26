package me.whereareiam.identica.provider;

/**
 * Represents provider-owned platform behavior that must be attached to or
 * detached from core platform extension points during provider lifecycle
 * transitions.
 */
public interface ProviderPlatformBinding {
	/**
	 * Attaches this binding to the active platform runtime.
	 */
	void register();

	/**
	 * Detaches this binding from the active platform runtime.
	 */
	void unregister();
}
