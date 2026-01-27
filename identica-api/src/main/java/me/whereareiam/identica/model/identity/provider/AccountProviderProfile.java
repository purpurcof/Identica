package me.whereareiam.identica.model.identity.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Provider-specific profile data for an identity link.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountProviderProfile {
	private @NotNull String providerId;
	private @NotNull String providerSubject;
	private @NotNull String providerUsername;
}
