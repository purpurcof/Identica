package me.whereareiam.identica.engine.pipeline.scenario.base.session.phase.base;

import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.base.AbstractGroupState;
import me.whereareiam.identica.engine.pipeline.scenario.base.identity.item.IdentityMetaItem;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
public abstract class AbstractBuildSessionPhase<C extends ScenarioContext, S extends AbstractGroupState> implements PipelinePhase<S> {
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
	public @NotNull CompletionStage<PhaseResult<S>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull S state
	) {
		PipelineResult result = state.getResult();
		if (result == null || result.getStatus() != PipelineStatus.COMPLETE)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		C context = resolveContext(state, pipelineState);
		IdentityMetaItem identity = pipelineState.item(IdentityMetaItem.class).orElse(null);
		if (context == null || identity == null) {
			state.setResult(PipelineResult.failed(sessionBuildMissingMessage(messagesProvider.get())));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		ProviderContext provider = context.getProvider();
		String currentUsername = resolveCurrentUsername(identity);
		if (provider == null || isBlank(currentUsername) || context.getAccountUniqueId() == null) {
			state.setResult(PipelineResult.failed(sessionBuildMissingMessage(messagesProvider.get())));
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
				.uniqueId(context.getAccountUniqueId())
				.providerId(provider.getProviderId())
				.providerSubject(provider.getProviderSubject())
				.originalUsername(originalUsername)
				.effectiveUsername(effectiveUsername)
				.ip(context.getIp())
				.createdAt(System.currentTimeMillis())
				.build();

		storeContext(state, context);
		storeSession(state, session);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	protected abstract @Nullable C resolveContext(@NotNull S state, @NotNull PipelineState pipelineState);

	protected abstract void storeContext(@NotNull S state, @NotNull C context);

	protected abstract void storeSession(@NotNull S state, @NotNull Session session);

	protected abstract @NotNull String sessionBuildMissingMessage(@NotNull Messages messages);

	private @Nullable String resolveCurrentUsername(@NotNull IdentityMetaItem identity) {
		IdentityMetaItem.Change<String> username = identity.getUsername();
		return username != null ? username.getCurrent() : null;
	}

	private boolean isBlank(@Nullable String value) {
		return value == null || value.isBlank();
	}
}
