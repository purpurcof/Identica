package me.whereareiam.identica.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Session {
	private String sessionId;
	private UUID identicaUniqueId;

	private String providerId;

	private String originalUsername;
	private String effectiveUsername;

	@Builder.Default
	private Map<String, String> conflictMetadata = new HashMap<>();
	private String ip;
	private long createdAt;
}
