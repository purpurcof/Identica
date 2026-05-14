package me.whereareiam.identica.provider.credential.model.authentication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.provider.credential.model.CredentialAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@ToString
@AllArgsConstructor
public class AuthenticationAttemptContext {
	private final @NotNull CredentialAccount credential;
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable UUID accountUniqueId;
	private final @Nullable String username;
	private final @Nullable String ip;
}
