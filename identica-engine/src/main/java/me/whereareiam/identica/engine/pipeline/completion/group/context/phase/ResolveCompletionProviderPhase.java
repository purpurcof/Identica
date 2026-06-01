package me.whereareiam.identica.engine.pipeline.completion.group.context.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.completion.CompletionPipelineState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.provider.ProviderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolveCompletionProviderPhase implements PipelinePhase<CompletionPipelineState> {
	private final ProviderManager providerManager;

	@Override
	public @NotNull String id() {
		return "resolve-completion-provider";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<CompletionPipelineState> stateType() {
		return CompletionPipelineState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<CompletionPipelineState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull CompletionPipelineState state
	) {
		Session session = state.getSession();
		String providerId = session != null ? session.getProviderId() : null;
		state.setProvider(resolveProvider(providerId));
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @Nullable InternalProvider resolveProvider(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;
		for (InternalProvider provider : providerManager.getProviders()) {
			if (provider == null || provider.getDescriptor() == null) continue;

			String currentId = provider.getDescriptor().getId();
			if (currentId.equalsIgnoreCase(providerId)) return provider;
		}

		return null;
	}
}
