package me.whereareiam.identica.adapter.command.executor.admin;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.adapter.command.suggestion.CrossPlayerSuggestions;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.type.verification.status.VerificationResetStatus;
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
public class VerificationResetCommand {
	private final Provider<Messages> messagesProvider;
	private final VerificationService verificationService;
	private final IdentityService identityService;
	private final AccountPersistenceService accountPersistenceService;

	@Definition("admin-verification-reset")
	@Command("identica admin 2fa reset <target> [provider]")
	public void reset(
			@NotNull Actor sender,
			@Argument("target") @Suggestions(CrossPlayerSuggestions.KEY) String target,
			@Argument("provider") @Nullable String providerId
	) {
		ResolvedTarget resolved = resolveTarget(sender, target);
		if (resolved == null) return;

		if (verificationService.reset(resolved.uniqueId(), isBlank(providerId) ? null : providerId).getStatus() != VerificationResetStatus.RESET)
			return;

		sendMessage(sender, resetMessages().getCompleted(), Map.of("target", resolved.display()));
	}

	private @Nullable ResolvedTarget resolveTarget(@NotNull Actor sender, @NotNull String target) {
		UUID parsed = parseUniqueId(target);
		if (parsed != null) return new ResolvedTarget(parsed, target);

		Optional<Identity> identity = identityService.find(target);
		if (identity.isPresent()) return new ResolvedTarget(identity.get().getUniqueId(), identity.get().getUsername());

		List<Account> matches = accountPersistenceService.findByUsername(target);
		if (matches.isEmpty()) {
			sendMessage(sender, resetMessages().getTargetNotFound(), Map.of("target", target));
			return null;
		}

		Account account = matches.getFirst();
		return new ResolvedTarget(account.getUniqueId(), account.getUsername());
	}

	private Messages.Commands.Admin.Verification.Reset resetMessages() {
		return messagesProvider.get().getCommands().getAdmin().getVerification().getReset();
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

	private boolean isBlank(@Nullable String value) {
		return value == null || value.isBlank();
	}

	private record ResolvedTarget(@NotNull UUID uniqueId, @NotNull String display) {
	}
}
