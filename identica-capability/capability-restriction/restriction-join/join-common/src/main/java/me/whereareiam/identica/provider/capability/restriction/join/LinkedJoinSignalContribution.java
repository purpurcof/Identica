package me.whereareiam.identica.provider.capability.restriction.join;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.provider.capability.restriction.contribution.ProviderRestrictionSignalContribution;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionEvaluationRequest;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionSignal;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionType;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import org.jetbrains.annotations.NotNull;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LinkedJoinSignalContribution implements ProviderRestrictionSignalContribution {
	private static final RestrictionSignal LINKED = RestrictionSignal.of("linked");

	private final ProviderLinkPersistenceService providerLinkPersistenceService;

	@Override
	public @NotNull ProviderCapability capability() {
		return JoinRestrictionCapability.CAPABILITY;
	}

	@Override
	public @NotNull RestrictionType restrictionType() {
		return JoinRestrictionType.TYPE;
	}

	@Override
	public @NotNull RestrictionSignal signal() {
		return LINKED;
	}

	@Override
	public boolean matches(@NotNull RestrictionEvaluationRequest request) {
		String providerId = request.getProviderId();
		String providerSubject = request.getProviderSubject();
		if (providerId == null || providerId.isBlank()) return false;
		if (providerSubject == null || providerSubject.isBlank()) return false;

		return providerLinkPersistenceService.findBySubject(providerId, providerSubject).isPresent();
	}
}
