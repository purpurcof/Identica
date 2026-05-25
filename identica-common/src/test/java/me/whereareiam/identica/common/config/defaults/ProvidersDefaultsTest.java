package me.whereareiam.identica.common.config.defaults;

import me.whereareiam.identica.common.config.defaults.provider.ProvidersDefaults;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Providers Defaults")
class ProvidersDefaultsTest {
	@DisplayName("Provider recognition overrides default to disabled")
	@Test
	void providerRecognitionOverridesDefaultToDisabled() {
		Providers providers = new ProvidersDefaults().supply(new Providers());

		assertTrue(provider(providers, "premium").getOverrides().getRecognition().getSignals().isEmpty());
		assertNull(provider(providers, "premium").getOverrides().getRecognition().getEnabled());
		assertFalse(provider(providers, "premium").getOverrides().getRecognition().getEligibility().isAllowOnUntrustedIp());
		assertTrue(provider(providers, "credential").getOverrides().getRecognition().getSignals().isEmpty());
		assertNull(provider(providers, "credential").getOverrides().getRecognition().getEnabled());
		assertFalse(provider(providers, "credential").getOverrides().getRecognition().getEligibility().isAllowOnUntrustedIp());
	}

	@DisplayName("Provider join restriction defaults are present but disabled")
	@Test
	void providerJoinRestrictionDefaultsPresentButDisabled() {
		Providers providers = new ProvidersDefaults().supply(new Providers());

		assertFalse(provider(providers, "premium").getJoinRestriction().isEnabled());
		assertEquals(List.of(ProviderJoinRestrictionCondition.RECOGNIZED),
				provider(providers, "premium").getJoinRestriction().getAllow());
		assertFalse(provider(providers, "credential").getJoinRestriction().isEnabled());
		assertEquals(
				List.of(ProviderJoinRestrictionCondition.RECOGNIZED, ProviderJoinRestrictionCondition.LINKED),
				provider(providers, "credential").getJoinRestriction().getAllow()
		);
	}

	private Providers.ProviderEntry provider(Providers providers, String id) {
		return providers.getProviders().stream()
				.filter(entry -> entry.getId().equals(id))
				.findFirst()
				.orElseThrow();
	}
}
