package me.whereareiam.identica.pipeline;

import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.phase.PhasePlacement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registry for pipeline groups and phases.
 */
public interface PipelineRegistry {
	/**
	 * Registers a pipeline group.
	 *
	 * @param group group to register
	 */
	void register(@NotNull PipelineGroup<?> group);

	/**
	 * Unregisters a pipeline group instance.
	 *
	 * @param group group to unregister
	 */
	void unregister(@NotNull PipelineGroup<?> group);

	/**
	 * Unregisters a pipeline group by id.
	 *
	 * @param groupId group identifier
	 * @return {@code true} if a group was removed
	 */
	boolean unregister(@NotNull String groupId);

	/**
	 * Resolves the groups that match the provided pipeline state.
	 *
	 * @param pipelineState pipeline state
	 * @return resolved groups
	 */
	@NotNull List<PipelineGroup<?>> resolve(@NotNull PipelineState pipelineState);

	/**
	 * Returns all registered pipeline groups.
	 *
	 * @return registered groups
	 */
	@NotNull List<PipelineGroup<?>> getAll();

	/**
	 * Registers a pipeline phase in the provided group with placement rules.
	 *
	 * @param groupId group identifier
	 * @param phase pipeline phase to register
	 * @param placement placement rules
	 */
	void registerPhase(@NotNull String groupId, @NotNull PipelinePhase<?> phase, @NotNull PhasePlacement placement);

	/**
	 * Unregisters a pipeline phase by id from the provided group.
	 *
	 * @param groupId group identifier
	 * @param phaseId phase identifier
	 * @return {@code true} if a phase was removed
	 */
	boolean unregisterPhase(@NotNull String groupId, @NotNull String phaseId);

	/**
	 * Resolves phases for the provided group and state type.
	 *
	 * @param groupId group identifier
	 * @param stateType state type
	 * @param <S> state type
	 * @return resolved phases
	 */
	<S> @NotNull List<PipelinePhase<S>> resolvePhases(@NotNull String groupId, @NotNull Class<S> stateType);
}
