package me.whereareiam.identica.common.attributes;

import me.whereareiam.identica.Key;
import me.whereareiam.identica.type.AttributeScope;
import me.whereareiam.identica.attributes.ScopedAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultScopedAttributes implements ScopedAttributes {
	private final Map<AttributeScope, Map<String, Map<String, Entry>>> values = new ConcurrentHashMap<>();

	@Override
	public <T> void put(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key,
			@NotNull T value
	) {
		put(scope, ownerKey, key, value, 0);
	}

	@Override
	public <T> void put(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key,
			@NotNull T value,
			long ttlMs
	) {
		if (ttlMs < 0) return;
		String normalized = normalize(scope, ownerKey);
		if (normalized == null) return;

		Map<String, Map<String, Entry>> scopeMap = values.computeIfAbsent(scope, ignored -> new ConcurrentHashMap<>());
		Map<String, Entry> entry = scopeMap.computeIfAbsent(normalized, ignored -> new ConcurrentHashMap<>());
		long expiresAt = ttlMs > 0 ? System.currentTimeMillis() + ttlMs : 0;
		entry.put(key.getName(), new Entry(value, expiresAt));
	}

	@Override
	public @NotNull <T> Optional<T> get(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key
	) {
		return read(scope, ownerKey, key, false);
	}

	@Override
	public @NotNull <T> Optional<T> remove(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key
	) {
		return read(scope, ownerKey, key, true);
	}

	@Override
	public void clear(@NotNull AttributeScope scope, @NotNull String ownerKey) {
		String normalized = normalize(scope, ownerKey);
		if (normalized == null) return;

		Map<String, Map<String, Entry>> scopeMap = values.get(scope);
		if (scopeMap == null) return;
		scopeMap.remove(normalized);
	}

	private <T> Optional<T> read(
			@NotNull AttributeScope scope,
			@NotNull String ownerKey,
			@NotNull Key<T> key,
			boolean remove
	) {
		String normalized = normalize(scope, ownerKey);
		if (normalized == null) return Optional.empty();

		Map<String, Map<String, Entry>> scopeMap = values.get(scope);
		if (scopeMap == null) return Optional.empty();

		Map<String, Entry> entry = scopeMap.get(normalized);
		if (entry == null) return Optional.empty();

		Entry stored = entry.get(key.getName());
		if (stored == null) return Optional.empty();

		if (stored.expiresAt > 0 && stored.expiresAt <= System.currentTimeMillis()) {
			entry.remove(key.getName());
			if (entry.isEmpty())
				scopeMap.remove(normalized, entry);
			return Optional.empty();
		}

		if (remove) {
			entry.remove(key.getName());
			if (entry.isEmpty())
				scopeMap.remove(normalized, entry);
		}

		return cast(key, stored.value);
	}

	private @Nullable String normalize(@NotNull AttributeScope scope, @NotNull String ownerKey) {
		if (ownerKey.isBlank())
			return null;

		String normalized = ownerKey.trim();
		if (scope == AttributeScope.PROFILE_HINT)
			return normalized.toLowerCase(Locale.ROOT);

		return normalized;
	}

	private <T> Optional<T> cast(@NotNull Key<T> key, Object value) {
		if (key.getType().isInstance(value))
			return Optional.of(key.cast(value));

		return Optional.empty();
	}

	private record Entry(Object value, long expiresAt) {
	}
}
