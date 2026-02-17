package me.whereareiam.identica.common.ratelimit;

import com.google.inject.Singleton;
import me.whereareiam.identica.ratelimit.RateLimitDefinition;
import me.whereareiam.identica.registry.Registry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class RateLimitRegistry implements Registry<RateLimitDefinition> {
	private final Set<RateLimitDefinition> definitions = new CopyOnWriteArraySet<>();

	@Override
	public void register(RateLimitDefinition value) {
		if (value == null) return;
		definitions.add(value);
	}

	@Override
	public void unregister(RateLimitDefinition value) {
		if (value == null) return;
		definitions.remove(value);
	}

	@Override
	public Set<RateLimitDefinition> values() {
		return Collections.unmodifiableSet(definitions);
	}
}
