package me.whereareiam.identica.ratelimit;

import me.whereareiam.identica.model.ratelimit.RateLimitContext;
import me.whereareiam.identica.model.ratelimit.RateLimitDecision;
import me.whereareiam.identica.type.ratelimit.RateLimitScope;

import java.util.Optional;

public interface RateLimitService {
	Optional<RateLimitDecision> evaluate(RateLimitScope scope, RateLimitContext ctx);

	RateLimitDecision record(String id, RateLimitContext ctx);

	void clear(String id, RateLimitContext ctx);
}
