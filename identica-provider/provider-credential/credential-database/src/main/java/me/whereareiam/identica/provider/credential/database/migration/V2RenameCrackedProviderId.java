package me.whereareiam.identica.provider.credential.database.migration;

import me.whereareiam.dialectica.migration.SchemaMigration;
import me.whereareiam.dialectica.migration.SchemaMigrationContext;
import org.jetbrains.annotations.NotNull;

public final class V2RenameCrackedProviderId implements SchemaMigration {
	private static final String LEGACY_PROVIDER_ID = "cracked";
	private static final String CANONICAL_PROVIDER_ID = "credential";

	@Override
	public int version() {
		return 2;
	}

	@Override
	public @NotNull String name() {
		return "rename_cracked_provider_id";
	}

	@Override
	public void migrate(@NotNull SchemaMigrationContext context) {
		copyProviderLinks(context);
		copyProviderProfiles(context);
		copyCredentialAccounts(context);
		copyCredentialAccountHistory(context);
		copyVerificationSelections(context);
		copyUsernameHistory(context);

		deleteCredentialAccountHistory(context);
		deleteCredentialAccounts(context);
		deleteProviderProfiles(context);
		deleteProviderLinks(context);
		deleteVerificationSelections(context);
		deleteUsernameHistory(context);
	}

	private void copyProviderLinks(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_links")) return;

		context.execute("""
				INSERT INTO identica_provider_links (
					provider_id, provider_subject, unique_id, is_primary, linked_at, last_seen_at
				)
				SELECT '%s', legacy.provider_subject, legacy.unique_id, legacy.is_primary, legacy.linked_at, legacy.last_seen_at
				  FROM identica_provider_links legacy
				 WHERE legacy.provider_id = '%s'
				   AND NOT EXISTS (
						SELECT 1
						  FROM identica_provider_links target
						 WHERE target.provider_id = '%s'
						   AND target.provider_subject = legacy.provider_subject
				   )
				   AND NOT EXISTS (
						SELECT 1
						  FROM identica_provider_links target
						 WHERE target.provider_id = '%s'
						   AND target.unique_id = legacy.unique_id
				   )
				""".formatted(CANONICAL_PROVIDER_ID, LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void copyProviderProfiles(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_profiles") || !context.tableExists("identica_provider_links")) return;

		context.execute("""
				INSERT INTO identica_provider_profiles (
					provider_id, provider_subject, provider_username
				)
				SELECT '%s', legacy.provider_subject, legacy.provider_username
				  FROM identica_provider_profiles legacy
				  JOIN identica_provider_links target
				    ON target.provider_id = '%s'
				   AND target.provider_subject = legacy.provider_subject
				 WHERE legacy.provider_id = '%s'
				   AND NOT EXISTS (
						SELECT 1
						  FROM identica_provider_profiles existing
						 WHERE existing.provider_id = '%s'
						   AND existing.provider_subject = legacy.provider_subject
				   )
				""".formatted(CANONICAL_PROVIDER_ID, CANONICAL_PROVIDER_ID, LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void copyCredentialAccounts(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_credential_accounts")) return;

		context.execute("""
				INSERT INTO identica_provider_credential_accounts (
					provider_id, provider_subject, password_hash, hashing_method, created_at, updated_at
				)
				SELECT '%s', legacy.provider_subject, legacy.password_hash, legacy.hashing_method, legacy.created_at, legacy.updated_at
				  FROM identica_provider_credential_accounts legacy
				 WHERE legacy.provider_id = '%s'
				   AND NOT EXISTS (
						SELECT 1
						  FROM identica_provider_credential_accounts target
						 WHERE target.provider_id = '%s'
						   AND target.provider_subject = legacy.provider_subject
				   )
				""".formatted(CANONICAL_PROVIDER_ID, LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void copyCredentialAccountHistory(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_credential_accounts_history")
				|| !context.tableExists("identica_provider_credential_accounts"))
			return;

		context.execute("""
				INSERT INTO identica_provider_credential_accounts_history (
					provider_id, provider_subject, hashing_method, change_reason, changed_at
				)
				SELECT '%s', legacy.provider_subject, legacy.hashing_method, legacy.change_reason, legacy.changed_at
				  FROM identica_provider_credential_accounts_history legacy
				  JOIN identica_provider_credential_accounts target
				    ON target.provider_id = '%s'
				   AND target.provider_subject = legacy.provider_subject
				 WHERE legacy.provider_id = '%s'
				   AND NOT EXISTS (
						SELECT 1
						  FROM identica_provider_credential_accounts_history existing
						 WHERE existing.provider_id = '%s'
						   AND existing.provider_subject = legacy.provider_subject
						   AND existing.changed_at = legacy.changed_at
				   )
				""".formatted(CANONICAL_PROVIDER_ID, CANONICAL_PROVIDER_ID, LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void copyVerificationSelections(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_verification_selections")) return;

		context.execute("""
				INSERT INTO identica_verification_selections (
					unique_id, provider_id, method_id, selected_at
				)
				SELECT legacy.unique_id, '%s', legacy.method_id, legacy.selected_at
				  FROM identica_verification_selections legacy
				 WHERE legacy.provider_id = '%s'
				   AND NOT EXISTS (
						SELECT 1
						  FROM identica_verification_selections target
						 WHERE target.unique_id = legacy.unique_id
						   AND target.provider_id = '%s'
				   )
				""".formatted(CANONICAL_PROVIDER_ID, LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void copyUsernameHistory(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_username_history")) return;

		context.execute("""
				INSERT INTO identica_username_history (
					unique_id, provider_id, old_username, new_username, source, changed_at
				)
				SELECT legacy.unique_id, '%s', legacy.old_username, legacy.new_username, legacy.source, legacy.changed_at
				  FROM identica_username_history legacy
				 WHERE legacy.provider_id = '%s'
				   AND NOT EXISTS (
						SELECT 1
						  FROM identica_username_history existing
						 WHERE existing.unique_id = legacy.unique_id
						   AND existing.provider_id = '%s'
						   AND existing.old_username = legacy.old_username
						   AND existing.new_username = legacy.new_username
						   AND existing.source = legacy.source
						   AND existing.changed_at = legacy.changed_at
				   )
				""".formatted(CANONICAL_PROVIDER_ID, LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void deleteCredentialAccountHistory(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_credential_accounts_history")) return;

		context.execute("""
				DELETE FROM identica_provider_credential_accounts_history
				 WHERE provider_id = '%s'
				   AND EXISTS (
						SELECT 1
						  FROM identica_provider_credential_accounts_history target
						 WHERE target.provider_id = '%s'
						   AND target.provider_subject = identica_provider_credential_accounts_history.provider_subject
						   AND target.changed_at = identica_provider_credential_accounts_history.changed_at
				   )
				""".formatted(LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void deleteCredentialAccounts(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_credential_accounts")) return;

		context.execute("""
				DELETE FROM identica_provider_credential_accounts
				 WHERE provider_id = '%s'
				   AND EXISTS (
						SELECT 1
						  FROM identica_provider_credential_accounts target
						 WHERE target.provider_id = '%s'
						   AND target.provider_subject = identica_provider_credential_accounts.provider_subject
				   )
				""".formatted(LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void deleteProviderProfiles(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_profiles")) return;

		context.execute("""
				DELETE FROM identica_provider_profiles
				 WHERE provider_id = '%s'
				   AND EXISTS (
						SELECT 1
						  FROM identica_provider_profiles target
						 WHERE target.provider_id = '%s'
						   AND target.provider_subject = identica_provider_profiles.provider_subject
				   )
				""".formatted(LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void deleteProviderLinks(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_provider_links")) return;

		context.execute("""
				DELETE FROM identica_provider_links
				 WHERE provider_id = '%s'
				   AND EXISTS (
						SELECT 1
						  FROM identica_provider_links target
						 WHERE target.provider_id = '%s'
						   AND target.provider_subject = identica_provider_links.provider_subject
				   )
				""".formatted(LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void deleteVerificationSelections(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_verification_selections")) return;

		context.execute("""
				DELETE FROM identica_verification_selections
				 WHERE provider_id = '%s'
				   AND EXISTS (
						SELECT 1
						  FROM identica_verification_selections target
						 WHERE target.unique_id = identica_verification_selections.unique_id
						   AND target.provider_id = '%s'
				   )
				""".formatted(LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}

	private void deleteUsernameHistory(@NotNull SchemaMigrationContext context) {
		if (!context.tableExists("identica_username_history")) return;

		context.execute("""
				DELETE FROM identica_username_history
				 WHERE provider_id = '%s'
				   AND EXISTS (
						SELECT 1
						  FROM identica_username_history target
						 WHERE target.unique_id = identica_username_history.unique_id
						   AND target.provider_id = '%s'
						   AND target.old_username = identica_username_history.old_username
						   AND target.new_username = identica_username_history.new_username
						   AND target.source = identica_username_history.source
						   AND target.changed_at = identica_username_history.changed_at
				   )
				""".formatted(LEGACY_PROVIDER_ID, CANONICAL_PROVIDER_ID));
	}
}
