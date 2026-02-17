package me.whereareiam.identica.provider.cracked.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.cracked.util.PasswordRules;
import me.whereareiam.identica.util.UniqueIdGenerator;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ManagementCommand {
	private final Provider<CrackedMessages> messagesProvider;
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final PasswordRules passwordPolicy;

	@Definition("admin-force-register")
	@Command("identica cracked register <username> <password>")
	public void forceRegister(
			@NotNull Actor sender,
			@Argument("username") String username,
			@Argument("password") String password
	) {
		CrackedMessages.Commands.Admin messages = messagesProvider.get().getCommands().getAdmin();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null) {
			sendMessage(sender, messages.getNotFound());
			return;
		}

		if (accountService.find(providerSubject).isPresent()) {
			sendMessage(sender, messages.getAlreadyRegistered());
			return;
		}

		String error = passwordPolicy.validate(password);
		if (error != null && !error.isBlank()) {
			sendMessage(sender, error);
			return;
		}

		PasswordCandidate candidate = cryptographyService.hash(password);
		if (candidate == null) {
			sendMessage(sender, messages.getNotFound());
			return;
		}

		CrackedAccount account = accountService.register(
				providerSubject,
				candidate.getPasswordHash(),
				candidate.getHashingMethod(),
				PasswordChangeReason.ADMIN_SET
		).orElse(null);
		if (account == null) {
			sendMessage(sender, messages.getNotFound());
			return;
		}

		sendMessage(sender, messages.getRegistered());
	}

	@Definition("admin-set-password")
	@Command("identica cracked setpassword <username> <password>")
	public void setPassword(
			@NotNull Actor sender,
			@Argument("username") String username,
			@Argument("password") String password
	) {
		CrackedMessages.Commands.Admin messages = messagesProvider.get().getCommands().getAdmin();
		String providerSubject = resolveProviderSubject(username);
		if (providerSubject == null) {
			sendMessage(sender, messages.getNotFound());
			return;
		}

		CrackedAccount account = accountService.find(providerSubject).orElse(null);
		if (account == null) {
			sendMessage(sender, messages.getNotFound());
			return;
		}

		String error = passwordPolicy.validate(password);
		if (error != null && !error.isBlank()) {
			sendMessage(sender, error);
			return;
		}

		PasswordCandidate candidate = cryptographyService.hash(password);
		if (candidate != null) {
			accountService.updatePassword(
					account,
					candidate.getPasswordHash(),
					candidate.getHashingMethod(),
					PasswordChangeReason.ADMIN_SET
			);
		}
		sendMessage(sender, messages.getPasswordSet());
	}

	private void sendMessage(@NotNull Actor sender, @NotNull String message) {
		if (message.isBlank()) return;
		SerializerContent content = SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.build();
		sender.sendMessage(Serializer.serialize(content));
	}

	private String resolveProviderSubject(String username) {
		UUID uuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		return uuid != null ? uuid.toString() : null;
	}
}
