package me.whereareiam.identica.provider.premium;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import me.whereareiam.identica.provider.premium.profile.PremiumProfileLookup;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumEligibilityResolver implements ProviderEligibilityResolver {
	private static final String PREMIUM_ID = "premium";

	private final @NotNull PremiumProfileLookup profileLookup;
	private final @NotNull Provider<PremiumSettings> settingsProvider;

	@Override
	public boolean isEligible(
			@NotNull AuthContext context,
			@NotNull InternalProvider provider,
			@NotNull AuthFlowType flow
	) {
		ProviderDescriptor descriptor = provider.getDescriptor();
		if (descriptor == null) return true;

		String providerId = descriptor.getId();
		if (!providerId.equalsIgnoreCase(PREMIUM_ID))
			return true;

		String username = context.getUsername();
		if (username == null || username.isBlank())
			return false;

		long timeoutMs = settingsProvider.get().getLookup().getTimeout().toMillis();
		CompletableFuture<Boolean> future = profileLookup.hasPremiumProfile(username);
		if (timeoutMs > 0)
			future = future.completeOnTimeout(false, timeoutMs, TimeUnit.MILLISECONDS);

		return future.exceptionally(ignored -> false).join();
	}
}
