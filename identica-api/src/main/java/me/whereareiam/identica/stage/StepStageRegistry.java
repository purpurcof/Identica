package me.whereareiam.identica.stage;

import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registry for authentication stages.
 */
public interface StepStageRegistry {
	/**
	 * Registers a new stage.
	 *
	 * @param stage stage to register
	 */
	void register(@NotNull StepStage stage);

	/**
	 * Unregisters a stage instance.
	 *
	 * @param stage stage to unregister
	 */
	void unregister(@NotNull StepStage stage);

	/**
	 * Unregisters a stage by id.
	 *
	 * @param stageId stage identifier
	 * @return {@code true} if a stage was removed
	 */
	boolean unregister(@NotNull String stageId);

	/**
	 * Resolves stages for the given flow and context.
	 *
	 * @param context authentication context
	 * @param flow flow type
	 * @return resolved stages
	 */
	@NotNull List<StepStage> resolve(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	);

	/**
	 * Returns all registered stages.
	 *
	 * @return registered stages
	 */
	@NotNull List<StepStage> getAll();
}
