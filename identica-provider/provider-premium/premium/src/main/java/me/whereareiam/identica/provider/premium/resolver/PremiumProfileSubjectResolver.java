package me.whereareiam.identica.provider.premium.resolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.premium.PremiumConstants;
import me.whereareiam.identica.provider.premium.PremiumIdentityMetaItem;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
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
	private final PipelineStateStore pipelineStateStore;

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

		PipelineStateReference reference = PipelineStateReference.builder()
				.username(username)
				.ip(context.getIp())
				.build();
		String profileUniqueId = pipelineStateStore.find(reference)
				.flatMap(state -> state.item(PremiumIdentityMetaItem.class))
				.map(PremiumIdentityMetaItem::getProfileId)
				.orElse(null);
		if (profileUniqueId == null || profileUniqueId.isBlank())
			return null;

		UUID offlineUuid = UniqueIdGenerator.offlinePlayerUniqueId(username);
		if (offlineUuid != null && profileUniqueId.equalsIgnoreCase(offlineUuid.toString()))
			return null;

		return profileUniqueId.trim();
	}
}
