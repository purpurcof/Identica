package me.whereareiam.identica.provider.premium.profile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.premium.PremiumKeys;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.registry.PreLoginExtensions;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumProfileSubjectResolver implements ProfileSubjectResolver {
	private static final String PROVIDER_ID = "premium";
	private final PreLoginExtensions preLoginExtensions;

	@Override
	public boolean supports(@NotNull ProfileResolveContext context) {
		return resolveObservation(context) != null;
	}

	@Override
	public @Nullable ProfileResolution resolve(@NotNull ProfileResolveContext context) {
		String subject = resolveObservation(context);
		if (subject == null) return null;

		return ProfileResolution.builder()
				.providerId(PROVIDER_ID)
				.providerSubject(subject)
				.build();
	}

	@Override
	public int priority() {
		return 50;
	}

	private @Nullable String resolveObservation(@NotNull ProfileResolveContext context) {
		String username = context.getUsername();
		if (username == null || username.isBlank())
			return null;

		String profileUniqueId = preLoginExtensions
				.consume(username, PremiumKeys.PLATFORM_PROFILE_ID)
				.orElse(null);
		if (profileUniqueId == null || profileUniqueId.isBlank())
			return null;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && profileUniqueId.equalsIgnoreCase(offlineUuid.toString()))
			return null;

		return profileUniqueId.trim();
	}
}
