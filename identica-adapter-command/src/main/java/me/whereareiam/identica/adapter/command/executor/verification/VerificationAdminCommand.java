package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Default;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.adapter.command.suggestion.CrossPlayerSuggestions;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationAdminCommand {
	private final Provider<Messages> messagesProvider;
	private final VerificationService verificationService;
	private final IdentityService identityService;
	private final AccountPersistenceService accountPersistenceService;

	@Definition("verification-reset")
	@Command("identica 2fa reset <target> [provider]")
	public void reset(
			@NotNull Actor sender,
			@Argument("target") @Suggestions(CrossPlayerSuggestions.KEY) String target,
			@Argument("provider") @Default("") String providerId
	) {
		ResolvedTarget resolved = resolveTarget(sender, target);
		if (resolved == null) return;

		verificationService.reset(resolved.uniqueId(), providerId.isBlank() ? null : providerId);
		sendMessage(sender, verificationMessages().getReset().getCompleted(), Map.of("target", resolved.display()));
	}

	private @Nullable ResolvedTarget resolveTarget(@NotNull Actor sender, @NotNull String target) {
		UUID parsed = parseUniqueId(target);
		if (parsed != null) return new ResolvedTarget(parsed, target);

		Optional<Identity> identity = identityService.find(target);
		if (identity.isPresent()) return new ResolvedTarget(identity.get().getUniqueId(), identity.get().getUsername());

		List<Account> matches = accountPersistenceService.findByUsername(target);
		if (matches.isEmpty()) {
			sendMessage(sender, verificationMessages().getReset().getTargetNotFound(), Map.of("target", target));
			return null;
		}

		Account account = matches.getFirst();
		return new ResolvedTarget(account.getUniqueId(), account.getUsername());
	}

	private Messages.Commands.Verification verificationMessages() {
		return messagesProvider.get().getCommands().getVerification();
	}

	private void sendMessage(@NotNull Actor sender, @Nullable String message, @NotNull Map<String, String> placeholders) {
		if (message == null || message.isBlank()) return;
		sender.sendMessage(Serializer.serialize(SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build()));
	}

	private UUID parseUniqueId(String target) {
		try {
			return UUID.fromString(target);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private record ResolvedTarget(@NotNull UUID uniqueId, @NotNull String display) {
	}
}
