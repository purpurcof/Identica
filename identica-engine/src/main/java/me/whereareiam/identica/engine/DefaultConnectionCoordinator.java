package me.whereareiam.identica.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.engine.connection.ConnectionDecisionResolver;
import me.whereareiam.identica.engine.pipeline.scenario.AbstractScenarioPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.ScenarioRegistry;
import me.whereareiam.identica.engine.pipeline.scenario.ScenarioSelection;
import me.whereareiam.identica.event.connection.attempt.ConnectionAdvanceAttemptEvent;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.connection.attempt.ConnectionAttemptEvent;
import me.whereareiam.identica.event.connection.attempt.ConnectionProcessAttemptEvent;
import me.whereareiam.identica.event.connection.attempt.ConnectionResumeAttemptEvent;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.engine.pipeline.handshake.HandshakePipeline;
import me.whereareiam.identica.identity.account.RegistrationAccountService;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.AdvanceRequest;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultConnectionCoordinator implements ConnectionCoordinator {
	// Identity/account lifecycle
	private final RegistrationAccountService registrationAccountService;
	private final ScenarioRegistry scenarioRegistry;
	private final ConnectionDecisionResolver decisionResolver;

	// Persistence/state
	private final PipelineStateStore pipelineStateStore;

	// Runtime orchestration
	private final HandshakePipeline handshakePipeline;
	private final EventManager eventManager;

	@Override
	public @NotNull CompletionStage<HandshakeDecision> handshake(@Nullable HandshakeRequest request) {
		return handshakePipeline.execute(request)
				.thenApply(decision -> decision != null
						? decision
						: HandshakeDecision.allow());
	}

	@Override
	public @Nullable UUID prepareProfile(@Nullable ProfileRequest request) {
		if (request == null) return null;
		return registrationAccountService.reserve(request);
	}

	@Override
	public @NotNull CompletionStage<ConnectionDecision> process(@Nullable ConnectionRequest request) {
		if (request != null) {
			ConnectionAttemptEvent entryEvent = new ConnectionProcessAttemptEvent(
					request.getConnectionUniqueId(),
					request.getIdentity().getUniqueId(),
					request.getUsername(),
					request.getIp()
			);
			eventManager.call(entryEvent);
			if (entryEvent.getDecision() != null)
				return CompletableFuture.completedFuture(entryEvent.getDecision());
		}

		ScenarioSelection selection = scenarioRegistry.select(request);
		if (selection.isResume()) {
			CompletionStage<PipelineResult> execution = selection.getRunner().execute(null, selection.getResumeRequest());

			return execution.handle((result, error) -> resolveDecision(result, error, selection.getRunner().type()))
					.thenCompose(decision -> {
						if (decision.getStatus() == ConnectionDecision.Status.NO_PENDING) {
							AbstractScenarioPipeline runner = scenarioRegistry.selectNewFlow(request);
							return executeNewPipeline(request, runner);
						}
						return CompletableFuture.completedFuture(decision);
					});
		}

		return executeNewPipeline(request, selection.getRunner());
	}

	@Override
	public @NotNull CompletionStage<ConnectionDecision> resume(
			@NotNull ResumeRequest request
	) {
		ConnectionAttemptEvent entryEvent = new ConnectionResumeAttemptEvent(
				request.getConnectionUniqueId(),
				request.getIdentityUniqueId(),
				request.getUsername(),
				request.getIp()
		);
		eventManager.call(entryEvent);
		if (entryEvent.getDecision() != null)
			return CompletableFuture.completedFuture(entryEvent.getDecision());

		AbstractScenarioPipeline runner = scenarioRegistry.selectForResume(request);
		CompletionStage<PipelineResult> execution = runner.execute(null, request);

		return execution.handle((result, error) -> resolveDecision(result, error, runner.type()));
	}

	@Override
	public @NotNull CompletionStage<ConnectionDecision> advanceFlow(
			@NotNull AdvanceRequest request
	) {
		ConnectionAttemptEvent entryEvent = new ConnectionAdvanceAttemptEvent(
				request.getConnectionUniqueId(),
				request.getIdentityUniqueId(),
				request.getUsername(),
				request.getIp()
		);
		eventManager.call(entryEvent);
		if (entryEvent.getDecision() != null)
			return CompletableFuture.completedFuture(entryEvent.getDecision());

		AbstractScenarioPipeline runner = scenarioRegistry.selectForAdvance(request);
		ResumeRequest pendingRequest = ResumeRequest.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identity(request.getIdentity())
				.intendedServer(request.getIntendedServer())
				.build();
		CompletionStage<PipelineResult> execution = runner.executeAdvance(pendingRequest);

		return execution.handle((result, error) -> resolveDecision(result, error, runner.type()));
	}

	@Override
	public boolean hasPending(@NotNull UUID connectionUniqueId) {
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();

		PipelineState state = pipelineStateStore.find(reference).orElse(null);
		if (state == null) return false;
		return scenarioRegistry.isPending(state);
	}

	private @NotNull ConnectionDecision resolveDecision(
			@Nullable PipelineResult result,
			@Nullable Throwable error,
			@NotNull PipelineType pipelineType
	) {
		return decisionResolver.resolveDecision(result, error, pipelineType);
	}

	private @NotNull CompletionStage<ConnectionDecision> executeNewPipeline(
			@Nullable ConnectionRequest request,
			@NotNull AbstractScenarioPipeline runner
	) {
		CompletionStage<PipelineResult> execution = runner.execute(request, null);
		return execution.handle((result, error) -> resolveDecision(result, error, runner.type()));
	}
}
