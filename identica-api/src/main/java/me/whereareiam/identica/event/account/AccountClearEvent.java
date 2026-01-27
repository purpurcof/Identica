package me.whereareiam.identica.event.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.actor.OfflineIdentity;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.type.ClearScope;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an account clear is requested.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class AccountClearEvent implements Event, SynchronousEvent {
	private final @NotNull OfflineIdentity identity;
	private final @NotNull ClearScope scope;
	private final boolean synchronizedEvent;

	/**
	 * Creates a non-synchronized clear event.
	 *
	 * @param identity target identity
	 * @param scope clear scope
	 */
	public AccountClearEvent(@NotNull OfflineIdentity identity, @NotNull ClearScope scope) {
		this(identity, scope, false);
	}
}
