package me.whereareiam.identica.common.auth.step.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.auth.step.registry.StepRegistry;
import me.whereareiam.identica.common.auth.step.EnrollmentStep;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.provider.ProviderDisabledEvent;
import me.whereareiam.identica.event.provider.ProviderUnloadedEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.step.StepDefinition;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class DefaultStepRegistry implements StepRegistry, EventListener {
	private final List<StepDefinition> definitions = new CopyOnWriteArrayList<>();

	@Inject
	public DefaultStepRegistry(
			EnrollmentStep enrollmentStep,
			EventManager eventManager
	) {
		register(null, StepPhase.PRE, 0, AuthFlowType.INTERACTIVE, enrollmentStep);
		eventManager.register(this);
	}

	@Override
	public void register(
			@Nullable String providerId,
			@NotNull StepPhase phase,
			int order,
			@NotNull AuthenticationStep step
	) {
		StepDefinition definition = new StepDefinition(providerId, phase, order, null, step);
		definitions.add(definition);
		logRegistration(definition);
	}

	@Override
	public void register(
			@Nullable String providerId,
			@NotNull StepPhase phase,
			int order,
			@NotNull AuthFlowType flow,
			@NotNull AuthenticationStep step
	) {
		StepDefinition definition = new StepDefinition(providerId, phase, order, flow, step);
		definitions.add(definition);
		logRegistration(definition);
	}

	@Override
	public @NotNull List<StepDefinition> resolve(
			@Nullable String providerId,
			@NotNull StepPhase phase,
			@NotNull AuthFlowType flow
	) {
		Map<String, StepDefinition> selected = new LinkedHashMap<>();
		for (StepDefinition definition : definitions) {
			if (!matchesProvider(definition, providerId)) continue;
			if (definition.getPhase() != phase) continue;

			AuthFlowType targetFlow = definition.getFlow();
			if (targetFlow != null && targetFlow != flow) continue;

			StepDefinition existing = selected.get(definition.getName());
			if (existing == null) {
				selected.put(definition.getName(), definition);
				continue;
			}

			if (existing.getFlow() == null && targetFlow == flow)
				selected.put(definition.getName(), definition);
		}

		List<StepDefinition> resolved = new ArrayList<>(selected.values());
		resolved.sort(Comparator.comparingInt(StepDefinition::getOrder)
				.thenComparing(StepDefinition::getName, String.CASE_INSENSITIVE_ORDER));
		return Collections.unmodifiableList(resolved);
	}

	@Override
	public @NotNull List<StepDefinition> getAll() {
		return Collections.unmodifiableList(definitions);
	}

	@IdenticEvent
	public void onProviderDisabled(ProviderDisabledEvent event) {
		if (event == null || event.getProvider().getDescriptor() == null)
			return;

		removeProviderSteps(event.getProvider().getDescriptor().getId());
	}

	@IdenticEvent
	public void onProviderUnloaded(ProviderUnloadedEvent event) {
		if (event == null || event.getProvider().getDescriptor() == null)
			return;

		removeProviderSteps(event.getProvider().getDescriptor().getId());
	}

	private void removeProviderSteps(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return;
		int before = definitions.size();
		definitions.removeIf(definition -> providerId.equalsIgnoreCase(definition.getProviderId()));

		int removed = before - definitions.size();
		if (removed > 0) Logger.debug("Removed %d step(s) for provider %s", removed, providerId);
	}

	private boolean matchesProvider(StepDefinition definition, @Nullable String providerId) {
		if (providerId == null)
			return definition.getProviderId() == null;

		String entry = definition.getProviderId();
		return entry != null && entry.equalsIgnoreCase(providerId);
	}

	private void logRegistration(@NotNull StepDefinition definition) {
		String providerId = definition.getProviderId() != null ? definition.getProviderId() : "global";
		String flow = definition.getFlow() != null ? definition.getFlow().name() : "any";
		Logger.debug("Registered step %s (phase: %s, flow: %s, provider: %s)",
				definition.getName(),
				definition.getPhase(),
				flow,
				providerId);
	}
}
