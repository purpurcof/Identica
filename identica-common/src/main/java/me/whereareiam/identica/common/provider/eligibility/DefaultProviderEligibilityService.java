package me.whereareiam.identica.common.provider.eligibility;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.auth.step.type.InteractiveStep;
import me.whereareiam.identica.auth.step.registry.StepRegistry;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.provider.ProviderEligibilityEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.step.StepDefinition;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.provider.ProviderState;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultProviderEligibilityService implements ProviderEligibilityService {
	private final ProviderManager providerManager;
	private final StepRegistry stepRegistry;
	private final EventManager eventManager;

	@Override
	public @NotNull List<InternalProvider> eligibleProviders(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	) {
		List<InternalProvider> providers = providerManager.getProviders();
		if (providers == null || providers.isEmpty()) return List.of();

		List<InternalProvider> eligible = new ArrayList<>();
		for (InternalProvider provider : providers)
			if (provider != null && isEligible(context, provider, flow))
				eligible.add(provider);

		eligible.sort(Comparator.comparingInt(InternalProvider::getPriority)
				.reversed()
				.thenComparing(this::resolveId, String.CASE_INSENSITIVE_ORDER));

		return List.copyOf(eligible);
	}

	@Override
	public boolean isEligible(
			@NotNull AuthContext context,
			@NotNull InternalProvider provider,
			@NotNull AuthFlowType flow
	) {
		if (provider.getState() != ProviderState.ENABLED) return false;

		ProviderDescriptor descriptor = provider.getDescriptor();
		if (descriptor == null || isBlank(descriptor.getId())) return false;

		if (!supportsFlow(descriptor.getId(), flow)) return false;
		if (!resolversAllow(context, provider, flow)) return false;

		ProviderEligibilityEvent event = new ProviderEligibilityEvent(context, provider, flow);
		eventManager.call(event);
		return !event.isCancelled();
	}

	private boolean supportsFlow(@NotNull String providerId, @NotNull AuthFlowType flow) {
		List<StepDefinition> steps = stepRegistry.resolve(providerId, StepPhase.PROVIDER, flow);
		if (steps.isEmpty()) return false;

		if (flow != AuthFlowType.SEAMLESS)
			return true;

		for (StepDefinition definition : steps)
			if (definition.getStep() instanceof InteractiveStep)
				return false;

		return true;
	}

	private boolean resolversAllow(
			@NotNull AuthContext context,
			@NotNull InternalProvider provider,
			@NotNull AuthFlowType flow
	) {
		Set<ProviderEligibilityResolver> resolvers = provider.getEligibilityResolvers();
		if (resolvers == null || resolvers.isEmpty())
			return true;

		for (ProviderEligibilityResolver resolver : resolvers)
			if (!resolver.isEligible(context, provider, flow))
				return false;

		return true;
	}

	private String resolveId(InternalProvider provider) {
		ProviderDescriptor descriptor = provider != null ? provider.getDescriptor() : null;
		String id = descriptor != null ? descriptor.getId() : null;
		return id != null ? id : "";
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
