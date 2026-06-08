package me.whereareiam.identica.common.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.type.verification.UnavailableSelectionPolicy;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationPolicyResolver {
	private final Provider<Providers> providersProvider;

	public @Nullable ResolvedProviderPolicy resolveProviderPolicy(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;

		Providers providers = providersProvider.get();
		if (providers == null) return null;

		for (Providers.ProviderEntry entry : providers.getProviders()) {
			if (entry == null) continue;
			if (!providerId.equalsIgnoreCase(entry.getId())) continue;

			Providers.ProviderEntry.Verification verification = entry.getVerification();
			if (verification == null) {
				return new ResolvedProviderPolicy(
						false,
						false,
						UnavailableSelectionPolicy.CLEAR_SELECTION
				);
			}

			return new ResolvedProviderPolicy(
					verification.isEnabled(),
					verification.isRequired(),
					verification.getUnavailableSelectionPolicy() != null
							? verification.getUnavailableSelectionPolicy()
							: UnavailableSelectionPolicy.CLEAR_SELECTION
			);
		}

		return null;
	}

	public boolean hasConfiguredProvider(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return false;

		Providers providers = providersProvider.get();
		if (providers == null) return false;

		for (Providers.ProviderEntry entry : providers.getProviders()) {
			if (entry == null || entry.getId().isBlank()) continue;
			if (providerId.equalsIgnoreCase(entry.getId()))
				return true;
		}

		return false;
	}

	public @Nullable ResolvedMethodPolicy resolveMethodPolicy(@Nullable String providerId, @Nullable String methodId) {
		if (providerId == null || providerId.isBlank() || methodId == null || methodId.isBlank()) return null;

		Providers providers = providersProvider.get();
		if (providers == null) return null;

		for (Providers.ProviderEntry entry : providers.getProviders()) {
			if (entry == null || !providerId.equalsIgnoreCase(entry.getId())) continue;

			Providers.ProviderEntry.Verification verification = entry.getVerification();
			if (verification == null) return null;
			for (Providers.ProviderEntry.Verification.MethodEntry method : verification.getMethods()) {
				if (method == null || !methodId.equalsIgnoreCase(method.getId())) continue;

				boolean required = method.getRequired() != null ? method.getRequired() : verification.isRequired();
				UnavailableSelectionPolicy policy = method.getUnavailableSelectionPolicy() != null
						? method.getUnavailableSelectionPolicy()
						: verification.getUnavailableSelectionPolicy() != null
								? verification.getUnavailableSelectionPolicy()
								: UnavailableSelectionPolicy.CLEAR_SELECTION;

				return new ResolvedMethodPolicy(method.isEnabled(), required, policy);
			}
		}

		return null;
	}

	public record ResolvedProviderPolicy(
			boolean enabled,
			boolean required,
			UnavailableSelectionPolicy unavailableSelectionPolicy
	) {
	}

	public record ResolvedMethodPolicy(
			boolean enabled,
			boolean required,
			UnavailableSelectionPolicy unavailableSelectionPolicy
	) {
	}
}
