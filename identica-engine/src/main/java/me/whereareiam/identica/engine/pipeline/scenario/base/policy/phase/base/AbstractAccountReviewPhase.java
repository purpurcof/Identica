package me.whereareiam.identica.engine.pipeline.scenario.base.policy.phase.base;

import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.engine.pipeline.scenario.base.identity.item.IdentityMetaItem;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.AccountDecision;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.AbstractGroupState;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.PipelinePhase;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
public abstract class AbstractAccountReviewPhase<C extends ScenarioContext, S extends AbstractGroupState> implements PipelinePhase<S> {
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "account-review";
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

		IdentityMetaItem identity = pipelineState.item(IdentityMetaItem.class).orElse(null);
		if (identity == null) {
			state.setResult(PipelineResult.failed(accountReviewMissingMessage(messagesProvider.get())));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		C context = resolveContext(pipelineState);
		ProviderContext provider = context != null ? context.getProvider() : null;
		if (context == null || provider == null || context.getAccountUniqueId() == null) {
			state.setResult(PipelineResult.failed(accountReviewMissingMessage(messagesProvider.get())));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}
		if (provider.getProviderId() == null || provider.getProviderId().isBlank()
				|| provider.getProviderSubject() == null || provider.getProviderSubject().isBlank()) {
			state.setResult(PipelineResult.failed(accountReviewMissingMessage(messagesProvider.get())));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		Account account = accountPersistenceService.findByUniqueId(context.getAccountUniqueId()).orElse(null);
		if (account == null) {
			state.setResult(PipelineResult.failed(accountReviewMissingMessage(messagesProvider.get())));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		Optional<AccountProviderLink> linkOptional = providerLinkPersistenceService
				.findBySubject(provider.getProviderId(), provider.getProviderSubject());
		if (linkOptional.isEmpty()) {
			state.setResult(PipelineResult.failed(accountReviewMissingMessage(messagesProvider.get())));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		AccountProviderLink link = linkOptional.get();
		AccountProviderProfile profile = providerProfilePersistenceService
				.findBySubject(provider.getProviderId(), provider.getProviderSubject())
				.orElseGet(() -> AccountProviderProfile.builder()
						.providerId(provider.getProviderId())
						.providerSubject(provider.getProviderSubject())
						.providerUsername(provider.getProviderUsername())
						.build());

		IdentityMetaItem.Change<String> usernameChange = identity.getUsername();
		IdentityMetaItem.Change<String> sourceChange = identity.getSource();

		if (usernameChange != null && usernameChange.getCurrent() != null)
			account.setUsername(usernameChange.getCurrent());
		if (sourceChange != null && sourceChange.getCurrent() != null)
			account.setSource(UsernameSource.fromId(sourceChange.getCurrent()));

		AccountPrepareEvent preparedEvent = new AccountPrepareEvent(
				account.getUsername(),
				provider,
				account,
				link,
				profile,
				null
		);
		EventUtil.callEvent(preparedEvent);
		AccountDecision decision = preparedEvent.getDecision();
		if (decision == null) decision = AccountDecision.allow();
		String effectiveUsername = preparedEvent.getEffectiveUsername();
		if (effectiveUsername == null || effectiveUsername.isBlank())
			effectiveUsername = account.getUsername();

		if (decision.isDenied()) {
			if (hasChange(usernameChange) && usernameChange.getPrevious() != null)
				account.setUsername(usernameChange.getPrevious());
			if (hasChange(sourceChange) && sourceChange.getPrevious() != null)
				account.setSource(UsernameSource.fromId(sourceChange.getPrevious()));
		}

		identity.setEffectiveUsername(effectiveUsername);
		pipelineState.putItem(identity, 0L);

		if (decision.isDenied()) {
			String message = decision.getMessage();
			state.setResult(PipelineResult.denied(resolveMessage(message, messagesProvider.get())));
		}

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	protected abstract @Nullable C resolveContext(@NotNull PipelineState pipelineState);

	protected abstract @NotNull String failedMessage(@NotNull Messages messages);

	protected abstract @NotNull String accountReviewMissingMessage(@NotNull Messages messages);

	private boolean hasChange(@Nullable IdentityMetaItem.Change<String> change) {
		return change != null && !Objects.equals(change.getPrevious(), change.getCurrent());
	}

	private @NotNull String resolveMessage(@Nullable String message, @NotNull Messages messages) {
		if (message != null && !message.isBlank())
			return message;
		return failedMessage(messages);
	}

	protected final @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}
}
