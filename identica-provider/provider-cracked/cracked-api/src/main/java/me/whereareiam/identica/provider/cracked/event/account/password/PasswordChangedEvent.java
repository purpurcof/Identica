package me.whereareiam.identica.provider.cracked.event.account.password;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a cracked account password has changed.
 */
@Getter
@AllArgsConstructor
public class PasswordChangedEvent implements Event {
	private final @NotNull CrackedAccount account;
	private final @NotNull PasswordChangeReason reason;
	private final long changedAt;
}
