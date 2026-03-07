package me.whereareiam.identica.engine.prepare.group.context.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.prepare.ConnectionProviderContextResolver;
import me.whereareiam.identica.engine.prepare.group.PrepareGroupState;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolveEntrypointPhase implements PipelinePhase<PrepareGroupState> {
	private final ProviderOperations providerOperations;
	private final ConnectionProviderContextResolver providerContextResolver;

	@Override
	public @NotNull String id() {
		return "resolve-entrypoint";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<PrepareGroupState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull PrepareGroupState state
	) {
		if (state.getRequest() == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		var identity = state.getRequest().getIdentity();
		var origin = identity.getOrigin();
		if (origin == null || origin.getHost().isBlank())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		int port = origin.getPort() != null ? origin.getPort() : -1;
		var resolved = providerOperations.resolveEntrypoint(origin.getHost(), port);
		if (resolved == null) return CompletableFuture.completedFuture(PhaseResult.pass(state));

		Logger.debug("Prepare matched entrypoint username=%s provider=%s",
				identity.getUsername(),
				resolved.getProviderId());
		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(new PrepareContextItem());
		context.setProvider(ProviderContext.of(
				resolved.getProviderId(),
				null,
				identity.getUsername(),
				providerContextResolver.resolveSource(identity, resolved.getProviderId())
		));
		pipelineState.putItem(context, 0L);

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}
}
