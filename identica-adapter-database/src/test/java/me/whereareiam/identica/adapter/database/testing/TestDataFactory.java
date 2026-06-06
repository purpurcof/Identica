package me.whereareiam.identica.adapter.database.testing;

import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;

import java.util.UUID;

public final class TestDataFactory {
	public static final long CREATED_AT = 1_700_000_000_000L;
	public static final long LAST_SEEN_AT = 1_700_000_000_500L;
	public static final long LINKED_AT = 1_700_000_001_000L;

	public static Account account(UUID uniqueId, String username) {
		return Account.builder()
				.uniqueId(uniqueId)
				.username(username)
				.createdAt(CREATED_AT)
				.lastSeenAt(LAST_SEEN_AT)
				.build();
	}

	public static AccountProviderLink providerLink(UUID uniqueId, String providerId, String subject, boolean primary) {
		return AccountProviderLink.builder()
				.uniqueId(uniqueId)
				.providerId(providerId)
				.providerSubject(subject)
				.primaryLink(primary)
				.linkedAt(LINKED_AT)
				.lastSeenAt(LAST_SEEN_AT)
				.build();
	}

	public static AccountProviderProfile providerProfile(String providerId, String subject, String username) {
		return AccountProviderProfile.builder()
				.providerId(providerId)
				.providerSubject(subject)
				.providerUsername(username)
				.build();
	}
}
