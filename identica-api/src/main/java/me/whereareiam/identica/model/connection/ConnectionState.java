package me.whereareiam.identica.model.connection;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.AuthContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Mutable connection-scoped state for a single connection.
 */
@SuppressWarnings("UnusedReturnValue")
@RequiredArgsConstructor
public class ConnectionState {
	private final @NotNull UUID connectionUniqueId;
	private volatile @Nullable AuthContext context;
	private volatile @Nullable RoutingTarget routingTarget;
	private volatile @Nullable FlowState flowState;

	/**
	 * Returns the connection unique id.
	 *
	 * @return connection unique id
	 */
	public @NotNull UUID getConnectionUniqueId() {
		return connectionUniqueId;
	}

	/**
	 * Stores the authentication context for this connection.
	 *
	 * @param context authentication context
	 */
	public synchronized void putContext(@NotNull AuthContext context) {
		this.context = context;
	}

	/**
	 * Returns the stored authentication context without clearing it.
	 *
	 * @return optional authentication context
	 */
	public synchronized @NotNull Optional<AuthContext> peekContext() {
		return Optional.ofNullable(context);
	}

	/**
	 * Returns the stored authentication context and clears it.
	 *
	 * @return optional authentication context
	 */
	public synchronized @NotNull Optional<AuthContext> consumeContext() {
		AuthContext current = context;
		context = null;
		return Optional.ofNullable(current);
	}

	/**
	 * Clears the stored authentication context.
	 *
	 * @return {@code true} if a context was cleared
	 */
	public synchronized boolean clearContext() {
		boolean had = context != null;
		context = null;
		return had;
	}

	/**
	 * Stores the routing target for this connection.
	 *
	 * @param target routing target
	 */
	public synchronized void putRoutingTarget(@NotNull RoutingTarget target) {
		this.routingTarget = target;
	}

	/**
	 * Returns the routing target without clearing it.
	 *
	 * @return optional routing target
	 */
	public synchronized @NotNull Optional<RoutingTarget> peekRoutingTarget() {
		return Optional.ofNullable(routingTarget);
	}

	/**
	 * Returns the routing target and clears it.
	 *
	 * @return optional routing target
	 */
	public synchronized @NotNull Optional<RoutingTarget> consumeRoutingTarget() {
		RoutingTarget current = routingTarget;
		routingTarget = null;
		return Optional.ofNullable(current);
	}

	/**
	 * Clears the routing target.
	 *
	 * @return {@code true} if a target was cleared
	 */
	public synchronized boolean clearRoutingTarget() {
		boolean had = routingTarget != null;
		routingTarget = null;
		return had;
	}

	/**
	 * Stores the flow state for this connection.
	 *
	 * @param flowState flow state snapshot
	 */
	public synchronized void putFlowState(@NotNull FlowState flowState) {
		this.flowState = flowState;
	}

	/**
	 * Returns the flow state without clearing it.
	 *
	 * @return optional flow state
	 */
	public synchronized @NotNull Optional<FlowState> peekFlowState() {
		return Optional.ofNullable(flowState);
	}

	/**
	 * Returns the flow state and clears it.
	 *
	 * @return optional flow state
	 */
	public synchronized @NotNull Optional<FlowState> consumeFlowState() {
		FlowState current = flowState;
		flowState = null;
		return Optional.ofNullable(current);
	}

	/**
	 * Clears the flow state.
	 *
	 * @return {@code true} if a flow state was cleared
	 */
	public synchronized boolean clearFlowState() {
		boolean had = flowState != null;
		flowState = null;
		return had;
	}
}
