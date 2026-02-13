package me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityMetaItem;
import me.whereareiam.identica.engine.pipeline.scenario.migration.group.identity.IdentityState;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.UsernameSource;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UsernameReplicationPhase implements PipelinePhase<IdentityState> {
	private final ProviderManager providerManager;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "username-replication";
	}

	@Override
	public int order() {
		return 500;
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

		Account account = state.getAccount();
		AccountProviderLink link = state.getLink();
		AccountProviderProfile profile = state.getProfile();
		if (account == null || link == null || profile == null) {
			state.setResult(PipelineResult.failed(identityReplicationMissingMessage()));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		String previousUsername = account.getUsername();
		UsernameSource previousSource = account.getSource();

		applyReplication(account, link, profile);

		IdentityMetaItem.Change<String> username = IdentityMetaItem.Change.<String>builder()
				.previous(previousUsername)
				.current(account.getUsername())
				.build();
		IdentityMetaItem.Change<String> source = IdentityMetaItem.Change.<String>builder()
				.previous(previousSource.getId())
				.current(account.getSource().getId())
				.build();

		IdentityMetaItem existing = pipelineState.item(IdentityMetaItem.class).orElse(null);
		IdentityMetaItem identity = IdentityMetaItem.builder()
				.effectiveUsername(existing != null ? existing.getEffectiveUsername() : null)
				.resumed(existing != null && existing.isResumed())
				.username(username)
				.source(source)
				.build();
		pipelineState.putItem(identity, 0L);

		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull String identityReplicationMissingMessage() {
		return joinMessage(messagesProvider.get()
				.getConnection()
				.getMigration()
				.getErrors()
				.getIdentityReplicationMissing());
	}

	private @NotNull String joinMessage(@NotNull List<String> lines) {
		return String.join("\n", lines);
	}

	private boolean applyReplication(
			@NotNull Account account,
			@NotNull AccountProviderLink link,
			@NotNull AccountProviderProfile profile
	) {
		String providerUsername = profile.getProviderUsername();
		if (providerUsername.isBlank()) return false;
		if (account.getSource() == UsernameSource.MANUAL) return false;
		if (!link.isPrimary()) return false;
		if (!isProviderAuthoritative(link.getProviderId())) return false;

		String candidate = providerUsername.trim();
		if (candidate.equals(account.getUsername()))
			return false;

		account.setUsername(candidate);
		account.setSource(UsernameSource.PROVIDER);
		return true;
	}

	private boolean isProviderAuthoritative(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return false;
		return providerManager.getProviders().stream()
				.map(InternalProvider::getDescriptor)
				.filter(Objects::nonNull)
				.anyMatch(descriptor -> descriptor.getId().equalsIgnoreCase(providerId)
						&& descriptor.hasCapability(ProviderCapability.AUTHORITATIVE_USERNAME));
	}
}
