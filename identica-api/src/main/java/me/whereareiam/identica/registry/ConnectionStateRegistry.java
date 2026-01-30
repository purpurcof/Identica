package me.whereareiam.identica.registry;

import me.whereareiam.identica.model.connection.ConnectionState;
import me.whereareiam.identica.model.connection.FlowState;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.stage.PendingStage;
import me.whereareiam.identica.stage.StepStage;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Registry for connection-scoped state such as routing and flow progress.
 */
@SuppressWarnings("unused")
public interface ConnectionStateRegistry {
	/**
	 * Returns an existing connection state or creates a new one.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return connection state
	 */
	@NotNull ConnectionState ensure(@NotNull UUID connectionUniqueId);

	/**
	 * Returns a connection state when present.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return optional connection state
	 */
	@NotNull Optional<ConnectionState> find(@NotNull UUID connectionUniqueId);

	/**
	 * Restores a pending flow state and clears its snapshot.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return optional flow state
	 */
	@NotNull Optional<FlowState> consumePending(@NotNull UUID connectionUniqueId);

	/**
	 * Returns the routing target for this connection when available.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return optional routing target
	 */
	@NotNull Optional<RoutingTarget> peekRoutingTarget(@NotNull UUID connectionUniqueId);

	/**
	 * Returns all tracked connection states.
	 *
	 * @return collection of connection states
	 */
	@NotNull Collection<ConnectionState> getStates();

	/**
	 * Clears the connection state for the provided id.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return {@code true} if a state was cleared
	 */
	boolean clear(@NotNull UUID connectionUniqueId);

	/**
	 * Returns whether this connection has a pending authentication flow.
	 *
	 * @param connectionUniqueId connection unique id
	 * @return {@code true} when a pending flow is available
	 */
	boolean hasPending(@NotNull UUID connectionUniqueId);

	/**
	 * Clears any stored pending flow for this connection.
	 *
	 * @param connectionUniqueId connection unique id
	 */
	void clearPending(@NotNull UUID connectionUniqueId);

	/**
	 * Stores a pending authentication flow snapshot from the provided connection state.
	 * No-op when the state has no pending flow.
	 *
	 * @param connectionState connection state to read from
	 */
	void storePending(@NotNull ConnectionState connectionState);

	/**
	 * Stores a pending authentication flow snapshot for later resume.
	 *
	 * @param context authentication context
	 * @param flow flow type
	 * @param stages resolved stages
	 * @param stageIndex current stage index
	 * @param pendingStage pending stage snapshot
	 * @param completionResult completion result, if present
	 */
	void storePending(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@NotNull List<StepStage> stages,
			int stageIndex,
			@NotNull PendingStage pendingStage,
			@Nullable StepResult completionResult
	);
}
