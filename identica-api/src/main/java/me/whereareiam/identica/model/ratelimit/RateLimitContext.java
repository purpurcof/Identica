package me.whereareiam.identica.model.ratelimit;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@Builder
public class RateLimitContext {
	private final UUID uniqueId;
	private final UUID connectionUniqueId;
	private final String username;
	private final String ip;
}
