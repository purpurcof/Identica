package me.whereareiam.identica.engine.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountLifecycleEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.type.ScenarioResolution;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationResolvedEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationResolvedEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationResolvedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
public class PendingScenarioAccountLifecycle implements EventListener {
	private final PipelineStateStore pipelineStateStore;
	private final IdentityService identityService;
	private final EventManager eventManager;

	@Inject
	public PendingScenarioAccountLifecycle(
			@NotNull PipelineStateStore pipelineStateStore,
			@NotNull IdentityService identityService,
			@NotNull EventManager eventManager
	) {
		this.pipelineStateStore = pipelineStateStore;
		this.identityService = identityService;
		this.eventManager = eventManager;
		eventManager.register(this);
	}

	@IdenticEvent
	public void onAccountLifecycle(@NotNull AccountLifecycleEvent event) {
		UUID connectionUniqueId = resolveConnectionUniqueId(event);
		if (connectionUniqueId == null) return;

		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();
		PipelineState stored = pipelineStateStore.consume(reference).orElse(null);
		if (stored == null) return;

		PipelineType pipelineType = stored.getPipelineType();
		ScenarioContext context = pipelineType != null ? stored.getScenario(pipelineType) : stored.getScenario();
		if (pipelineType == null || context == null) return;

		emitResolved(context);
	}

	private void emitResolved(@NotNull ScenarioContext context) {
		switch (context) {
			case AuthContext authContext ->
					eventManager.call(new AuthenticationResolvedEvent(authContext, ScenarioResolution.CANCELLED, false));
			case RegistrationContext registrationContext ->
					eventManager.call(new RegistrationResolvedEvent(registrationContext, ScenarioResolution.CANCELLED, false));
			case MigrationContext migrationContext ->
					eventManager.call(new MigrationResolvedEvent(migrationContext, ScenarioResolution.CANCELLED, false));
			default -> {
			}
		}
	}

	private @Nullable UUID resolveConnectionUniqueId(@NotNull AccountLifecycleEvent event) {
		UUID connectionUniqueId = event.getIdentity().getAccountUniqueId();
		if (connectionUniqueId != null) return connectionUniqueId;

		String username = event.getIdentity().getUsername();
		if (username.isBlank()) return null;

		return identityService.find(username)
				.map(Identity::getUniqueId)
				.orElse(null);
	}
}
