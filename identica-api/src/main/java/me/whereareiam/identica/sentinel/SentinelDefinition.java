package me.whereareiam.identica.sentinel;

import me.whereareiam.identica.model.sentinel.SentinelContext;
import me.whereareiam.identica.model.sentinel.SentinelKey;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import me.whereareiam.identica.type.sentinel.SentinelMode;
import me.whereareiam.identica.type.sentinel.SentinelScope;

public interface SentinelDefinition {
	String id();

	SentinelScope[] scopes();

	SentinelMode modeFor(SentinelScope scope);

	SentinelPolicy policy(SentinelContext ctx);

	default SentinelKey key(SentinelContext ctx) {
		return SentinelKey.ipAndIdentity(ctx);
	}

	default int priority() {
		return 0;
	}
}
