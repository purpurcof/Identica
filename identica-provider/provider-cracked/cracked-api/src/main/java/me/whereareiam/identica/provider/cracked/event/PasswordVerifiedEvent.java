package me.whereareiam.identica.provider.cracked.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a cracked account password was verified successfully.
 */
@Getter
@AllArgsConstructor
public class PasswordVerifiedEvent implements Event, SynchronousEvent {
	private final @NotNull CrackedAccount account;
	private final @NotNull String password;
}
