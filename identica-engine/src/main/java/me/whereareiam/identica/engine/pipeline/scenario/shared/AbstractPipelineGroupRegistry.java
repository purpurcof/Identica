package me.whereareiam.identica.engine.pipeline.scenario.shared;

import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.group.PipelineGroup;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhasePlacement;
import me.whereareiam.identica.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared registry runner for flow groups and phases.
 */
public abstract class AbstractPipelineGroupRegistry implements PipelineRegistry {
	private final List<PipelineGroup<?>> groups = new CopyOnWriteArrayList<>();
	private final Map<String, List<PipelinePhase<?>>> phasesByGroup = new ConcurrentHashMap<>();

	@Override
	public void register(@NotNull PipelineGroup<?> group) {
		String groupId = group.id();
		if (groupId.isBlank())
			return;

		unregister(groupId);
		groups.add(group);
		Logger.debug("Registered flow group %s in %s", groupId, registryLabel());
		phasesByGroup.computeIfAbsent(normalize(groupId), ignored -> new CopyOnWriteArrayList<>());
	}

	@Override
	public void unregister(@NotNull PipelineGroup<?> group) {
		unregister(group.id());
	}

	@Override
	public boolean unregister(@NotNull String groupId) {
		if (groupId.isBlank()) return false;

		boolean removed = groups.removeIf(group ->
				group != null && groupId.equalsIgnoreCase(group.id()));
		if (removed) {
			Logger.debug("Unregistered flow group %s in %s", groupId, registryLabel());
			phasesByGroup.remove(normalize(groupId));
		}

		return removed;
	}

	@Override
	public @NotNull List<PipelineGroup<?>> resolve(@NotNull PipelineState pipelineState) {
		List<PipelineGroup<?>> resolved = new ArrayList<>();
		for (PipelineGroup<?> group : groups) {
			if (group == null) continue;
			if (!group.supports(pipelineState)) continue;
			resolved.add(group);
		}

		sortGroups(resolved);
		return Collections.unmodifiableList(resolved);
	}

	@Override
	public @NotNull List<PipelineGroup<?>> getAll() {
		List<PipelineGroup<?>> snapshot = new ArrayList<>();
		for (PipelineGroup<?> group : groups) {
			if (group != null)
				snapshot.add(group);
		}

		sortGroups(snapshot);
		return Collections.unmodifiableList(snapshot);
	}

	@Override
	public void registerPhase(
			@NotNull String groupId,
			@NotNull PipelinePhase<?> phase,
			@NotNull PhasePlacement placement
	) {
		if (groupId.isBlank())
			return;

		String phaseId = phase.id();
		if (phaseId.isBlank())
			return;

		List<PipelinePhase<?>> phases = phasesByGroup.computeIfAbsent(normalize(groupId), ignored -> new CopyOnWriteArrayList<>());
		phases.removeIf(candidate -> candidate != null && phaseId.equalsIgnoreCase(candidate.id()));
		insertPhase(phases, phase, placement);

		Logger.debug("Registered %s %s for group %s", phaseLabel(), phaseId, groupId);
	}

	@Override
	public boolean unregisterPhase(@NotNull String groupId, @NotNull String phaseId) {
		if (groupId.isBlank() || phaseId.isBlank())
			return false;

		List<PipelinePhase<?>> phases = phasesByGroup.get(normalize(groupId));
		if (phases == null)
			return false;

		boolean removed = phases.removeIf(candidate -> candidate != null && phaseId.equalsIgnoreCase(candidate.id()));
		if (removed)
			Logger.debug("Unregistered %s %s for group %s", phaseLabel(), phaseId, groupId);
		return removed;
	}

	@Override
	public <S> @NotNull List<PipelinePhase<S>> resolvePhases(@NotNull String groupId, @NotNull Class<S> stateType) {
		List<PipelinePhase<?>> phases = phasesByGroup.get(normalize(groupId));
		if (phases == null)
			return List.of();

		List<PipelinePhase<S>> resolved = new ArrayList<>();
		for (PipelinePhase<?> phase : phases) {
			if (phase == null) continue;
			if (!stateType.equals(phase.stateType())) continue;

			@SuppressWarnings("unchecked")
			PipelinePhase<S> typed = (PipelinePhase<S>) phase;
			resolved.add(typed);
		}

		return List.copyOf(resolved);
	}

	protected abstract @NotNull String phaseLabel();

	protected @NotNull String registryLabel() {
		return getClass().getSimpleName();
	}

	private void insertPhase(
			@NotNull List<PipelinePhase<?>> phases,
			@NotNull PipelinePhase<?> phase,
			@NotNull PhasePlacement placement
	) {
		PhasePlacement.Type type = placement.getType();
		if (type == PhasePlacement.Type.FIRST) {
			phases.addFirst(phase);
			return;
		}

		if (type == PhasePlacement.Type.BEFORE || type == PhasePlacement.Type.AFTER) {
			String anchor = placement.getAnchorPhaseId();
			int anchorIndex = findPhaseIndex(phases, anchor);
			if (anchorIndex >= 0) {
				if (type == PhasePlacement.Type.BEFORE)
					phases.add(anchorIndex, phase);
				else
					phases.add(anchorIndex + 1, phase);
				return;
			}
		}

		phases.add(phase);
	}

	private int findPhaseIndex(@NotNull List<PipelinePhase<?>> phases, String phaseId) {
		if (phaseId == null || phaseId.isBlank())
			return -1;

		for (int index = 0; index < phases.size(); index++) {
			PipelinePhase<?> phase = phases.get(index);
			if (phase == null) continue;
			if (phaseId.equalsIgnoreCase(phase.id()))
				return index;
		}

		return -1;
	}

	private @NotNull String normalize(@NotNull String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private void sortGroups(@NotNull List<PipelineGroup<?>> orderedGroups) {
		orderedGroups.sort(Comparator.comparingInt((PipelineGroup<?> group) -> group.order())
				.thenComparing(PipelineGroup::id, String.CASE_INSENSITIVE_ORDER));
	}
}
