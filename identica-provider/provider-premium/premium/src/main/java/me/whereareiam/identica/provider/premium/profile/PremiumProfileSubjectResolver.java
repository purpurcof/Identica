package me.whereareiam.identica.provider.premium.profile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.flow.FlowRefeference;
import me.whereareiam.identica.flow.FlowTransit;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.PremiumFlowSignals;
import me.whereareiam.identica.provider.profile.ProfileResolution;
import me.whereareiam.identica.provider.profile.ProfileResolveContext;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.util.UniqueIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumProfileSubjectResolver implements ProfileSubjectResolver {
	private final FlowTransit flowTransit;

	@Override
	public boolean supports(@NotNull ProfileResolveContext context) {
		return readObservation(context) != null;
	}

	@Override
	public @Nullable ProfileResolution resolve(@NotNull ProfileResolveContext context) {
		String subject = readObservation(context);
		if (subject == null) return null;

		return ProfileResolution.builder()
				.providerId(PremiumConstants.PROVIDER_ID)
				.providerSubject(subject)
				.build();
	}

	@Override
	public int priority() {
		return 50;
	}

	private @Nullable String readObservation(@NotNull ProfileResolveContext context) {
		String username = context.getUsername();
		if (username == null || username.isBlank())
			return null;

		String profileUniqueId = flowTransit
				.scope(FlowRefeference.builder()
						.username(username)
						.ip(context.getIp())
						.build())
				.peek(PremiumFlowSignals.PLATFORM_PROFILE_ID)
				.orElse(null);
		if (profileUniqueId == null || profileUniqueId.isBlank())
			return null;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && profileUniqueId.equalsIgnoreCase(offlineUuid.toString()))
			return null;

		return profileUniqueId.trim();
	}
}
