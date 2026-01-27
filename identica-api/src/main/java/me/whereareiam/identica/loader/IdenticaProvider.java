package me.whereareiam.identica.loader;

import lombok.Setter;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
import me.whereareiam.identica.conflict.resolver.ConflictResolver;
import me.whereareiam.identica.conflict.ConflictType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

import com.google.inject.Module;

@Setter
@SuppressWarnings("unused")
public abstract class IdenticaProvider {
	protected ProviderDescriptor descriptor;
	protected Path workingPath;

	/**
	 * Provides dependency metadata for this provider.
	 *
	 * @return provider libraries descriptor
	 */
	public @NotNull ProviderLibraries libraries() {
		return ProviderLibraries.empty();
	}

	/**
	 * Provides Guice modules for this provider.
	 *
	 * @return list of modules to install
	 */
	public @NotNull List<Module> modules() {
		return List.of();
	}

	/**
	 * Define the authentication step pipeline for this provider.
	 *
	 * @return ordered list of step instances
	 */
	public abstract @NotNull List<AuthenticationStep> getAuthenticationSteps();

	/**
	 * Provide conflict resolvers owned by this provider.
	 *
	 * @return list of provider-defined resolvers
	 */
	public @NotNull List<ConflictResolver> getConflictResolvers() {
		return List.of();
	}

	/**
	 * Provide conflict types owned by this provider.
	 *
	 * @return list of provider-defined conflict types
	 */
	public @NotNull List<ConflictType> getConflictTypes() {
		return List.of();
	}

	/**
	 * Called after the provider is loaded.
	 */
	public void onLoad() {

	}

	/**
	 * Called when the provider is enabled.
	 */
	public void onEnable() {

	}

	/**
	 * Called when the provider is disabled.
	 */
	public void onDisable() {

	}

	/**
	 * Called when the provider is unloaded.
	 */
	public void onUnload() {

	}
}
