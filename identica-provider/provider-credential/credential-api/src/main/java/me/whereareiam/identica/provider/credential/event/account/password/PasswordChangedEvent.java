package me.whereareiam.identica.provider.credential.event.account.password;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a password credential has changed.
 */
@Getter
@AllArgsConstructor
public class PasswordChangedEvent implements Event {
	private final @NotNull CredentialAccount credential;
	private final @NotNull PasswordChangeReason reason;
	private final long changedAt;
}
