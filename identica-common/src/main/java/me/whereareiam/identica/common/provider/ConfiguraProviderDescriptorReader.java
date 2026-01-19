package me.whereareiam.identica.common.provider;

import me.whereareiam.configura.Config;
import me.whereareiam.identica.model.ProviderDescriptor;

import java.io.InputStream;

public class ConfiguraProviderDescriptorReader implements ProviderDescriptorReader {
	@Override
	public ProviderDescriptor read(InputStream inputStream) {
		return Config.load(inputStream, ProviderDescriptor.class);
	}
}
