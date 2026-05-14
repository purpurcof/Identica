package me.whereareiam.identica.identity;

import lombok.*;
import me.whereareiam.identica.identity.actor.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Runtime attachment record for an online identity.
 *
 * <p>An attachment bridges the live connection UUID used by the platform with
 * the resolved account UUID used by sessions and account-scoped flows.</p>
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class IdentityAttachment {
	private @NotNull UUID connectionUniqueId;
	private @Nullable UUID accountUniqueId;
	private @NotNull String username;
	private @NotNull Identity identity;
}
