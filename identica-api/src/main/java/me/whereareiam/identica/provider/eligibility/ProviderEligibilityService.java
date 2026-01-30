package me.whereareiam.identica.provider.eligibility;

import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Service for resolving eligible providers.
 */
public interface ProviderEligibilityService {
	/**
	 * Returns eligible providers for the context and flow.
	 *
	 * @param context authentication context
	 * @param flow flow type
	 * @return eligible providers
	 */
	@NotNull List<InternalProvider> eligibleProviders(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	);

	/**
	 * Checks whether the provider can handle the context for the flow.
	 *
	 * @param context authentication context
	 * @param provider provider instance
	 * @param flow flow type
	 * @return {@code true} if eligible
	 */
	boolean isEligible(
			@NotNull AuthContext context,
			@NotNull InternalProvider provider,
			@NotNull AuthFlowType flow
	);
}
