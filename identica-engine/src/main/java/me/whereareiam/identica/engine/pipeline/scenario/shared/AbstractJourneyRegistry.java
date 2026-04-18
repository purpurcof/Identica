package me.whereareiam.identica.engine.pipeline.scenario.shared;

import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.pipeline.journey.registry.JourneyRegistry;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractJourneyRegistry implements JourneyRegistry {
	private final List<JourneyStage> stages = new CopyOnWriteArrayList<>();
	private final List<JourneyStep> steps = new CopyOnWriteArrayList<>();

	@Override
	public void registerStage(@NotNull JourneyStage stage) {
		if (stage.getId().isBlank())
			return;

		unregisterStage(stage.getId());
		stages.add(stage);
		Logger.debug("Registered journey stage %s in %s", stage.getId(), registryLabel());
	}

	@Override
	public void unregisterStage(@NotNull JourneyStage stage) {
		unregisterStage(stage.getId());
	}

	@Override
	public boolean unregisterStage(@NotNull String stageId) {
		if (stageId.isBlank())
			return false;
		String normalized = normalize(stageId);
		boolean removed = stages.removeIf(stage -> normalize(stage.getId()).equals(normalized));
		if (removed) {
			steps.removeIf(step -> normalize(step.getStageId()).equals(normalized));
		}
		return removed;
	}

	@Override
	public void registerStep(@NotNull JourneyStep step) {
		if (step.getStageId().isBlank() || step.getStep().getName().isBlank())
			return;

		JourneyStep normalized = normalizeFlows(step);
		boolean replaced = steps.removeIf(existing -> isDuplicate(existing, normalized));
		if (replaced) {
			Logger.warn(
					"Journey step %s already registered for stage %s, replacing registration",
					normalized.getStep().getName(),
					normalized.getStageId()
			);
		}

		steps.add(normalized);
	}

	@Override
	public boolean removeStep(
			@NotNull String stageId,
			@Nullable String providerId,
			@NotNull PipelineType pipelineType,
			@NotNull String stepName
	) {
		if (stageId.isBlank() || stepName.isBlank())
			return false;

		String normalizedStageId = normalize(stageId);
		return steps.removeIf(step -> {
			if (!normalize(step.getStageId()).equals(normalizedStageId))
				return false;
			if (!step.getStep().getName().equalsIgnoreCase(stepName))
				return false;
			if (!step.supports(pipelineType, JourneyType.SEAMLESS, providerId)
					&& !step.supports(pipelineType, JourneyType.INTERACTIVE, providerId))
				return false;
			return matchesProvider(step.getProviderId(), providerId);
		});
	}

	@Override
	public @NotNull JourneyPlan resolvePlan(
			@NotNull ScenarioContext context,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable String providerId
	) {
		List<JourneyStage> sortedStages = new ArrayList<>();
		for (JourneyStage stage : stages) {
			if (stage == null)
				continue;
			if (!stage.supports(pipelineType, flow))
				continue;
			sortedStages.add(stage);
		}

		sortedStages.sort(Comparator
				.comparingInt((JourneyStage stage) -> stage.getType().order())
				.thenComparingInt(JourneyStage::getOrder)
				.thenComparing(JourneyStage::getId, String.CASE_INSENSITIVE_ORDER));

		List<JourneyPlan.StageEntry> entries = new ArrayList<>();
		for (JourneyStage stage : sortedStages) {
			List<JourneyStep> resolvedSteps = resolveStageSteps(stage, pipelineType, flow, providerId);
			entries.add(new JourneyPlan.StageEntry(stage, resolvedSteps));
		}

		return new JourneyPlan(entries);
	}

	@Override
	public @NotNull List<JourneyStage> getAllStages() {
		List<JourneyStage> snapshot = new ArrayList<>(stages);
		snapshot.sort(Comparator
				.comparingInt((JourneyStage stage) -> stage.getType().order())
				.thenComparingInt(JourneyStage::getOrder)
				.thenComparing(JourneyStage::getId, String.CASE_INSENSITIVE_ORDER));
		return List.copyOf(snapshot);
	}

	private @NotNull List<JourneyStep> resolveStageSteps(
			@NotNull JourneyStage stage,
			@NotNull PipelineType pipelineType,
			@NotNull JourneyType flow,
			@Nullable String providerId
	) {
		Map<String, JourneyStep> selected = new LinkedHashMap<>();
		String stageId = normalize(stage.getId());

		for (JourneyStep candidate : steps) {
			if (candidate == null)
				continue;
			if (!normalize(candidate.getStageId()).equals(stageId))
				continue;
			if (!candidate.supports(pipelineType, flow, providerId))
				continue;

			String stepName = candidate.getStep().getName().toLowerCase(Locale.ROOT);
			JourneyStep existing = selected.get(stepName);
			if (existing == null || comparePriority(candidate, existing, flow) > 0)
				selected.put(stepName, candidate);
		}

		List<JourneyStep> resolved = new ArrayList<>(selected.values());
		resolved.sort(Comparator
				.comparingInt(JourneyStep::resolvedOrder)
				.thenComparing(step -> step.getStep().getName(), String.CASE_INSENSITIVE_ORDER));
		return List.copyOf(resolved);
	}

	private int comparePriority(
			@NotNull JourneyStep candidate,
			@NotNull JourneyStep existing,
			@NotNull JourneyType flow
	) {
		int candidateScore = 0;
		if (candidate.isProviderSpecific())
			candidateScore += 2;
		if (candidate.isFlowSpecific(flow))
			candidateScore += 1;

		int existingScore = 0;
		if (existing.isProviderSpecific())
			existingScore += 2;
		if (existing.isFlowSpecific(flow))
			existingScore += 1;

		return Integer.compare(candidateScore, existingScore);
	}

	private boolean isDuplicate(@NotNull JourneyStep existing, @NotNull JourneyStep incoming) {
		if (!normalize(existing.getStageId()).equals(normalize(incoming.getStageId())))
			return false;
		if (!existing.getStep().getName().equalsIgnoreCase(incoming.getStep().getName()))
			return false;
		if (!matchesProvider(existing.getProviderId(), incoming.getProviderId()))
			return false;
		if (!safeScenarios(existing.getScenarios()).equals(safeScenarios(incoming.getScenarios())))
			return false;
		return safeFlows(existing.getFlows()).equals(safeFlows(incoming.getFlows()));
	}

	private boolean matchesProvider(@Nullable String registeredProviderId, @Nullable String providerId) {
		if (registeredProviderId == null || registeredProviderId.isBlank())
			return providerId == null || providerId.isBlank();
		if (providerId == null || providerId.isBlank())
			return false;
		return registeredProviderId.equalsIgnoreCase(providerId);
	}

	private @NotNull String normalize(@NotNull String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private @NotNull Set<PipelineType> safeScenarios(@Nullable Set<PipelineType> values) {
		if (values == null || values.isEmpty())
			return EnumSet.allOf(PipelineType.class);
		return values;
	}

	private @NotNull Set<JourneyType> safeFlows(@Nullable Set<JourneyType> values) {
		if (values == null || values.isEmpty())
			return EnumSet.allOf(JourneyType.class);
		return values;
	}

	private @NotNull JourneyStep normalizeFlows(@NotNull JourneyStep step) {
		Set<JourneyType> flows = step.getFlows();
		if (!flows.isEmpty())
			return step;

		boolean interactive = step.getStep() instanceof InteractiveStep;
		EnumSet<JourneyType> resolved;
		if (interactive)
			resolved = EnumSet.of(JourneyType.INTERACTIVE);
		else
			resolved = EnumSet.allOf(JourneyType.class);

		return step.toBuilder()
				.flows(resolved)
				.build();
	}

	protected @NotNull String registryLabel() {
		return getClass().getSimpleName();
	}
}
