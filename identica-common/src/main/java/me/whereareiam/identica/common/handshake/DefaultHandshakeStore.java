package me.whereareiam.identica.common.handshake;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.cache.codec.type.JsonCodec;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.handshake.HandshakeInstructionEvent;
import me.whereareiam.identica.handshake.HandshakePolicy;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.type.event.EventOrder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public final class DefaultHandshakeStore implements HandshakeStore, EventListener {
	private final Set<HandshakePolicy> policies = new CopyOnWriteArraySet<>();
	private final Cache<HandshakeInstruction> cache;
	private final EventManager eventManager;

	@Inject
	public DefaultHandshakeStore(
			CacheService cacheService,
			Provider<Replication> replicationProvider,
			EventManager eventManager
	) {
		this.cache = cacheService.synchronizedCache(resolveNamespace(replicationProvider),
				JsonCodec.of(HandshakeInstruction.class));
		this.eventManager = eventManager;
		eventManager.register(this);
	}

	@Override
	public void registerPolicy(@NotNull HandshakePolicy policy) {
		policies.add(policy);
		Logger.debug("Registered handshake policy %s", policy.getClass().getSimpleName());
	}

	@Override
	public void unregisterPolicy(@NotNull HandshakePolicy policy) {
		policies.remove(policy);
		Logger.debug("Unregistered handshake policy %s", policy.getClass().getSimpleName());
	}

	@Override
	public @NotNull Set<HandshakePolicy> policies() {
		return Collections.unmodifiableSet(policies);
	}

	@Override
	public void putInstruction(@NotNull HandshakeInstruction instruction) {
		HandshakeInstructionEvent event = new HandshakeInstructionEvent(instruction);
		eventManager.call(event);
		if (event.isCancelled()) return;

		HandshakeInstruction stored = event.getInstruction();
		String username = stored.getIdentity().getUsername();
		if (username.isBlank()) return;

		String key = normalize(username);
		long ttlMs = Math.max(1, stored.getExpiresAt() - System.currentTimeMillis());
		cache.put(key, stored, ttlMs).join();
	}

	@Override
	public @NotNull Optional<HandshakeInstruction> consumeInstruction(@NotNull String username) {
		if (username.isBlank()) return Optional.empty();
		return read(username);
	}

	@Override
	public void invalidateInstruction(@NotNull String username) {
		if (username.isBlank()) return;
		String key = normalize(username);
		cache.invalidate(key).join();
	}

	@IdenticEvent(EventOrder.LOWEST)
	public void onAccountClear(@NotNull AccountClearEvent event) {
		String username = event.getIdentity().getUsername();
		if (username.isBlank()) return;
		invalidateInstruction(username);
	}

	private Optional<HandshakeInstruction> read(String username) {
		String key = normalize(username);
		HandshakeInstruction instruction = (cache.consume(key))
				.join()
				.orElse(null);

		if (instruction == null) return Optional.empty();
		if (instruction.isExpired(System.currentTimeMillis()))
			return Optional.empty();

		return Optional.of(instruction);
	}

	private String normalize(String username) {
		return username.trim().toLowerCase();
	}

	private static String resolveNamespace(Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null)
			throw new IllegalStateException("replication is missing");

		String namespace = replication.getCache().getInstructions();
		if (namespace.isBlank())
			throw new IllegalStateException("replication.cache.instructions is missing");

		return namespace;
	}
}
