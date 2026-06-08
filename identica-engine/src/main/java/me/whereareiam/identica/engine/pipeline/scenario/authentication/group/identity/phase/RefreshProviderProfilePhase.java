package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.IdentityState;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionStore;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.session.SessionRecognitionSnapshot;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RefreshProviderProfilePhase implements PipelinePhase<IdentityState> {
	private final ProviderProfilePersistenceService providerProfilePersistenceService;
	private final SessionRecognitionStore sessionRecognitionStore;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "refresh-provider-profile";
	}

	@Override
	public int order() {
		return 250;
	}

	@Override
	public @NotNull Class<IdentityState> stateType() {
		return IdentityState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<IdentityState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull IdentityState state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ProviderContext provider = state.getProvider();
		if (provider == null
				|| isBlank(provider.getProviderId())
				|| isBlank(provider.getProviderSubject())
				|| isBlank(provider.getProviderUsername())) {
			state.setResult(PipelineResult.failed(identityProfileMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		AccountProviderProfile profile = state.getProfile();
		AuthContext authContext = pipelineState.getScenario(pipelineState.getPipelineType()) instanceof AuthContext context
				? context
				: null;
		ConnectionIdentity.Origin origin = authContext != null ? authContext.getIdentity().getOrigin() : null;
		if (profile == null) {
			profile = AccountProviderProfile.builder()
					.providerId(provider.getProviderId())
					.providerSubject(provider.getProviderSubject())
					.providerUsername(provider.getProviderUsername())
					.build();
		}

		String candidate = provider.getProviderUsername().trim();
		AccountProviderProfile updated = profile.toBuilder()
				.providerUsername(candidate)
				.build();
		AccountProviderProfile stored = providerProfilePersistenceService.upsert(updated);
		state.setProfile(stored);
		sessionRecognitionStore.save(SessionRecognitionSnapshot.builder()
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.providerUsername(candidate)
				.lastIp(authContext != null ? authContext.getIp() : null)
				.lastVirtualHost(origin != null ? origin.getHost() : null)
				.lastVirtualPort(origin != null ? origin.getPort() : null)
				.capturedAt(System.currentTimeMillis())
				.build());

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String identityProfileMissingMessage() {
		return String.join("\n", messagesProvider.get()
				.getScenarios()
				.getAuthentication()
				.getErrors()
				.getIdentity().getProfileMissing());
	}

	private boolean isBlank(@Nullable String value) {
		return value == null || value.isBlank();
	}
}
