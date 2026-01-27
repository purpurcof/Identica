package me.whereareiam.identica.model.identity.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Link between an internal Identica identity and a provider subject.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountProviderLink {
	/**
	 * Identica identity id.
	 */
	private @NotNull UUID uniqueId;

	private @NotNull String providerId;
	private @NotNull String providerSubject;

	private boolean primary;
	private long linkedAt;
	private long lastSeenAt;
}
