package me.whereareiam.identica.common.auth.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.step.StepFinishedEvent;
import me.whereareiam.identica.event.step.StepPrepareEvent;
import me.whereareiam.identica.event.step.StepStartedEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepAudience;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class StepExecutor {
	private final Provider<Messages> messagesProvider;
	private final EventManager eventManager;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;

	@NotNull
	public CompletionStage<StepExecution> execute(
			@Nullable IdenticaProvider provider,
			@NotNull AuthFlowType flow,
			@NotNull StepPhase phase,
			@NotNull List<AuthenticationStep> steps,
			@NotNull AuthContext context,
			int stepIndex,
			boolean requireCompletion
	) {
		if (stepIndex >= steps.size()) {
			if (requireCompletion) {
				StepResult failed = StepResult.failed(joinMessage(messagesProvider.get().getAuthentication().getNoCompletionStep()));
				return CompletableFuture.completedFuture(new StepExecution(failed, context, steps, stepIndex, false, stepIndex == 0));
			}

			return CompletableFuture.completedFuture(new StepExecution(StepResult.proceed(context), context, steps, stepIndex, true, false));
		}

		AuthenticationStep step = steps.get(stepIndex);
		if (!matchesAudience(step, context))
			return execute(provider, flow, phase, steps, context, stepIndex + 1, requireCompletion);

		if (!step.shouldExecute(context))
			return execute(provider, flow, phase, steps, context, stepIndex + 1, requireCompletion);

		eventManager.call(new StepPrepareEvent(provider, step, context, flow, phase));
		CompletableFuture<StepResult> execution = step.execute(context);
		eventManager.call(new StepStartedEvent(provider, step, context));

		return execution.thenCompose(result -> {
			if (result == null || result.getStatus() == null) {
				StepResult failed = StepResult.failed(joinMessage(messagesProvider.get().getAuthentication().getStepNoStatus()));
				eventManager.call(new StepFinishedEvent(provider, step, context, failed, phase));
				return CompletableFuture.completedFuture(new StepExecution(failed, context, steps, stepIndex, false, stepIndex == 0));
			}

			eventManager.call(new StepFinishedEvent(provider, step, context, result, phase));

			if (result.getStatus() != StepResult.StepStatus.CONTINUE)
				Logger.debug("Step %s returned %s", step.getName(), result.getStatus());

			AuthContext next = result.getUpdatedContext() != null ? result.getUpdatedContext() : context;
			return switch (result.getStatus()) {
				case CONTINUE -> execute(provider, flow, phase, steps, next, stepIndex + 1, requireCompletion);
				case WAITING -> CompletableFuture.completedFuture(new StepExecution(result, next, steps, stepIndex, false, false));
				case COMPLETE, DENIED, REQUIRE_RECONNECT ->
						CompletableFuture.completedFuture(new StepExecution(result, next, steps, stepIndex, false, false));
				case FAILED, NO_PENDING ->
						CompletableFuture.completedFuture(new StepExecution(result, next, steps, stepIndex, false, stepIndex == 0));
			};
		});
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

	private boolean matchesAudience(@NotNull AuthenticationStep step, @NotNull AuthContext context) {
		StepAudience audience = step.getAudience();
		if (audience == StepAudience.ALL)
			return true;

		UUID uniqueId = context.getIdenticaUniqueId();
		if (uniqueId == null)
			return audience == StepAudience.NEW_PLAYERS;

		boolean hasLinks = !providerLinkPersistenceService.findByUniqueId(uniqueId).isEmpty();
		return switch (audience) {
			case NEW_PLAYERS -> !hasLinks;
			case EXISTING_PLAYERS -> hasLinks;
			default -> true;
		};
	}

	@Getter
	@RequiredArgsConstructor
	public static final class StepExecution {
		private final @NotNull StepResult result;
		private final @NotNull AuthContext context;
		private final @NotNull List<AuthenticationStep> steps;
		private final int stepIndex;
		private final boolean stageComplete;
		private final boolean allowFallback;
	}
}
