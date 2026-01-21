package me.whereareiam.identica.common.auth.handshake;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.registry.Registry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class HandshakePolicyRegistry implements Registry<HandshakePolicy>, Provider<Set<HandshakePolicy>> {
	private final Set<HandshakePolicy> policies = new CopyOnWriteArraySet<>();

	@Override
	public void register(HandshakePolicy value) {
		if (value == null) return;
		policies.add(value);
	}

	@Override
	public void unregister(HandshakePolicy value) {
		if (value == null) return;
		policies.remove(value);
	}

	@Override
	public Set<HandshakePolicy> values() {
		return Collections.unmodifiableSet(policies);
	}

	@Override
	public Set<HandshakePolicy> get() {
		return values();
	}
}
