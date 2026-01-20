package me.whereareiam.identica.common.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.auth.AuthenticationStep;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.provider.InternalProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AuthPipeline {
	private final ProviderManager providerManager;
	private final Map<UUID, WaitingPipeline> waitingPipelines = new ConcurrentHashMap<>();

	public CompletableFuture<StepResult> authenticate(AuthContext context) {
		List<InternalProvider> providers = providerManager.getProviders();
		return executeProviders(providers, context);
	}

	public CompletableFuture<StepResult> resume(UUID connectionUniqueId) {
		if (connectionUniqueId == null) {
			return CompletableFuture.completedFuture(StepResult.failed("Missing connection id"));
		}

		WaitingPipeline waiting = waitingPipelines.remove(connectionUniqueId);
		if (waiting == null) {
			return CompletableFuture.completedFuture(StepResult.failed("No pending authentication"));
		}

		return executeStepPipeline(waiting.provider, waiting.steps, waiting.context, waiting.stepIndex + 1)
				.thenApply(result -> result.result);
	}

	private CompletableFuture<StepResult> executeProviders(List<InternalProvider> providers, AuthContext context) {
		if (providers == null || providers.isEmpty()) {
			return CompletableFuture.completedFuture(StepResult.failed("No providers available"));
		}

		return executeProviderIndex(providers, context, 0);
	}

	private CompletableFuture<StepResult> executeProviderIndex(List<InternalProvider> providers, AuthContext context, int index) {
		if (index >= providers.size()) {
			return CompletableFuture.completedFuture(StepResult.failed("No providers matched"));
		}

		IdenticaProvider provider = providers.get(index).getProvider();
		if (provider == null) {
			return executeProviderIndex(providers, context, index + 1);
		}

		List<AuthenticationStep> steps = provider.getAuthenticationSteps();
		if (steps == null || steps.isEmpty()) {
			return executeProviderIndex(providers, context, index + 1);
		}

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
					StepResult.failed("No completion step"),
					stepIndex == 0
			));
		}

		AuthenticationStep step = steps.get(stepIndex);
		if (!step.shouldExecute(context)) {
			return executeStepPipeline(provider, steps, context, stepIndex + 1);
		}

		return step.execute(context).thenCompose(result -> {
			if (result == null || result.getStatus() == null) {
				return CompletableFuture.completedFuture(new PipelineResult(
						StepResult.failed("Step returned no status"),
						stepIndex == 0
				));
			}

			return switch (result.getStatus()) {
				case CONTINUE -> executeStepPipeline(provider, steps, context, stepIndex + 1);
				case WAITING -> {
					storeWaitingPipeline(context, provider, steps, stepIndex);
					yield CompletableFuture.completedFuture(new PipelineResult(result, false));
				}
				case COMPLETE -> CompletableFuture.completedFuture(new PipelineResult(result, false));
				case FAILED -> CompletableFuture.completedFuture(new PipelineResult(result, stepIndex == 0));
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
		if (connectionUuid == null) {
			return;
		}
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
}
