package me.whereareiam.identica.engine.pipeline.scenario.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.engine.pipeline.scenario.AbstractScenarioPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class MigrationPipeline extends AbstractScenarioPipeline {
	@Inject
	public MigrationPipeline(
			@Named("migrationPipelineRegistry") PipelineRegistry registry,
			Provider<Messages> messagesProvider,
			Provider<Settings> settingsProvider,
			PipelineStateStore pipelineStateStore
	) {
		super(registry, messagesProvider, settingsProvider, pipelineStateStore, PipelineType.MIGRATION);
	}

	@Override
	protected @Nullable ScenarioContext buildContext(@NotNull ConnectionRequest request) {
		if (request.getIdentity().getUniqueId() == null) {
			UUID fallbackUniqueId = request.getConnectionUniqueId();
			if (fallbackUniqueId == null)
				fallbackUniqueId = UniqueIdGenerator.offlinePlayerUniqueId(request.getUsername());

			if (fallbackUniqueId == null) {
				Logger.severe("%s request missing Identica UUID and fallback UUID", type());
				return null;
			}

			Logger.warn("%s request missing Identica UUID, applying fallback UUID %s", type(), fallbackUniqueId);
			request.getIdentity().setUniqueId(fallbackUniqueId);
		}

		MigrationContext context = MigrationContext.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identity(request.getIdentity())
				.intendedServer(request.getIntendedServer())
				.build();

		emitScenarioBuilt(context);

		return context;
	}

	@Override
	protected @NotNull ScenarioContext mergeContext(@NotNull ScenarioContext base, @NotNull ResumeRequest request) {
		ConnectionIdentity identity = mergeIdentity(base, request);
		if (identity == null) return base;

		String intendedServer = request.getIntendedServer() != null
				? request.getIntendedServer()
				: base.getIntendedServer();

		if (base instanceof MigrationContext migration) {
			MigrationContext merged = MigrationContext.builder()
					.connectionUniqueId(identity.getUniqueId())
					.identity(identity)
					.intendedServer(intendedServer)
					.targetProviderId(migration.getTargetProviderId())
					.build();

			merged.setProvider(migration.getProvider());
			return merged;
		}

		return base;
	}

	@Override
	protected boolean isPending(@NotNull PipelineState state) {
		return state.item(JourneyStateItem.class).isPresent();
	}

	@Override
	protected void onStart(@NotNull PipelineState pipelineState, boolean resumed) {
		IdentityMetaItem identity = pipelineState.item(IdentityMetaItem.class).orElse(null);
		if (identity == null) identity = new IdentityMetaItem();

		identity.setResumed(resumed);
		pipelineState.putItem(identity, 0L);
	}
}
