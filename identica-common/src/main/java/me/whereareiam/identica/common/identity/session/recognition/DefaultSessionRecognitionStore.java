package me.whereareiam.identica.common.identity.session.recognition;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionStore;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.model.session.SessionRecognitionSnapshot;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

@Singleton
public class DefaultSessionRecognitionStore implements SessionRecognitionStore {
	private final ReplicatedCache<SessionRecognitionSnapshot> cache;

	@Inject
	public DefaultSessionRecognitionStore(
			@NotNull Provider<Settings> settingsProvider,
			@NotNull Provider<Replication> replicationProvider,
			@NotNull ReplicationSystem replicationSystem
	) {
		Replication.Sessions.Recognition recognition = replicationProvider.get().getCache().getSessions().getRecognition();
		long ttlMs = settingsProvider.get().getSessions().getRecognition().validityMillis();
		ReplicationType<SessionRecognitionSnapshot, SessionRecognitionSnapshot> type =
				ReplicationType.identity(SessionRecognitionSnapshot.class);
		this.cache = replicationSystem.cache(resolveNamespace(recognition))
				.defaultTtl(ttlMs)
				.replicated(type);
	}

	@Override
	public @NotNull Optional<SessionRecognitionSnapshot> find(
			@Nullable String providerId,
			@Nullable String providerSubject
	) {
		String key = key(providerId, providerSubject);
		if (key == null) return Optional.empty();

		return cache.get(key).join();
	}

	@Override
	public void save(@Nullable SessionRecognitionSnapshot snapshot) {
		if (snapshot == null) return;

		String key = key(snapshot.getProviderId(), snapshot.getProviderSubject());
		if (key == null) return;

		cache.put(key, snapshot).join();
	}

	@Override
	public void clear(
			@Nullable String providerId,
			@Nullable String providerSubject
	) {
		String key = key(providerId, providerSubject);
		if (key == null)
			return;

		cache.invalidate(key).join();
	}

	private @Nullable String key(
			@Nullable String providerId,
			@Nullable String providerSubject
	) {
		String normalizedProviderId = normalize(providerId);
		String normalizedProviderSubject = normalize(providerSubject);
		if (normalizedProviderId == null || normalizedProviderSubject == null)
			return null;

		return normalizedProviderId + "|" + normalizedProviderSubject;
	}

	private @Nullable String normalize(@Nullable String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static @NotNull String resolveNamespace(@NotNull Replication.Sessions.Recognition recognition) {
		String namespace = recognition.getSnapshot();
		if (namespace.isBlank()) throw new IllegalStateException("replication.cache.sessions.recognition.snapshot is missing");

		return namespace;
	}
}
