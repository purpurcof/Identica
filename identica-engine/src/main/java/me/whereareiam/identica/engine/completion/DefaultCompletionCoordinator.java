package me.whereareiam.identica.engine.completion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.pipeline.completion.CompletionCoordinator;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.pipeline.completion.step.CompletionStep;
import me.whereareiam.identica.pipeline.completion.CompletionPendingStore;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultCompletionCoordinator implements CompletionCoordinator {
	private final CompletionPendingStore completionPendingStore;
	private final CompletionExtensionRegistry completionExtensionRegistry;
	private final SessionService sessionService;
	private final ProviderManager providerManager;

	@Override
	public void consumeAndExecute(@NotNull Identity identity) {
		CompletionPendingState pendingState = completionPendingStore.consume(identity.getUniqueId()).orElse(null);
		if (pendingState == null) return;

		execute(identity, pendingState);
	}

	@Override
	public void execute(@NotNull Identity identity, @NotNull CompletionPendingState pendingState) {
		UUID identicaUniqueId = pendingState.getIdenticaUniqueId();
		if (identicaUniqueId == null) return;

		Session session = sessionService.findByUniqueId(identicaUniqueId).join().orElse(null);
		if (session == null) return;

		execute(identity, pendingState.getPipelineType(), session);
	}

	@Override
	public void execute(
			@NotNull Identity identity,
			@NotNull PipelineType pipelineType,
			@NotNull Session session
	) {
		String providerId = session.getProviderId();
		if (providerId == null || providerId.isBlank())
			return;

		InternalProvider provider = resolveProvider(providerId);
		CompletionContext context = CompletionContext.builder()
				.identity(identity)
				.pipelineType(pipelineType)
				.session(session)
				.provider(provider)
				.build();

		List<CompletionStep> steps = completionExtensionRegistry.resolve(providerId, pipelineType);
		for (CompletionStep step : steps) {
			if (step == null)
				continue;
			if (!step.shouldExecute(context))
				continue;

			try {
				step.execute(context);
			} catch (Exception exception) {
				Logger.warn(
						"Completion step %s failed for provider=%s pipeline=%s: %s",
						step.getName(),
						providerId,
						pipelineType,
						exception.getMessage()
				);
			}
		}
	}

	private @Nullable InternalProvider resolveProvider(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank())
			return null;

		for (InternalProvider provider : providerManager.getProviders()) {
			if (provider == null || provider.getDescriptor() == null) continue;

			String currentId = provider.getDescriptor().getId();
			if (currentId.equalsIgnoreCase(providerId)) return provider;
		}

		return null;
	}
}
