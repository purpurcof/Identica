package me.whereareiam.identica.feature.verification;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Registry for managing registered verification methods.
 *
 * <p>This API exposes the runtime method catalog used by verification flows.
 * Core code and addons can register method handlers, inspect the available
 * handlers, and resolve a handler by its id.</p>
 */
public interface VerificationRegistry {
	/**
	 * Registers a verification method handler.
	 *
	 * @param handler method handler to register
	 */
	void register(@NotNull VerificationMethod handler);

	/**
	 * Unregisters a verification method handler.
	 *
	 * @param handler method handler to unregister
	 */
	void unregister(@NotNull VerificationMethod handler);

	/**
	 * Returns all currently registered verification method handlers.
	 *
	 * @return immutable snapshot of registered handlers
	 */
	@NotNull Set<VerificationMethod> values();

	/**
	 * Resolves a verification method handler by id.
	 *
	 * @param id method id to resolve
	 * @return resolved handler, or empty when missing
	 */
	@NotNull Optional<VerificationMethod> find(@Nullable String id);
}
