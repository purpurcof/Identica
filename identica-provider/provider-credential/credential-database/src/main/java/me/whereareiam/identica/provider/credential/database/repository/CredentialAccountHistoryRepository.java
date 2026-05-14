package me.whereareiam.identica.provider.credential.database.repository;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface CredentialAccountHistoryRepository {
	@SqlUpdate("""
			INSERT INTO identica_provider_credential_accounts_history
				(provider_id, provider_subject, hashing_method, change_reason, changed_at)
			VALUES
				(:providerId, :providerSubject, :hashingMethod, :changeReason, :changedAt)
			""")
	void insert(
			@Bind("providerId") String providerId,
			@Bind("providerSubject") String providerSubject,
			@Bind("hashingMethod") String hashingMethod,
			@Bind("changeReason") String changeReason,
			@Bind("changedAt") long changedAt
	);
}
