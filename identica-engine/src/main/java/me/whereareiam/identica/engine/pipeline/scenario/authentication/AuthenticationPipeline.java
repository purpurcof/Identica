package me.whereareiam.identica.engine.pipeline.scenario.authentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.engine.pipeline.PipelineExecutor;
import me.whereareiam.identica.engine.pipeline.scenario.AbstractScenarioPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem;
import me.whereareiam.identica.event.pipeline.scenario.authentication.AuthenticationContextBuiltEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.PipelineRegistry;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.util.EventUtil;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class AuthenticationPipeline extends AbstractScenarioPipeline {
	private final ProviderLinkPersistenceService providerLinkPersistenceService;

	@Inject
	public AuthenticationPipeline(
			@Named("authenticationPipelineRegistry") PipelineRegistry registry,
			Provider<Messages> messagesProvider,
			Provider<Settings> settingsProvider,
			PipelineStateStore pipelineStateStore,
			PipelineExecutor executor,
			ProviderLinkPersistenceService providerLinkPersistenceService
	) {
		super(registry, messagesProvider, settingsProvider, pipelineStateStore, PipelineType.AUTHENTICATION, executor);
		this.providerLinkPersistenceService = providerLinkPersistenceService;
	}

	@Override
	protected @Nullable ScenarioContext buildContext(@NotNull ConnectionRequest request) {
		if (request.getIdentity().getAccountUniqueId() == null) {
			UUID fallbackUniqueId = request.getConnectionUniqueId();
			String username = request.getUsername();
			if (fallbackUniqueId == null && username != null)
				fallbackUniqueId = UniqueIdGenerator.offlinePlayerUniqueId(username);

			if (fallbackUniqueId == null) {
				Logger.severe("%s request missing Identica UUID and fallback UUID", type());
				return null;
			}

			Logger.warn("%s request missing Identica UUID, applying fallback UUID %s", type(), fallbackUniqueId);
			request.getIdentity().setAccountUniqueId(fallbackUniqueId);
		}

		AuthContext context = AuthContext.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identity(request.getIdentity())
				.intendedServer(request.getIntendedServer())
				.build();
		context.setProvider(request.getProvider());
		context.setTransition(request.getTransition());

		EventUtil.callEvent(new AuthenticationContextBuiltEvent(context));
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

		if (base instanceof AuthContext authContext) {
			return AuthContext.builder()
					.connectionUniqueId(resolveConnectionUniqueId(base, request))
					.identity(identity)
					.intendedServer(intendedServer)
					.provider(authContext.getProvider())
					.transition(authContext.getTransition())
					.build();
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

	@Override
	public boolean matchesNewScenario(@Nullable ConnectionRequest request) {
		if (request == null) return false;
		UUID uniqueId = request.getIdentity().getAccountUniqueId();
		if (uniqueId == null) return false;

		return !providerLinkPersistenceService.findByUniqueId(uniqueId).isEmpty();
	}
}
