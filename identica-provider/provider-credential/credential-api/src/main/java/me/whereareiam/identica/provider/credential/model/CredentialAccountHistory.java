package me.whereareiam.identica.provider.credential.model;

import lombok.*;
import me.whereareiam.identica.provider.credential.type.PasswordChangeReason;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CredentialAccountHistory {
	private @NotNull String providerId;
	private @NotNull String providerSubject;
	private @NotNull String hashingMethod;
	private @NotNull PasswordChangeReason changeReason;
	private long changedAt;
}
