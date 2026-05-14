package me.whereareiam.identica.provider.credential.event.account.password;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a password credential was verified successfully.
 */
@Getter
@AllArgsConstructor
public class PasswordVerifiedEvent implements Event, SynchronousEvent {
	private final @NotNull CredentialAccount credential;
	private final @NotNull String password;
}
