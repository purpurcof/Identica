package me.whereareiam.identica.common.config.adapter;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.identica.type.provider.ProviderCapability;

public class ProviderCapabilityAdapter implements TypeAdapter<ProviderCapability> {
	@Override
	public ProviderCapability deserialize(String value) {
		ProviderCapability capability = ProviderCapability.fromId(value);
		if (capability == null) throw new IllegalArgumentException("Unknown provider capability: " + value);

		return capability;
	}

	@Override
	public String serialize(ProviderCapability value) {
		return value != null ? value.getId() : null;
	}
}
