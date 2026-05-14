package me.whereareiam.identica.engine.pipeline.scenario.migration.group.session.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.session.SessionState;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
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

		MigrationContext context = resolveContext(pipelineState);
		IdentityMetaItem identity = pipelineState.item(IdentityMetaItem.class).orElse(null);
		if (context == null || identity == null) {
			state.setResult(PipelineResult.failed(sessionBuildMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		ProviderContext provider = context.getProvider();
		UUID accountUniqueId = context.getAccountUniqueId();

		String currentUsername = resolveCurrentUsername(identity);
		if (provider == null || isBlank(currentUsername) || accountUniqueId == null) {
			state.setResult(PipelineResult.failed(sessionBuildMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String providerUsername = provider.getProviderUsername();
		String originalUsername = isBlank(providerUsername)
				? currentUsername
				: providerUsername;

		String effectiveUsername = identity.getEffectiveUsername();
		if (effectiveUsername == null || effectiveUsername.isBlank())
			effectiveUsername = currentUsername;

		Session session = Session.builder()
				.uniqueId(accountUniqueId)
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.originalUsername(originalUsername)
				.effectiveUsername(effectiveUsername)
				.ip(context.getIp())
				.createdAt(System.currentTimeMillis())
				.build();

		state.setContext(context);
		state.setSession(session);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private String resolveCurrentUsername(@NotNull IdentityMetaItem identity) {
		IdentityMetaItem.Change<String> username = identity.getUsername();
		return username != null ? username.getCurrent() : null;
	}

	private MigrationContext resolveContext(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		if (pipelineType == null)
			return null;
		return pipelineState.getScenario(pipelineType) instanceof MigrationContext migrationContext
				? migrationContext
				: null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private @NotNull String sessionBuildMissingMessage() {
		return joinMessage(messagesProvider.get()
				.getConnection()
				.getMigration()
				.getErrors()
				.getSession().getBuildMissing());
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
