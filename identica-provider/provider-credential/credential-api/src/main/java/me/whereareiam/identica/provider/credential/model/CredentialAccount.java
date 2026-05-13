package me.whereareiam.identica.provider.credential.model;

import lombok.*;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CredentialAccount {
	private @NotNull String providerId;
	private @NotNull String providerSubject;
	private @NotNull String passwordHash;
	private @NotNull String hashingMethod;
	private long createdAt;
	private long updatedAt;
}
