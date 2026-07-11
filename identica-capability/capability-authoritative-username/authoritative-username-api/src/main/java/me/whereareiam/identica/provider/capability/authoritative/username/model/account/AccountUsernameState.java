package me.whereareiam.identica.provider.capability.authoritative.username.model.account;

import lombok.*;
import me.whereareiam.identica.provider.capability.authoritative.username.type.AccountUsernameSource;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Current persisted username control state for a single Identica account.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountUsernameState {
	private @NotNull UUID uniqueId;
	private @NotNull AccountUsernameSource source;
	private long updatedAt;
}
