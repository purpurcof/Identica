package me.whereareiam.identica.provider.capability.authoritative.username.model.conflict;

import lombok.Builder;
import lombok.Getter;
import me.whereareiam.identica.conflict.ConflictSubject;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Builder(toBuilder = true)
public class UsernameConflictSubject implements ConflictSubject {
	@Builder.Default
	private final @NotNull String hookId = "prepare";
	private final @Nullable String candidateUsername;
	private final @Nullable Account account;
	private final @Nullable AccountProviderLink incomingLink;
	private final @Nullable AccountProviderProfile incomingProfile;
	private final @Nullable ProviderContext providerContext;

	@Override
	public @NotNull String hook() {
		return hookId;
	}
}
