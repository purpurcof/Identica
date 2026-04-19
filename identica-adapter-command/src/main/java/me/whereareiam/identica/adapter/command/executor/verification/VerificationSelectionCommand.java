package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.adapter.command.suggestion.ProviderIdSuggestions;
import me.whereareiam.identica.adapter.command.suggestion.VerificationMethodSuggestions;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationSelectionCommand {
	private final Provider<Messages> messagesProvider;
	private final VerificationService verificationService;
	private final VerificationMessagePresenter messagePresenter;

	@Definition("verification-use")
	@Command("2fa use <provider> <method>")
	public void use(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions(ProviderIdSuggestions.KEY) String providerId,
			@Argument("method") @Suggestions(VerificationMethodSuggestions.KEY) String methodId
	) {
		Identity identity = requireIdentity(sender);
		if (identity == null) return;

		messagePresenter.presentActionResult(sender, verificationService.selectMethod(identity.getUniqueId(), providerId, methodId));
	}

	@Definition("verification-disable")
	@Command("2fa disable <method>")
	public void disable(
			@NotNull Actor sender,
			@Argument("method") @Suggestions(VerificationMethodSuggestions.KEY) String methodId
	) {
		Identity identity = requireIdentity(sender);
		if (identity == null) return;

		messagePresenter.presentActionResult(sender, verificationService.disableMethod(identity.getUniqueId(), methodId));
	}

	private @Nullable Identity requireIdentity(@NotNull Actor sender) {
		if (sender instanceof Identity identity) return identity;
		sendMessage(sender, messagesProvider.get().getCommands().getVerification().getPlayerOnly(), Map.of());
		return null;
	}

	private void sendMessage(@NotNull Actor sender, @Nullable String message, @NotNull Map<String, String> placeholders) {
		if (message == null || message.isBlank()) return;
		sender.sendMessage(Serializer.serialize(SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build()));
	}
}
