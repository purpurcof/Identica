package me.whereareiam.identica.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.engine.connection.ConnectionDecisionResolver;
import me.whereareiam.identica.engine.pipeline.scenario.AbstractScenarioPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.ScenarioRegistry;
import me.whereareiam.identica.engine.pipeline.scenario.ScenarioSelection;
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
import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitDecision;
import me.whereareiam.identica.ratelimit.RateLimitService;
import me.whereareiam.identica.type.ratelimit.RateLimitScope;
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
	private final RateLimitService rateLimitService;

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
		ConnectionDecision limited = checkRateLimit(RateLimitScope.PROCESS, rateLimitContext(request));
		if (limited != null) return CompletableFuture.completedFuture(limited);

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
		ConnectionDecision limited = checkRateLimit(RateLimitScope.RESUME, rateLimitContext(request));
		if (limited != null)
			return CompletableFuture.completedFuture(limited);

		AbstractScenarioPipeline runner = scenarioRegistry.selectForResume(request);
		CompletionStage<PipelineResult> execution = runner.execute(null, request);

		return execution.handle((result, error) -> resolveDecision(result, error, runner.type()));
	}

	@Override
	public @NotNull CompletionStage<ConnectionDecision> advanceFlow(
			@NotNull AdvanceRequest request
	) {
		ConnectionDecision limited = checkRateLimit(RateLimitScope.ADVANCE, rateLimitContext(request));
		if (limited != null)
			return CompletableFuture.completedFuture(limited);

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

	private @Nullable ConnectionDecision checkRateLimit(
			@NotNull RateLimitScope scope,
			@Nullable RateLimitContext ctx
	) {
		if (ctx == null) return null;
		RateLimitDecision decision = rateLimitService.evaluate(scope, ctx).orElse(null);
		if (decision == null || !decision.isLimited() || !decision.isDeny()) return null;
		return ConnectionDecision.deny(decision.getMessage());
	}

	private @Nullable RateLimitContext rateLimitContext(@Nullable ConnectionRequest request) {
		if (request == null) return null;
		return RateLimitContext.builder()
				.ip(request.getIp())
				.username(request.getUsername())
				.uniqueId(request.getIdentity().getUniqueId())
				.connectionUniqueId(request.getConnectionUniqueId())
				.build();
	}

	private @Nullable RateLimitContext rateLimitContext(@Nullable ResumeRequest request) {
		if (request == null) return null;
		return RateLimitContext.builder()
				.ip(request.getIp())
				.username(request.getUsername())
				.uniqueId(request.getIdentityUniqueId())
				.connectionUniqueId(request.getConnectionUniqueId())
				.build();
	}

	private @Nullable RateLimitContext rateLimitContext(@Nullable AdvanceRequest request) {
		if (request == null) return null;
		return RateLimitContext.builder()
				.ip(request.getIp())
				.username(request.getUsername())
				.uniqueId(request.getIdentityUniqueId())
				.connectionUniqueId(request.getConnectionUniqueId())
				.build();
	}
}
