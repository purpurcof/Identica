package me.whereareiam.identica.provider.credential.event.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a password credential is registered.
 */
@Getter
@AllArgsConstructor
public class AccountRegisteredEvent implements Event {
	private final @NotNull CredentialAccount credential;
	private final @NotNull PasswordChangeReason reason;
}
