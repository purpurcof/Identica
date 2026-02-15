package me.whereareiam.identica.provider.cracked.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a cracked account is registered.
 */
@Getter
@AllArgsConstructor
public class AccountRegisteredEvent implements Event {
	private final @NotNull CrackedAccount account;
	private final @NotNull PasswordChangeReason reason;
}
