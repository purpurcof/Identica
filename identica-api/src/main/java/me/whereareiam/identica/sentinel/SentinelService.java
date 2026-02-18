package me.whereareiam.identica.sentinel;

import me.whereareiam.identica.model.sentinel.SentinelContext;
import me.whereareiam.identica.model.sentinel.SentinelDecision;
import me.whereareiam.identica.type.sentinel.SentinelScope;

import java.util.Optional;

public interface SentinelService {
	Optional<SentinelDecision> evaluate(SentinelScope scope, SentinelContext ctx);

	SentinelDecision record(String id, SentinelContext ctx);

	void clear(String id, SentinelContext ctx);
}
