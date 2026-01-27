package me.whereareiam.identica.common.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthenticationService;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.AuthPendingClearedEvent;
import me.whereareiam.identica.event.auth.AuthProviderSelectedEvent;
import me.whereareiam.identica.event.auth.step.AuthStepFinishedEvent;
import me.whereareiam.identica.event.auth.step.AuthStepStartedEvent;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.provider.InternalProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AuthPipeline implements AuthenticationService {
	private final ProviderManager providerManager;
	private final Provider<Messages> messagesProvider;
	private final EventManager eventManager;
	private final Map<UUID, WaitingPipeline> waitingPipelines = new ConcurrentHashMap<>();

	@Override
	public CompletableFuture<StepResult> authenticate(AuthContext context) {
		List<InternalProvider> providers = providerManager.getProviders();
		return executeProviders(providers, context);
	}

	@Override
	public CompletableFuture<StepResult> resume(UUID connectionUniqueId) {
		return resume(connectionUniqueId, null);
	}

	@Override
	public CompletableFuture<StepResult> resume(
			UUID connectionUniqueId,
			Consumer<AuthContext> contextUpdater
	) {
		if (connectionUniqueId == null) {
			return CompletableFuture.completedFuture(StepResult.failed(
					joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed())
			));
		}

		WaitingPipeline waiting = waitingPipelines.remove(connectionUniqueId);
		if (waiting == null) {
			return CompletableFuture.completedFuture(StepResult.noPending());
		}

		if (contextUpdater != null) {
			contextUpdater.accept(waiting.context);
		}

		return executeStepPipeline(waiting.provider, waiting.steps, waiting.context, waiting.stepIndex + 1)
				.thenApply(result -> result.result);
	}

	@Override
	public boolean hasPending(UUID connectionUniqueId) {
		return connectionUniqueId != null && waitingPipelines.containsKey(connectionUniqueId);
	}

	@Override
	public boolean clearPending(UUID connectionUniqueId) {
		if (connectionUniqueId == null)
			return false;

		boolean removed = waitingPipelines.remove(connectionUniqueId) != null;
		eventManager.call(new AuthPendingClearedEvent(connectionUniqueId, removed));

		return removed;
	}

	private CompletableFuture<StepResult> executeProviders(List<InternalProvider> providers, AuthContext context) {
		if (providers == null || providers.isEmpty()) {
			return CompletableFuture.completedFuture(StepResult.failed(
					String.join("\n", messagesProvider.get().getProviders().getNoProvidersAvailable())
			));
		}

		return executeProviderIndex(providers, context, 0);
	}

	private CompletableFuture<StepResult> executeProviderIndex(List<InternalProvider> providers, AuthContext context, int index) {
		if (index >= providers.size()) {
			return CompletableFuture.completedFuture(StepResult.failed(
					String.join("\n", messagesProvider.get().getProviders().getNoProvidersMatched())
			));
		}

		IdenticaProvider provider = providers.get(index).getProvider();
		if (provider == null) return executeProviderIndex(providers, context, index + 1);
		eventManager.call(new AuthProviderSelectedEvent(provider, context));

		List<AuthenticationStep> steps = provider.getAuthenticationSteps();
		if (steps.isEmpty()) return executeProviderIndex(providers, context, index + 1);

		return executeStepPipeline(provider, steps, context, 0)
				.thenCompose(result -> {
					if (result.allowFallback && result.result.getStatus() == StepResult.StepStatus.FAILED) {
						return executeProviderIndex(providers, context, index + 1);
					}
					return CompletableFuture.completedFuture(result.result);
				});
	}

	private CompletableFuture<PipelineResult> executeStepPipeline(
			IdenticaProvider provider,
			List<AuthenticationStep> steps,
			AuthContext context,
			int stepIndex
	) {
		if (stepIndex >= steps.size()) {
			return CompletableFuture.completedFuture(new PipelineResult(
					StepResult.failed(joinMessage(messagesProvider.get().getAuthentication().getNoCompletionStep())),
					stepIndex == 0
			));
		}

		AuthenticationStep step = steps.get(stepIndex);
		if (!step.shouldExecute(context)) return executeStepPipeline(provider, steps, context, stepIndex + 1);

		eventManager.call(new AuthStepStartedEvent(provider, step, context));

		return step.execute(context).thenCompose(result -> {
			if (result == null || result.getStatus() == null) {
				StepResult failed = StepResult.failed(joinMessage(messagesProvider.get().getAuthentication().getStepNoStatus()));
				eventManager.call(new AuthStepFinishedEvent(provider, step, context, failed));

				return CompletableFuture.completedFuture(new PipelineResult(
						failed,
						stepIndex == 0
				));
			}

			eventManager.call(new AuthStepFinishedEvent(provider, step, context, result));

			return switch (result.getStatus()) {
				case CONTINUE -> executeStepPipeline(provider, steps, context, stepIndex + 1);
				case WAITING -> {
					storeWaitingPipeline(context, provider, steps, stepIndex);
					yield CompletableFuture.completedFuture(new PipelineResult(result, false));
				}
				case COMPLETE, DENIED, REQUIRE_RECONNECT -> CompletableFuture.completedFuture(new PipelineResult(result, false));
				case FAILED, NO_PENDING -> CompletableFuture.completedFuture(new PipelineResult(result, stepIndex == 0));
			};
		});
	}

	private void storeWaitingPipeline(
			AuthContext context,
			IdenticaProvider provider,
			List<AuthenticationStep> steps,
			int stepIndex
	) {
		UUID connectionUuid = context.getConnectionUniqueId();
		if (connectionUuid == null)
			return;

		waitingPipelines.put(connectionUuid, new WaitingPipeline(provider, steps, context, stepIndex));
	}

	private record WaitingPipeline(
			IdenticaProvider provider,
			List<AuthenticationStep> steps,
			AuthContext context,
			int stepIndex
	) {
	}

	private record PipelineResult(StepResult result, boolean allowFallback) {
	}

	private String joinMessage(List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
