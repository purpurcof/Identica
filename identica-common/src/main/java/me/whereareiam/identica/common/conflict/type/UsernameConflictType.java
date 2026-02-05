package me.whereareiam.identica.common.conflict.type;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.conflict.resolver.username.FormatUsernameConflictResolver;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.conflict.ConflictType;
import me.whereareiam.identica.conflict.resolver.ConflictResolver;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.ProviderProfilePersistenceService;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.account.AccountDecision;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.session.SessionService;
import me.whereareiam.identica.type.ConflictHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class UsernameConflictType implements ConflictType {
	private static final String KEY = "username";

	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;
	private final SessionService sessionService;
	private final FormatUsernameConflictResolver formatResolver;

	@Inject
	public UsernameConflictType(
			AccountPersistenceService accountPersistenceService,
			ProviderLinkPersistenceService providerLinkPersistenceService,
			ProviderProfilePersistenceService providerProfilePersistenceService,
			SessionService sessionService,
			FormatUsernameConflictResolver formatResolver,
			ConflictService conflictService
	) {
		this.accountPersistenceService = accountPersistenceService;
		this.providerLinkPersistenceService = providerLinkPersistenceService;
		this.providerProfilePersistenceService = providerProfilePersistenceService;
		this.sessionService = sessionService;
		this.formatResolver = formatResolver;

		conflictService.register(this);
	}

	@Override
	public @NotNull String getKey() {
		return KEY;
	}

	@Override
	public @NotNull ConflictHook getDefaultHook() {
		return ConflictHook.PREPARE;
	}

	@Override
	public @NotNull List<ConflictResolver> getResolvers() {
		return List.of(formatResolver);
	}

	@Override
	public @Nullable ConflictContext createContext(@NotNull AccountPrepareEvent event) {
		String candidate = event.getEffectiveUsername();
		if (candidate == null || candidate.isBlank())
			return null;

		Account account = event.getAccount();
		AccountProviderLink incomingLink = event.getLink();

		candidate = candidate.trim();
		String currentUsername = account.getUsername();
		if (candidate.equals(currentUsername))
			return null;

		Account conflictAccount = resolveActiveConflict(account.getUniqueId(), candidate)
				.orElse(null);
		if (conflictAccount == null)
			return null;

		AccountProviderLink existingLink = resolvePrimaryLink(conflictAccount.getUniqueId())
				.orElse(null);
		AccountProviderProfile existingProfile = existingLink != null
				? providerProfilePersistenceService.findBySubject(
						existingLink.getProviderId(),
						existingLink.getProviderSubject()
				).orElse(null)
				: null;

		if (existingLink == null)
			return null;

		return ConflictContext.builder()
				.key(KEY)
				.candidate(candidate)
				.incomingAccount(account)
				.incomingLink(incomingLink)
				.existingAccount(conflictAccount)
				.existingLink(existingLink)
				.existingProfile(existingProfile)
				.build();
	}

	@Override
	public void apply(
			@NotNull AccountPrepareEvent event,
			@NotNull ConflictContext context,
			@NotNull ConflictResolution resolution
	) {
		if (resolution.getAction() == ConflictResolution.Action.DENY) {
			event.setDecision(AccountDecision.deny(resolution.getMessage()));
			return;
		}

		Account existing = context.getExistingAccount();
		if (existing != null && resolution.getAction() == ConflictResolution.Action.KICK_EXISTING)
			sessionService.close(existing.getUniqueId()).join();

		if (existing != null && resolution.getAction() == ConflictResolution.Action.KICK_BOTH) {
			sessionService.close(existing.getUniqueId()).join();
			event.setDecision(AccountDecision.deny(resolution.getMessage()));
			return;
		}

		String overrideValue = resolution.getOverrideValue();
		if (overrideValue != null && !overrideValue.isBlank())
			applyOverride(event, context, resolution);

		event.setDecision(AccountDecision.allow());
	}

	private void applyOverride(
			@NotNull AccountPrepareEvent event,
			@NotNull ConflictContext context,
			@NotNull ConflictResolution resolution
	) {
		String overrideValue = resolution.getOverrideValue();
		if (overrideValue == null || overrideValue.isBlank()) return;

		ConflictResolution.OverrideTarget target = resolution.getOverrideTarget();
		if (target == ConflictResolution.OverrideTarget.INCOMING
				|| target == ConflictResolution.OverrideTarget.BOTH)
			event.setEffectiveUsername(overrideValue);

		if (target == ConflictResolution.OverrideTarget.EXISTING
				|| target == ConflictResolution.OverrideTarget.BOTH)
			applyExistingOverride(context, overrideValue);
	}

	private void applyExistingOverride(@NotNull ConflictContext context, @NotNull String overrideValue) {
		Account existing = context.getExistingAccount();
		if (existing == null) return;

		UUID uniqueId = existing.getUniqueId();
		sessionService.findByUniqueId(uniqueId)
				.thenCompose(found -> {
					if (found.isEmpty())
						return CompletableFuture.completedFuture(null);

					Session session = found.get();
					session.setEffectiveUsername(overrideValue);

					return sessionService.open(session).thenApply(_ -> null);
				}).join();
	}

	private Optional<Account> resolveActiveConflict(@NotNull UUID uniqueId, @NotNull String username) {
		List<Account> accounts = accountPersistenceService.findByUsername(username);
		if (accounts.isEmpty()) return Optional.empty();

		for (Account account : accounts) {
			if (account == null) continue;
			if (account.getUniqueId().equals(uniqueId)) continue;
			if (isActive(account.getUniqueId())) return Optional.of(account);
		}

		return Optional.empty();
	}

	private Optional<AccountProviderLink> resolvePrimaryLink(@NotNull UUID uniqueId) {
		List<AccountProviderLink> links = providerLinkPersistenceService.findByUniqueId(uniqueId);
		if (links.isEmpty()) return Optional.empty();

		for (AccountProviderLink link : links) {
			if (link != null && link.isPrimary()) return Optional.of(link);
		}

		return Optional.of(links.getFirst());
	}

	private boolean isActive(@NotNull UUID uniqueId) {
		return sessionService.findByUniqueId(uniqueId)
				.thenApply(Optional::isPresent)
				.join();
	}
}
