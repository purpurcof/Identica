package me.whereareiam.identica.engine.pipeline.prepare.group.context.phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.migration.MigrationPendingState;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.prepare.PrepareContextItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.state.prepare.PrepareGroupState;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ResolvePendingMigrationContextPhase implements PipelinePhase<PrepareGroupState> {
	private final PipelineStateStore pipelineStateStore;

	@Override
	public @NotNull String id() {
		return "resolve-pending-migration-context";
	}

	@Override
	public int order() {
		return 300;
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

		String connectionKey = state.getRequest().getConnectionKey();
		if (connectionKey == null || connectionKey.isBlank())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		PipelineState pendingState = pipelineStateStore.find(PipelineStateReference.builder()
				.connectionKey(connectionKey)
				.build()).orElse(null);
		if (pendingState == null || pendingState.getPipelineType() != PipelineType.MIGRATION)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		if (pendingState.item(MigrationPendingState.class).isEmpty())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		MigrationContext migration = (MigrationContext) pendingState.getScenario(PipelineType.MIGRATION);
		if (migration == null || migration.getTargetProviderId() == null || migration.getTargetProviderId().isBlank())
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ProviderContext resolved = resolveProviderContext(migration, state);
		if (resolved == null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		PrepareContextItem context = pipelineState.item(PrepareContextItem.class).orElse(new PrepareContextItem());
		context.setProvider(resolved);
		pipelineState.putItem(context, 0L);
		Logger.debug(
				"Prepare resolved pending migration context username=%s provider=%s key=%s",
				state.getRequest().getIdentity().getUsername(),
				resolved.getProviderId(),
				connectionKey
		);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private ProviderContext resolveProviderContext(@NotNull MigrationContext migration, @NotNull PrepareGroupState state) {
		return ProviderContext.of(
				migration.getTargetProviderId(),
				null,
				state.getRequest().getIdentity().getUsername(),
				ProviderOrigin.MANUAL
		);
	}
}
