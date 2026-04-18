package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item.IdentityMetaItem;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.SessionState;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BuildSessionPhase implements PipelinePhase<SessionState> {
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "build-session";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull Class<SessionState> stateType() {
		return SessionState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<SessionState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull SessionState state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		PipelineState source = result.getState() != null ? result.getState() : pipelineState;
		AuthContext authContext = resolveAuthContext(source);
		IdentityMetaItem identity = pipelineState.item(IdentityMetaItem.class).orElse(null);
		ProviderContext provider = resolveProvider(authContext);
		if (authContext == null || identity == null) {
			state.setResult(PipelineResult.failed(sessionBuildFailedMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}
		if (provider == null || isBlank(provider.getProviderId()) || isBlank(provider.getProviderSubject())) {
			state.setResult(PipelineResult.failed(sessionBuildFailedMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		if (authContext.getIdenticaUniqueId() == null) {
			state.setResult(PipelineResult.failed(sessionBuildFailedMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String effectiveUsername = identity.getEffectiveUsername();
		if (isBlank(effectiveUsername))
			effectiveUsername = resolveCurrentUsername(identity);
		if (isBlank(effectiveUsername))
			effectiveUsername = authContext.getUsername();

		Session session = Session.builder()
				.uniqueId(authContext.getIdenticaUniqueId())
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.originalUsername(authContext.getUsername())
				.effectiveUsername(effectiveUsername)
				.ip(authContext.getIp())
				.createdAt(System.currentTimeMillis())
				.build();

		state.setAuthContext(authContext);
		state.setSession(session);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @Nullable ProviderContext resolveProvider(@Nullable AuthContext authContext) {
		return authContext != null ? authContext.getProvider() : null;
	}

	private @NotNull String sessionBuildFailedMessage() {
		return String.join("\n", messagesProvider.get().getConnection().getAuthentication().getSessionBuildFailed());
	}

	private @Nullable String resolveCurrentUsername(@NotNull IdentityMetaItem identity) {
		IdentityMetaItem.Change<String> username = identity.getUsername();
		return username != null ? username.getCurrent() : null;
	}

	private @Nullable AuthContext resolveAuthContext(@NotNull PipelineState source) {
		PipelineType pipelineType = source.getPipelineType();
		if (pipelineType == null) return null;
		return source.getScenario(pipelineType) instanceof AuthContext authContext ? authContext : null;
	}

	private boolean isBlank(@Nullable String value) {
		return value == null || value.isBlank();
	}
}
