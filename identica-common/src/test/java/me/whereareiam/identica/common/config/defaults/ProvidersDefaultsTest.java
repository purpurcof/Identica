package me.whereareiam.identica.common.config.defaults;

import me.whereareiam.identica.model.config.Providers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Providers Defaults")
class ProvidersDefaultsTest {
	@DisplayName("Provider recognition overrides default to disabled")
	@Test
	void providerRecognitionOverridesDefaultToDisabled() {
		Providers providers = new ProvidersDefaults().supply(new Providers());

		assertFalse(provider(providers, "premium").getOverrides().isAllowRecognitionOnUntrustedIp());
		assertTrue(provider(providers, "premium").getOverrides().getRecognition().getSignals().isEmpty());
		assertNull(provider(providers, "premium").getOverrides().getRecognition().getEnabled());
		assertFalse(provider(providers, "credential").getOverrides().isAllowRecognitionOnUntrustedIp());
		assertTrue(provider(providers, "credential").getOverrides().getRecognition().getSignals().isEmpty());
		assertNull(provider(providers, "credential").getOverrides().getRecognition().getEnabled());
	}

	private Providers.ProviderEntry provider(Providers providers, String id) {
		return providers.getProviders().stream()
				.filter(entry -> entry.getId().equals(id))
				.findFirst()
				.orElseThrow();
	}
}
