package me.whereareiam.identica.provider.cracked.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.provider.cracked.CrackedConstants;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.cryptography.PasswordCandidate;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.util.PasswordRules;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import me.whereareiam.identica.util.UniqueIdGenerator;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ChangePasswordCommand {
	private final Provider<CrackedMessages> messagesProvider;
	private final SessionService sessionService;
	private final CrackedAccountService accountService;
	private final CryptographyService cryptographyService;
	private final PasswordRules passwordPolicy;

	@Definition("change-password")
	@Command("changepassword <current> <new> <repeat>")
	public void changePassword(
			@NotNull Actor sender,
			@Argument("current") String current,
			@Argument("new") String next,
			@Argument("repeat") String repeat
	) {
		if (!(sender instanceof Identity identity))
			return;

		CrackedMessages.ChangePassword messages = messagesProvider.get().getChangePassword();
		Session session = sessionService.findByUniqueId(identity.getUniqueId()).join().orElse(null);
		if (session == null) {
			sendMessage(identity, messages.getNotLoggedIn());
			return;
		}

		if (next == null || !next.equals(repeat)) {
			sendMessage(identity, messages.getMismatch());
			return;
		}

		String error = passwordPolicy.validate(next);
		if (error != null && !error.isBlank()) {
			sendMessage(identity, error);
			return;
		}

		String providerSubject = null;
		if (session.getProviderId() != null
				&& session.getProviderId().equalsIgnoreCase(CrackedConstants.PROVIDER_ID)) {
			providerSubject = session.getProviderSubject();
		}
		if (providerSubject == null || providerSubject.isBlank())
			providerSubject = resolveProviderSubject(identity.getUsername());
		CrackedAccount account = providerSubject != null
				? accountService.find(providerSubject).orElse(null)
				: null;
		if (account == null) {
			sendMessage(identity, messagesProvider.get().getLogin().getNotRegistered());
			return;
		}

		if (!cryptographyService.verify(account, current)) {
			sendMessage(identity, messages.getInvalidCurrent());
			return;
		}

		PasswordCandidate candidate = cryptographyService.hash(next);
		if (candidate != null) {
			accountService.updatePassword(
					account,
					candidate.getPasswordHash(),
					candidate.getHashingMethod(),
					PasswordChangeReason.CHANGE
			);
		}
		sendMessage(identity, messages.getSuccess());
	}

	private void sendMessage(@NotNull Identity identity, String message) {
		if (message == null || message.isBlank())
			return;
		SerializerContent content = SerializerContent.builder()
				.receiver(identity)
				.message(message)
				.build();
		identity.sendMessage(Serializer.serialize(content));
	}

	private String resolveProviderSubject(String username) {
		UUID uuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		return uuid != null ? uuid.toString() : null;
	}
}
