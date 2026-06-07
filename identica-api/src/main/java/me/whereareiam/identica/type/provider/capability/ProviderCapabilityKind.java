package me.whereareiam.identica.type.provider.capability;

/**
 * Classifies how a provider capability participates in the runtime.
 */
public enum ProviderCapabilityKind {
	/**
	 * Declares provider support for a core-owned behavior without installing
	 * capability-owned runtime modules.
	 */
	MARKER,
	/**
	 * Declares and installs capability-owned runtime behavior.
	 */
	RUNTIME
}
