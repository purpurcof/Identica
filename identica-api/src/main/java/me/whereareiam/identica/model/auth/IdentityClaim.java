package me.whereareiam.identica.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Identity claim returned by a provider after successful authentication.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class IdentityClaim {
	private String providerId;
	private String providerSubject;
	@Builder.Default
	private Map<String, String> conflictData = new HashMap<>();

	public Optional<String> getUsername() {
		return Optional.ofNullable(conflictData.get("username"));
	}
}
