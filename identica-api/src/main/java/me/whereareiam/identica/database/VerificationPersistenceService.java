package me.whereareiam.identica.database;

import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationRecoveryCode;
import me.whereareiam.identica.model.verification.VerificationSelection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationPersistenceService {
	@NotNull Optional<VerificationEnrollment> findEnrollment(@NotNull UUID uniqueId, @NotNull String methodId);

	@NotNull List<VerificationEnrollment> findEnrollments(@NotNull UUID uniqueId);

	@NotNull VerificationEnrollment upsertEnrollment(@NotNull VerificationEnrollment enrollment);

	void deleteEnrollment(@NotNull UUID uniqueId, @NotNull String methodId);

	@NotNull Optional<VerificationSelection> findSelection(@NotNull UUID uniqueId, @NotNull String providerId);

	@NotNull List<VerificationSelection> findSelections(@NotNull UUID uniqueId);

	@NotNull VerificationSelection upsertSelection(@NotNull VerificationSelection selection);

	void deleteSelection(@NotNull UUID uniqueId, @NotNull String providerId);

	void deleteSelectionsByMethod(@NotNull UUID uniqueId, @NotNull String methodId);

	@NotNull List<VerificationRecoveryCode> findRecoveryCodes(@NotNull UUID uniqueId, @NotNull String methodId);

	void replaceRecoveryCodes(@NotNull UUID uniqueId, @NotNull String methodId, @NotNull List<VerificationRecoveryCode> recoveryCodes);

	boolean markRecoveryCodeUsed(@NotNull UUID uniqueId, @NotNull String methodId, @NotNull String codeHash, long usedAt);

	void deleteAll(@NotNull UUID uniqueId);

	void deleteProviderSelections(@NotNull UUID uniqueId, @Nullable String providerId);
}
