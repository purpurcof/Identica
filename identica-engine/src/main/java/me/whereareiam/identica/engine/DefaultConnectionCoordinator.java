package me.whereareiam.identica.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.engine.connection.ConnectionDecisionResolver;
import me.whereareiam.identica.engine.connection.ConnectionScenarioSelector;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.AuthenticationPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.registration.RegistrationPipeline;
import me.whereareiam.identica.engine.pipeline.handshake.HandshakePipeline;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.auth.AuthPendingClearedEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.account.RegistrationAccountService;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.pipeline.journey.JourneyPendingState;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultConnectionCoordinator implements ConnectionCoordinator, EventListener {
	// Pipelines
	private final RegistrationPipeline registrationPipeline;
	private final AuthenticationPipeline authenticationPipeline;

	// Identity/account lifecycle
	private final RegistrationAccountService registrationAccountService;
	private final IdentityService identityService;
	private final ConnectionScenarioSelector scenarioSelector;
	private final ConnectionDecisionResolver decisionResolver;

	// Persistence/state
	private final PipelineStateStore pipelineStateStore;

	// Runtime orchestration
	private final HandshakePipeline handshakePipeline;
	private final EventManager eventManager;

	@Inject
	void registerListeners() {
		eventManager.register(this);
	}

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
		ConnectionScenarioSelector.ScenarioSelection selection = scenarioSelector.select(request);
		if (selection.resume()) {
			CompletionStage<PipelineResult> execution = selection.registration()
					? registrationPipeline.execute(null, selection.resumeRequest())
					: authenticationPipeline.execute(null, selection.resumeRequest());

			return execution.handle((result, error) -> resolveDecision(result, error, selection.registration()))
					.thenCompose(decision -> {
						if (decision.getStatus() == ConnectionDecision.Status.NO_PENDING)
							return executeNewPipeline(request, selection.registration());
						return CompletableFuture.completedFuture(decision);
					});
		}

		return executeNewPipeline(request, selection.registration());
	}

	@Override
	public @NotNull CompletionStage<ConnectionDecision> resume(
			@NotNull ResumeRequest request
	) {
		boolean registration = scenarioSelector.isRegistration(request);
		CompletionStage<PipelineResult> execution = registration
				? registrationPipeline.execute(null, request)
				: authenticationPipeline.execute(null, request);

		return execution.handle((result, error) -> resolveDecision(result, error, registration));
	}

	@Override
	public boolean hasPending(@NotNull UUID connectionUniqueId) {
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();

		return pipelineStateStore.find(reference)
				.map(state -> state.item(JourneyPendingState.class).isPresent())
				.orElse(false);
	}

	@Override
	public boolean clearPending(@NotNull UUID connectionUniqueId) {
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();

		boolean removed = pipelineStateStore.consume(reference).isPresent();
		eventManager.call(new AuthPendingClearedEvent(connectionUniqueId, removed));

		return removed;
	}

	@IdenticEvent(EventOrder.LOW)
	public void onAccountClear(@NotNull AccountClearEvent event) {
		UUID connectionUniqueId = event.getIdentity().getUniqueId();
		if (connectionUniqueId == null) {
			String username = event.getIdentity().getUsername();
			if (!username.isBlank()) {
				connectionUniqueId = identityService.find(username)
						.map(Identity::getUniqueId)
						.orElse(null);
			}
		}
		if (connectionUniqueId == null)
			return;

		clearPending(connectionUniqueId);
	}

	private @NotNull ConnectionDecision resolveDecision(
			@Nullable PipelineResult result,
			@Nullable Throwable error,
			boolean registration
	) {
		return decisionResolver.resolveDecision(result, error, registration);
	}

	private @NotNull CompletionStage<ConnectionDecision> executeNewPipeline(
			@Nullable ConnectionRequest request,
			boolean registration
	) {
		CompletionStage<PipelineResult> execution = registration
				? registrationPipeline.execute(request)
				: authenticationPipeline.execute(request);
		return execution.handle((result, error) -> resolveDecision(result, error, registration));
	}
}
