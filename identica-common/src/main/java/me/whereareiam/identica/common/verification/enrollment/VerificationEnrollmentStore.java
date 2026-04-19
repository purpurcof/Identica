package me.whereareiam.identica.common.verification.enrollment;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentSession;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.LocalCache;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class VerificationEnrollmentStore {
	private static final String NAMESPACE = "verification:enrollment";

	private final LocalCache<VerificationEnrollmentSession> cache;
	private final Provider<Verification> verificationProvider;

	@Inject
	public VerificationEnrollmentStore(
			@NotNull ReplicationSystem replicationSystem,
			@NotNull Provider<Verification> verificationProvider
	) {
		this.cache = replicationSystem.cache(NAMESPACE).local();
		this.verificationProvider = verificationProvider;
	}

	public void put(@NotNull UUID uniqueId, @NotNull VerificationEnrollmentSession pendingEnrollment) {
		cache.put(uniqueId.toString(), pendingEnrollment, ttlMs()).join();
	}

	public @NotNull Optional<VerificationEnrollmentSession> peek(@NotNull UUID uniqueId) {
		return cache.get(uniqueId.toString()).join();
	}

	public @NotNull Optional<VerificationEnrollmentSession> consume(@NotNull UUID uniqueId) {
		return cache.consume(uniqueId.toString()).join();
	}

	public boolean clear(@NotNull UUID uniqueId) {
		return consume(uniqueId).isPresent();
	}

	private long ttlMs() {
		return verificationProvider.get().enrollmentTtlMillis();
	}
}
