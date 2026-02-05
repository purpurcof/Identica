package me.whereareiam.identica.common.auth.stage.runner;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.common.auth.stage.StageOutcome;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.common.auth.step.StepExecutor;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.provider.ProviderSelectedEvent;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.auth.stage.PendingStage;
import me.whereareiam.identica.auth.stage.StepStage;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProviderStageRunner {
	private final ProviderManager providerManager;
	private final ProviderEligibilityService eligibilityService;
	private final StepExecutor stepExecutor;
	private final Provider<Messages> messagesProvider;
	private final EventManager eventManager;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;

	@NotNull
	public CompletionStage<StageOutcome> run(
			@NotNull StepStage stage,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult
	) {
		boolean fallbackAllowed = stage.allowFallback(context, flow);
		boolean allowAutoSelect = hasProviderLinks(context);
		String providerId = resolveSelectedProviderId(context);
		if (providerId != null) {
			InternalProvider provider = findProviderById(providerId);
			if (provider != null && eligibilityService.isEligible(context, provider, flow)) {
				return executeProviderSteps(stage, provider, context, flow, completionResult, null, -1, null, 0, fallbackAllowed);
			}

			if (!fallbackAllowed) {
				if (allowAutoSelect) {
					List<InternalProvider> providers = eligibilityService.eligibleProviders(context, flow);
					if (!providers.isEmpty())
						return executeProviderIndex(stage, providers, context, flow, 0, completionResult);
				}
				return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));
			}
		}

		if (!fallbackAllowed) {
			if (allowAutoSelect) {
				List<InternalProvider> providers = eligibilityService.eligibleProviders(context, flow);
				if (!providers.isEmpty())
					return executeProviderIndex(stage, providers, context, flow, 0, completionResult);
			}
			return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));
		}

		List<InternalProvider> providers = eligibilityService.eligibleProviders(context, flow);
		if (providers.isEmpty())
			return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));

		return executeProviderIndex(stage, providers, context, flow, 0, completionResult);
	}

	@NotNull
	public CompletionStage<StageOutcome> resume(
			@NotNull StepStage stage,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult,
			@NotNull PendingStage pendingStage
	) {
		boolean fallbackAllowed = stage.allowFallback(context, flow);
		boolean allowAutoSelect = hasProviderLinks(context);
		InternalProvider provider = findProviderById(pendingStage.getProviderId());
		if (provider == null || !eligibilityService.isEligible(context, provider, flow)) {
			if (!fallbackAllowed) {
				if (allowAutoSelect) {
					List<InternalProvider> providers = eligibilityService.eligibleProviders(context, flow);
					if (!providers.isEmpty())
						return executeProviderIndex(stage, providers, context, flow, 0, completionResult);
				}
				return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));
			}

			List<InternalProvider> providers = eligibilityService.eligibleProviders(context, flow);
			if (providers.isEmpty())
				return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));

			return executeProviderIndex(stage, providers, context, flow, 0, completionResult);
		}

		return executeProviderSteps(stage, provider, context, flow, completionResult, null, -1,
				pendingStage.getSteps(), pendingStage.getStepIndex() + 1, fallbackAllowed);
	}

	@NotNull
	public StepResult noProvidersResult() {
		List<InternalProvider> providers = providerManager.getProviders();
		if (providers == null || providers.isEmpty())
			return StepResult.failed(joinMessage(messagesProvider.get().getProviders().getNoProvidersAvailable()));

		return StepResult.failed(joinMessage(messagesProvider.get().getProviders().getNoProvidersMatched()));
	}

	private CompletionStage<StageOutcome> executeProviderIndex(
			@NotNull StepStage stage,
			@NotNull List<InternalProvider> providers,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			int index,
			@Nullable StepResult completionResult
	) {
		if (index >= providers.size())
			return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));

		InternalProvider provider = providers.get(index);
		if (provider == null)
			return executeProviderIndex(stage, providers, context, flow, index + 1, completionResult);

		if (!eligibilityService.isEligible(context, provider, flow))
			return executeProviderIndex(stage, providers, context, flow, index + 1, completionResult);

		return executeProviderSteps(stage, provider, context, flow, completionResult,
				providers, index, null, 0, true);
	}

	private CompletionStage<StageOutcome> executeProviderSteps(
			@NotNull StepStage stage,
			@NotNull InternalProvider provider,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult,
			@Nullable List<InternalProvider> providers,
			int providerIndex,
			@Nullable List<AuthenticationStep> stepsOverride,
			int stepIndex,
			boolean fallbackAllowed
	) {
		String providerId = resolveProviderId(provider);
		if (providerId == null)
			return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));

		selectProvider(context, providerId);
		eventManager.call(new ProviderSelectedEvent(context, provider, flow));

		List<AuthenticationStep> steps = stepsOverride != null
				? stepsOverride
				: stage.steps(context, flow, provider);

		IdenticaProvider providerInstance = provider.getProvider();
		return stepExecutor.execute(providerInstance, flow, stage.phase(), steps, context, stepIndex, stage.requireCompletion())
				.thenCompose(result -> handleProviderResult(stage, provider, flow, completionResult, providers, providerIndex,
						steps, fallbackAllowed, result));
	}

	private CompletionStage<StageOutcome> handleProviderResult(
			@NotNull StepStage stage,
			@NotNull InternalProvider provider,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult,
			@Nullable List<InternalProvider> providers,
			int providerIndex,
			@NotNull List<AuthenticationStep> steps,
			boolean fallbackAllowed,
			@NotNull StepExecutor.StepExecution result
	) {
		StepResult stepResult = result.getResult();
		AuthContext next = result.getContext();

		if (stepResult.getStatus() == StepResult.StepStatus.WAITING) {
			if (flow == AuthFlowType.SEAMLESS) {
				return CompletableFuture.completedFuture(StageOutcome.result(StepResult.failed(
						joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed())
				), next, completionResult));
			}

			PendingStage pendingStage = new PendingStage(resolveProviderId(provider), steps, result.getStepIndex());
			return CompletableFuture.completedFuture(StageOutcome.waiting(stepResult, next, completionResult, pendingStage));
		}

		if (stepResult.getStatus() == StepResult.StepStatus.COMPLETE) {
			if (stage.usesCompletionResult())
				return CompletableFuture.completedFuture(StageOutcome.advance(stepResult, next, stepResult));

			return CompletableFuture.completedFuture(StageOutcome.result(stepResult, next, completionResult));
		}

		if (stepResult.getStatus() == StepResult.StepStatus.REQUIRE_RECONNECT) {
			int resumeIndex = result.getStepIndex() - 1;
			PendingStage pendingStage = new PendingStage(resolveProviderId(provider), steps, resumeIndex);
			return CompletableFuture.completedFuture(StageOutcome.waiting(stepResult, next, completionResult, pendingStage));
		}

		if (stepResult.getStatus() == StepResult.StepStatus.CONTINUE && result.isStageComplete())
			return CompletableFuture.completedFuture(StageOutcome.advance(stepResult, next, completionResult));

		if ((stepResult.getStatus() == StepResult.StepStatus.FAILED || stepResult.getStatus() == StepResult.StepStatus.NO_PENDING)
				&& fallbackAllowed && result.isAllowFallback()) {
			return executeProviderFallback(stage, provider, next, flow, completionResult, providers, providerIndex);
		}

		return CompletableFuture.completedFuture(StageOutcome.result(stepResult, next, completionResult));
	}

	private CompletionStage<StageOutcome> executeProviderFallback(
			@NotNull StepStage stage,
			@NotNull InternalProvider provider,
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable StepResult completionResult,
			@Nullable List<InternalProvider> providers,
			int providerIndex
	) {
		List<InternalProvider> candidates = providers;
		int nextIndex = providerIndex + 1;

		if (candidates == null) {
			candidates = eligibilityService.eligibleProviders(context, flow);
			if (candidates.isEmpty())
				return CompletableFuture.completedFuture(StageOutcome.result(noProvidersResult(), context, completionResult));

			int currentIndex = indexOfProvider(candidates, provider);
			nextIndex = currentIndex + 1;
		}

		if (nextIndex < 0) nextIndex = 0;
		return executeProviderIndex(stage, candidates, context, flow, nextIndex, completionResult);
	}

	private String resolveSelectedProviderId(@NotNull AuthContext context) {
		if (context.getProvider() == null)
			return null;
		return context.getProvider().getProviderId();
	}

	private void selectProvider(@NotNull AuthContext context, @NotNull String providerId) {
		AuthContext.Provider provider = context.getProvider();
		if (provider == null) {
			String username = context.getUsername() != null ? context.getUsername() : "";
			context.setProvider(AuthContext.Provider.builder()
					.providerId(providerId)
					.providerUsername(username)
					.build());
			return;
		}

		if (provider.getProviderId() == null || provider.getProviderId().isBlank())
			provider.setProviderId(providerId);
	}

	private InternalProvider findProviderById(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;
		List<InternalProvider> providers = providerManager.getProviders();
		if (providers == null) return null;

		for (InternalProvider provider : providers) {
			if (provider == null || provider.getDescriptor() == null) continue;
			String id = provider.getDescriptor().getId();
			if (id.equalsIgnoreCase(providerId))
				return provider;
		}

		return null;
	}

	private int indexOfProvider(@Nullable List<InternalProvider> providers, @NotNull InternalProvider provider) {
		if (providers == null || providers.isEmpty())
			return -1;

		String providerId = resolveProviderId(provider);
		if (providerId == null)
			return providers.indexOf(provider);

		for (int i = 0; i < providers.size(); i++) {
			InternalProvider entry = providers.get(i);
			if (entry == null) continue;
			String id = resolveProviderId(entry);
			if (id != null && id.equalsIgnoreCase(providerId))
				return i;
		}

		return -1;
	}

	private String resolveProviderId(@Nullable InternalProvider provider) {
		if (provider == null || provider.getDescriptor() == null) return null;
		String id = provider.getDescriptor().getId();
		return !id.isBlank() ? id : null;
	}

	private boolean hasProviderLinks(@NotNull AuthContext context) {
		UUID uniqueId = context.getIdenticaUniqueId();
		if (uniqueId == null)
			return false;

		return !providerLinkPersistenceService.findByUniqueId(uniqueId).isEmpty();
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
