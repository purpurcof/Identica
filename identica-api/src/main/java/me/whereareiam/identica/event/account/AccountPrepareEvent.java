package me.whereareiam.identica.event.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.account.AccountDecision;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when account preparation has loaded or created core data
 * and before a final account decision is set.
 *
 * <pre>{@code
 * @IdenticEvent
 * public void onPrepare(AccountPrepareEvent event) {
 *     if (event.getDecision() == null) {
 *         event.setDecision(AccountDecision.allow());
 *     }
 * }
 * }</pre>
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class AccountPrepareEvent implements Event, SynchronousEvent {
	/**
	 * Candidate internal username to apply; later listeners may overwrite this value.
	 * Defaults to the account username at the time of preparation.
	 */
	private @Nullable String effectiveUsername;
	private @Nullable AccountDecision decision;

	private final @NotNull Account account;
	private final @NotNull AccountProviderLink link;
	private final @NotNull AccountProviderProfile profile;

	private final boolean created;
}
