package me.whereareiam.identica.provider.eligibility;

import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves whether a provider can handle a context for a flow.
 */
public interface ProviderEligibilityResolver {
	/**
	 * Checks whether the provider can handle the context.
	 *
	 * @param context authentication context
	 * @param provider provider instance
	 * @param flow current flow
	 * @return {@code true} if the provider can handle the context
	 */
	boolean isEligible(
			@NotNull AuthContext context,
			@NotNull InternalProvider provider,
			@NotNull AuthFlowType flow
	);
}
