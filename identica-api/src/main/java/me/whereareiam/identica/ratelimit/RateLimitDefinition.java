package me.whereareiam.identica.ratelimit;

import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitKey;
import me.whereareiam.identica.model.ratelimit.RateLimitPolicy;
import me.whereareiam.identica.type.ratelimit.RateLimitMode;
import me.whereareiam.identica.type.ratelimit.RateLimitScope;

public interface RateLimitDefinition {
	String id();

	RateLimitScope[] scopes();

	RateLimitMode modeFor(RateLimitScope scope);

	RateLimitPolicy policy(RateLimitContext ctx);

	default RateLimitKey key(RateLimitContext ctx) {
		return RateLimitKey.ipAndIdentity(ctx);
	}

	default int priority() {
		return 0;
	}
}
