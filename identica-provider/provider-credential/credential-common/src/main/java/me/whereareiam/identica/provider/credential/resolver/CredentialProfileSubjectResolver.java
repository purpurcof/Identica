package me.whereareiam.identica.provider.credential.resolver;

import me.whereareiam.identica.provider.credential.CredentialConstants;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Resolves the credential-provider subject from the current username using
 * the existing offline UUID convention.
 */
public final class CredentialProfileSubjectResolver implements ProfileSubjectResolver {
	@Override
	public boolean supports(@NotNull ProfileResolveContext context) {
		String username = context.getUsername();
		return username != null && !username.isBlank();
	}

	@Override
	public @Nullable ProfileResolution resolve(@NotNull ProfileResolveContext context) {
		String username = context.getUsername();
		if (username == null || username.isBlank()) return null;

		UUID subject = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (subject == null) return null;

		return ProfileResolution.builder()
				.providerId(CredentialConstants.PROVIDER_ID)
				.providerSubject(subject.toString())
				.build();
	}

	@Override
	public int priority() {
		return 10;
	}
}
