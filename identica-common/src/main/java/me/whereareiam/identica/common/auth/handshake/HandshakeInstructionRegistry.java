package me.whereareiam.identica.common.auth.handshake;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.cache.codec.type.JsonCodec;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.HandshakeMode;

import java.time.Duration;
import java.util.Optional;

@Singleton
public class HandshakeInstructionRegistry {
	private final Cache<HandshakeInstruction> cache;
	private final Provider<Settings> settingsProvider;

	@Inject
	public HandshakeInstructionRegistry(
			CacheService cacheService,
			Provider<Settings> settingsProvider,
			Provider<Replication> replicationProvider
	) {
		this.cache = cacheService.synchronizedCache(resolveNamespace(replicationProvider), JsonCodec.of(HandshakeInstruction.class));
		this.settingsProvider = settingsProvider;
	}

	public long getDefaultTtlMillis() {
		Settings.Authentication authentication = settingsProvider.get().getAuthentication();

		Duration configured = authentication.getHandshakeInstructionTtl();
		if (configured.isZero() || configured.isNegative())
			throw new IllegalStateException("settings.authentication.handshakeInstructionTtl must be positive");

		return configured.toMillis();
	}

	public void put(HandshakeInstruction instruction) {
		String key = normalize(instruction.getIdentity().getUsername());
		long ttlMs = Math.max(1, instruction.getExpiresAt() - System.currentTimeMillis());
		cache.put(key, instruction, ttlMs).join();
	}

	public void request(String username, HandshakeMode mode) {
		put(HandshakeInstruction.create(new ConnectionIdentity(username, null), mode, getDefaultTtlMillis()));
	}

	public Optional<HandshakeInstruction> peek(String username) {
		return read(username, false);
	}

	public Optional<HandshakeInstruction> consume(String username) {
		return read(username, true);
	}

	public void invalidate(String username) {
		String key = normalize(username);
		cache.invalidate(key).join();
	}

	private Optional<HandshakeInstruction> read(String username, boolean consume) {
		String key = normalize(username);
		HandshakeInstruction instruction = cache.get(key).join().orElse(null);
		if (instruction == null) return Optional.empty();

		if (instruction.isExpired(System.currentTimeMillis())) {
			cache.invalidate(key).join();
			return Optional.empty();
		}

		if (consume) cache.invalidate(key).join();
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
